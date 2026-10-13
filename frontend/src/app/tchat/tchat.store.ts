import { Injectable, computed, inject, signal } from '@angular/core';

import { ApiService, messageErreur } from '../noyau/api.service';
import {
  Compte,
  ConversationResume,
  ErreurTempsReel,
  Evenement,
  MessageAffiche,
  MessageVue,
} from '../noyau/modeles';
import { identifiantTemporaire } from '../noyau/outils';
import { SessionService } from '../noyau/session.service';
import { TempsReelService } from '../noyau/temps-reel.service';

interface Saisie {
  nom: string;
  expire: number;
}

const DELAI_ECHEC_ENVOI = 10_000;
const DUREE_INDICATEUR_SAISIE = 6_000;

/**
 * État de l'écran de tchat (client ou conseiller), à base de signaux.
 * Fourni au niveau du composant Tchat : il est détruit à la déconnexion.
 */
@Injectable()
export class TchatStore {
  private readonly api = inject(ApiService);
  private readonly tempsReel = inject(TempsReelService);
  private readonly session = inject(SessionService);

  readonly moi = computed(() => this.session.compte() as Compte);
  readonly estConseiller = computed(() => this.moi().role === 'CONSEILLER');

  readonly conversations = signal<ConversationResume[]>([]);
  readonly selection = signal<string | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal<string | null>(null);
  readonly maintenant = signal(Date.now());

  private readonly messagesParConversation = signal<Record<string, MessageAffiche[]>>({});
  private readonly saisies = signal<Record<string, Saisie | undefined>>({});
  private readonly nonLusParConversation = signal<Record<string, number>>({});

  readonly connecte = this.tempsReel.connecte;
  readonly instance = this.tempsReel.instance;

  readonly conversationActive = computed(
    () => this.conversations().find((c) => c.id === this.selection()) ?? null,
  );
  readonly messagesActifs = computed(() => {
    const id = this.selection();
    return id ? (this.messagesParConversation()[id] ?? null) : null;
  });
  readonly saisieActive = computed(() => {
    const id = this.selection();
    const saisie = id ? this.saisies()[id] : undefined;
    return saisie && saisie.expire > this.maintenant() ? saisie.nom : null;
  });
  readonly totalNonLus = computed(() =>
    Object.values(this.nonLusParConversation()).reduce((total, n) => total + n, 0),
  );

  private readonly desabonnements = new Map<string, () => void>();
  private minuteur: ReturnType<typeof setInterval> | null = null;
  private derniereSaisieEnvoyee = 0;


  demarrer(): void {
    this.tempsReel.connecter(
      this.moi().id,
      () => void this.recharger(), // première connexion et reprises après coupure
      (erreur: ErreurTempsReel) => this.erreur.set(erreur.message),
    );
    if (this.estConseiller()) {
      this.tempsReel.ecouter('/topic/file-attente', (corps) => this.traiter(corps as Evenement));
    }
    this.minuteur = setInterval(() => this.maintenant.set(Date.now()), 1000);
    void this.recharger();
  }

  arreter(): void {
    this.desabonnements.forEach((desabonner) => desabonner());
    this.desabonnements.clear();
    this.tempsReel.deconnecter();
    if (this.minuteur) {
      clearInterval(this.minuteur);
    }
  }

  // Chargement complet par REST : au démarrage et après chaque reconnexion (aucun message perdu).
  async recharger(): Promise<void> {
    try {
      const liste = await this.api.conversations();
      this.conversations.set(trier(liste));
      this.nonLusParConversation.set(Object.fromEntries(liste.map((c) => [c.id, c.nonLus ?? 0])));
      this.synchroniserAbonnements();
      const id = this.selection();
      if (id) {
        await this.chargerMessages(id);
        this.marquerLu(id);
      }
    } catch (e) {
      this.erreur.set(messageErreur(e));
    } finally {
      this.chargement.set(false);
    }
  }


  nonLus(id: string): number {
    return this.nonLusParConversation()[id] ?? 0;
  }

  async selectionner(id: string | null): Promise<void> {
    this.selection.set(id);
    if (!id) {
      return;
    }
    this.synchroniserAbonnements();
    if (!this.messagesParConversation()[id]) {
      await this.chargerMessages(id);
    }
    this.marquerLu(id);
  }

  private async chargerMessages(id: string): Promise<void> {
    try {
      const recus = await this.api.messages(id);
      this.messagesParConversation.update((tous) => {
        // on garde les messages encore « en cours d'envoi » qui ne sont pas encore confirmés
        const enAttente = (tous[id] ?? []).filter(
          (m) => m.etat !== 'envoye' && !recus.some((r) => r.id === m.id),
        );
        return { ...tous, [id]: [...recus.map(affiche), ...enAttente] };
      });
    } catch (e) {
      this.erreur.set(messageErreur(e));
    }
  }

  async ouvrir(sujet: string, reservationRef: string | null, message: string): Promise<boolean> {
    try {
      const conversation = await this.api.ouvrir(sujet, reservationRef, message);
      this.fusionner(conversation);
      await this.selectionner(conversation.id);
      return true;
    } catch (e) {
      this.erreur.set(messageErreur(e));
      return false;
    }
  }

  envoyer(texte: string): void {
    const conversation = this.conversationActive();
    const contenu = texte.trim();
    if (!conversation || !contenu) {
      return;
    }
    const idTemporaire = identifiantTemporaire();
    const moi = this.moi();
    this.ajouterMessage(
      {
        id: idTemporaire,
        conversationId: conversation.id,
        typeAuteur: moi.role,
        auteurId: moi.id,
        auteurNom: moi.nom,
        contenu,
        envoyeLe: new Date().toISOString(),
        luLe: null,
        etat: 'envoi',
        idTemporaire,
      },
      idTemporaire,
    );
    this.signalerSaisie(false);

    const parWebSocket = this.tempsReel.publier(`/app/conversations.${conversation.id}.messages`, {
      contenu,
      idTemporaire,
    });
    if (!parWebSocket) {
      // connexion temps réel coupée : envoi par l'API REST
      this.api
        .envoyer(conversation.id, contenu, idTemporaire)
        .then((message) => this.ajouterMessage(affiche(message), idTemporaire))
        .catch((e) => {
          this.marquerEchec(conversation.id, idTemporaire);
          this.erreur.set(messageErreur(e));
        });
    }
    setTimeout(() => this.marquerEchec(conversation.id, idTemporaire), DELAI_ECHEC_ENVOI);
  }

  reessayer(message: MessageAffiche): void {
    this.messagesParConversation.update((tous) => ({
      ...tous,
      [message.conversationId]: (tous[message.conversationId] ?? []).filter((m) => m.id !== message.id),
    }));
    this.envoyer(message.contenu);
  }

  signalerSaisie(actif: boolean): void {
    const conversation = this.conversationActive();
    if (!conversation || conversation.statut === 'CLOSE') {
      return;
    }
    const maintenant = Date.now();
    if (actif && maintenant - this.derniereSaisieEnvoyee < 3000) {
      return;
    }
    if (!actif && this.derniereSaisieEnvoyee === 0) {
      return;
    }
    this.derniereSaisieEnvoyee = actif ? maintenant : 0;
    this.tempsReel.publier(`/app/conversations.${conversation.id}.saisie`, { actif });
  }

  async prendreEnCharge(id: string): Promise<void> {
    try {
      this.fusionner(await this.api.prendreEnCharge(id));
      this.synchroniserAbonnements();
      this.marquerLu(id);
    } catch (e) {
      this.erreur.set(messageErreur(e));
    }
  }

  async clore(id: string): Promise<void> {
    try {
      this.fusionner(await this.api.clore(id));
    } catch (e) {
      this.erreur.set(messageErreur(e));
    }
  }

  // Accusé de lecture, seulement si l'onglet est visible.
  marquerLu(id: string): void {
    if (document.visibilityState !== 'visible' || this.selection() !== id) {
      return;
    }
    const conversation = this.conversations().find((c) => c.id === id);
    if (!conversation) {
      return;
    }
    if (this.estConseiller() && conversation.conseillerId !== this.moi().id) {
      return;
    }
    const autre = this.estConseiller() ? 'CLIENT' : 'CONSEILLER';
    const aLire = (this.messagesParConversation()[id] ?? []).some((m) => m.typeAuteur === autre && !m.luLe);
    if (!aLire && this.nonLus(id) === 0) {
      return;
    }
    this.nonLusParConversation.update((n) => ({ ...n, [id]: 0 }));
    if (!this.tempsReel.publier(`/app/conversations.${id}.lu`, {})) {
      void this.api.marquerLus(id).catch(() => undefined);
    }
  }


  private traiter(evenement: Evenement): void {
    switch (evenement.type) {
      case 'MESSAGE':
        this.recevoirMessage(evenement.message, evenement.idTemporaire);
        break;
      case 'CONVERSATION':
        this.fusionner(evenement.conversation);
        this.synchroniserAbonnements();
        break;
      case 'LU':
        if (evenement.lecteur !== this.moi().role) {
          this.messagesParConversation.update((tous) => ({
            ...tous,
            [evenement.conversationId]: (tous[evenement.conversationId] ?? []).map((m) =>
              m.typeAuteur === this.moi().role && !m.luLe ? { ...m, luLe: evenement.luLe } : m,
            ),
          }));
        }
        break;
      case 'SAISIE':
        if (evenement.auteurId !== this.moi().id) {
          this.saisies.update((s) => ({
            ...s,
            [evenement.conversationId]: evenement.actif
              ? { nom: evenement.auteurNom, expire: Date.now() + DUREE_INDICATEUR_SAISIE }
              : undefined,
          }));
        }
        break;
    }
  }

  private recevoirMessage(message: MessageVue, idTemporaire: string | null): void {
    const id = message.conversationId;
    if (this.messagesParConversation()[id]) {
      this.ajouterMessage(affiche(message), idTemporaire);
    }
    // aperçu dans la liste
    this.conversations.update((liste) =>
      trier(
        liste.map((c) =>
          c.id === id
            ? { ...c, dernierMessage: message.contenu, dernierAuteur: message.typeAuteur, derniereActiviteLe: message.envoyeLe }
            : c,
        ),
      ),
    );
    const autre = this.estConseiller() ? 'CLIENT' : 'CONSEILLER';
    if (message.typeAuteur === autre) {
      this.saisies.update((s) => ({ ...s, [id]: undefined }));
      if (this.selection() === id && document.visibilityState === 'visible') {
        this.marquerLu(id);
      } else {
        this.nonLusParConversation.update((n) => ({ ...n, [id]: (n[id] ?? 0) + 1 }));
      }
    }
  }

  private ajouterMessage(message: MessageAffiche, idTemporaire: string | null): void {
    this.messagesParConversation.update((tous) => {
      const liste = (tous[message.conversationId] ?? []).filter(
        (m) => m.id !== message.id && !(idTemporaire && m.idTemporaire === idTemporaire),
      );
      liste.push(message);
      liste.sort((a, b) => Date.parse(a.envoyeLe) - Date.parse(b.envoyeLe));
      return { ...tous, [message.conversationId]: liste };
    });
  }

  private marquerEchec(conversationId: string, idTemporaire: string): void {
    this.messagesParConversation.update((tous) => ({
      ...tous,
      [conversationId]: (tous[conversationId] ?? []).map((m) =>
        m.idTemporaire === idTemporaire && m.etat === 'envoi' ? { ...m, etat: 'echec' } : m,
      ),
    }));
  }

  // Met à jour (ou ajoute) une conversation dans la liste.
  private fusionner(conversation: ConversationResume): void {
    if (!this.estConseiller() && conversation.clientId !== this.moi().id) {
      return;
    }
    this.conversations.update((liste) =>
      trier([...liste.filter((c) => c.id !== conversation.id), conversation]),
    );
  }

  /**
   * Abonnements aux conversations utiles :
   * client → toutes ses conversations non closes ; conseiller → celles qu'il suit et celle qu'il consulte.
   */
  private synchroniserAbonnements(): void {
    const moi = this.moi();
    const utiles = new Set(
      this.conversations()
        .filter((c) => (this.estConseiller() ? c.conseillerId === moi.id && c.statut !== 'CLOSE' : c.statut !== 'CLOSE'))
        .map((c) => c.id),
    );
    const selection = this.selection();
    if (selection) {
      utiles.add(selection);
    }
    for (const [id, desabonner] of this.desabonnements) {
      if (!utiles.has(id)) {
        desabonner();
        this.desabonnements.delete(id);
      }
    }
    for (const id of utiles) {
      if (!this.desabonnements.has(id)) {
        this.desabonnements.set(
          id,
          this.tempsReel.ecouter(`/topic/conversations.${id}`, (corps) => this.traiter(corps as Evenement)),
        );
      }
    }
  }
}

function affiche(message: MessageVue): MessageAffiche {
  return { ...message, etat: 'envoye' };
}

function trier(liste: ConversationResume[]): ConversationResume[] {
  return [...liste].sort((a, b) => Date.parse(b.derniereActiviteLe) - Date.parse(a.derniereActiviteLe));
}
