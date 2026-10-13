import {
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  effect,
  inject,
  output,
  signal,
  untracked,
  viewChild,
} from '@angular/core';

import { LIBELLES_STATUT, MessageAffiche } from '../noyau/modeles';
import { couleurAvatar, heure, initiales, jourRelatif, telechargerTexte } from '../noyau/outils';
import { TchatStore } from '../tchat/tchat.store';

interface Ligne {
  message: MessageAffiche;
  jour: string | null;
  debutGroupe: boolean;
  deMoi: boolean;
}

const ECART_GROUPE_MS = 5 * 60_000;

/** Fil de discussion façon messagerie mobile : bulles, accusés de lecture, saisie en cours. */
@Component({
  selector: 'app-fil-discussion',
  templateUrl: './fil-discussion.html',
  styleUrl: './fil-discussion.css',
})
export class FilDiscussion {
  protected readonly store = inject(TchatStore);

  readonly retour = output<void>();
  readonly nouvelleDemande = output<void>();

  private readonly zoneMessages = viewChild<ElementRef<HTMLElement>>('zoneMessages');
  private readonly champ = viewChild<ElementRef<HTMLTextAreaElement>>('champ');
  private collerEnBas = true;

  readonly texte = signal('');
  readonly conversation = computed(() => this.store.conversationActive()!);
  readonly estConseiller = this.store.estConseiller;

  /** Messages enrichis : séparateur de jour, regroupement des messages consécutifs d'un même auteur. */
  readonly lignes = computed<Ligne[]>(() => {
    const messages = this.store.messagesActifs() ?? [];
    const moi = this.store.moi();
    let jourPrecedent = '';
    return messages.map((message, i) => {
      const precedent = messages[i - 1];
      const jour = jourRelatif(message.envoyeLe);
      const nouveauJour = jour !== jourPrecedent;
      jourPrecedent = jour;
      const debutGroupe =
        nouveauJour ||
        !precedent ||
        precedent.typeAuteur !== message.typeAuteur ||
        precedent.auteurId !== message.auteurId ||
        Date.parse(message.envoyeLe) - Date.parse(precedent.envoyeLe) > ECART_GROUPE_MS;
      return {
        message,
        jour: nouveauJour ? jour : null,
        debutGroupe,
        deMoi: message.auteurId === moi.id,
      };
    });
  });

  /** Peut écrire : conversation ouverte, et pour un conseiller, suivie par lui. */
  readonly peutEcrire = computed(() => {
    const c = this.conversation();
    if (c.statut === 'CLOSE') {
      return false;
    }
    return !this.estConseiller() || c.conseillerId === this.store.moi().id;
  });

  protected readonly libelles = LIBELLES_STATUT;
  protected readonly heure = heure;
  protected readonly initiales = initiales;
  protected readonly couleurAvatar = couleurAvatar;

  constructor() {
    // changement de conversation : brouillon vidé, retour en bas du fil
    effect(() => {
      this.store.selection();
      untracked(() => {
        this.texte.set('');
        this.collerEnBas = true;
      });
    });
    // défilement automatique vers le dernier message (sauf si l'utilisateur relit plus haut)
    afterRenderEffect(() => {
      const nombre = this.lignes().length;
      this.store.saisieActive();
      const zone = this.zoneMessages()?.nativeElement;
      if (zone && nombre >= 0 && untracked(() => this.collerEnBas)) {
        zone.scrollTop = zone.scrollHeight;
      }
    });
  }

  surDefilement(): void {
    const zone = this.zoneMessages()?.nativeElement;
    if (zone) {
      this.collerEnBas = zone.scrollHeight - zone.scrollTop - zone.clientHeight < 80;
    }
  }

  surSaisie(evenement: Event): void {
    const valeur = (evenement.target as HTMLTextAreaElement).value;
    this.texte.set(valeur);
    this.store.signalerSaisie(valeur.trim().length > 0);
  }

  surTouche(evenement: KeyboardEvent): void {
    if (evenement.key === 'Enter' && !evenement.shiftKey && !evenement.isComposing) {
      evenement.preventDefault();
      this.envoyer();
    }
  }

  envoyer(): void {
    if (!this.texte().trim()) {
      return;
    }
    this.store.envoyer(this.texte());
    this.texte.set('');
    this.collerEnBas = true;
    const champ = this.champ()?.nativeElement;
    if (champ) {
      champ.value = '';
      champ.focus();
    }
  }

  clore(): void {
    if (confirm('Clore cette conversation ? Vous ne pourrez plus y écrire.')) {
      void this.store.clore(this.conversation().id);
    }
  }

  prendreEnCharge(): void {
    void this.store.prendreEnCharge(this.conversation().id);
  }

  /** US-26 : transcription téléchargeable. */
  telechargerTranscription(): void {
    const c = this.conversation();
    const lignes = (this.store.messagesActifs() ?? []).map(
      (m) => `[${new Date(m.envoyeLe).toLocaleString('fr-FR')}] ${m.auteurNom} : ${m.contenu}`,
    );
    const entete = [
      'Your Car Your Way - transcription de conversation',
      `Sujet : ${c.sujet}`,
      c.reservationRef ? `Réservation : ${c.reservationRef}` : null,
      `Client : ${c.clientNom}`,
      c.conseillerNom ? `Conseiller : ${c.conseillerNom}` : null,
      '',
    ].filter((l) => l !== null);
    telechargerTexte(`conversation-${c.reservationRef ?? c.id.slice(0, 8)}.txt`, [...entete, ...lignes].join('\n'));
  }

  titre(): string {
    const c = this.conversation();
    return this.estConseiller() ? c.clientNom : (c.conseillerNom ?? 'Service client');
  }

  idAvatar(): string | null {
    const c = this.conversation();
    return this.estConseiller() ? c.clientId : c.conseillerId;
  }

  estimationAttente(position: number): string {
    const minutes = position * 3;
    return minutes <= 3 ? 'environ 3 minutes' : `environ ${minutes} minutes`;
  }

  etatLecture(m: MessageAffiche): string {
    if (m.etat === 'envoi') {
      return 'Envoi en cours';
    }
    if (m.etat === 'echec') {
      return "Échec de l'envoi";
    }
    return m.luLe ? 'Lu' : 'Envoyé';
  }
}
