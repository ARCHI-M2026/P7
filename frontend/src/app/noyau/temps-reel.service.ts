import { Injectable, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

import { ErreurTempsReel } from './modeles';

interface Abonnement {
  rappel: (corps: unknown) => void;
  stomp?: StompSubscription;
}

/**
 * Connexion WebSocket / STOMP au backend.
 * - reconnexion automatique (3 s) ; après une reconnexion, les écrans rechargent l'historique (aucune perte) ;
 * - les abonnements sont mémorisés et rétablis après chaque reconnexion.
 */
@Injectable({ providedIn: 'root' })
export class TempsReelService {
  /** État de la connexion, affiché dans l'en-tête. */
  readonly connecte = signal(false);
  /** Instance du backend qui porte la connexion (démonstration du relais RabbitMQ). */
  readonly instance = signal<string | null>(null);

  private client: Client | null = null;
  private readonly abonnements = new Map<string, Abonnement>();
  private surConnexion: (premiere: boolean) => void = () => {};
  private surErreur: (erreur: ErreurTempsReel) => void = () => {};
  private dejaConnecte = false;

  connecter(
    utilisateurId: string,
    surConnexion: (premiere: boolean) => void,
    surErreur: (erreur: ErreurTempsReel) => void,
  ): void {
    this.deconnecter();
    this.surConnexion = surConnexion;
    this.surErreur = surErreur;
    this.dejaConnecte = false;

    const protocole = location.protocol === 'https:' ? 'wss' : 'ws';
    this.client = new Client({
      brokerURL: `${protocole}://${location.host}/ws`,
      connectHeaders: { 'utilisateur-id': utilisateurId },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => this.connexionEtablie(),
      onWebSocketClose: () => this.connecte.set(false),
      onStompError: (trame) => console.warn('Erreur STOMP :', trame.headers['message'] ?? trame.body),
    });
    this.client.activate();
  }

  deconnecter(): void {
    this.abonnements.clear();
    this.connecte.set(false);
    this.instance.set(null);
    if (this.client) {
      void this.client.deactivate();
      this.client = null;
    }
  }

  /** S'abonne à une destination ; renvoie la fonction de désabonnement. */
  ecouter(destination: string, rappel: (corps: unknown) => void): () => void {
    this.abonnements.get(destination)?.stomp?.unsubscribe();
    const abonnement: Abonnement = { rappel };
    this.abonnements.set(destination, abonnement);
    if (this.client?.connected) {
      abonnement.stomp = this.abonner(destination, rappel);
    }
    return () => {
      abonnement.stomp?.unsubscribe();
      if (this.abonnements.get(destination) === abonnement) {
        this.abonnements.delete(destination);
      }
    };
  }

  estAbonne(destination: string): boolean {
    return this.abonnements.has(destination);
  }

  /** Envoie une commande ; renvoie false si la connexion n'est pas établie (l'appelant passe alors par REST). */
  publier(destination: string, corps: unknown): boolean {
    if (!this.client?.connected) {
      return false;
    }
    this.client.publish({ destination, body: JSON.stringify(corps) });
    return true;
  }

  private connexionEtablie(): void {
    const client = this.client;
    if (!client) {
      return;
    }
    this.connecte.set(true);
    client.subscribe('/user/queue/erreurs', (m) => this.surErreur(JSON.parse(m.body) as ErreurTempsReel));
    client.subscribe('/app/instance', (m) => this.instance.set((JSON.parse(m.body) as { instance: string }).instance));
    for (const [destination, abonnement] of this.abonnements) {
      abonnement.stomp = this.abonner(destination, abonnement.rappel);
    }
    const premiere = !this.dejaConnecte;
    this.dejaConnecte = true;
    this.surConnexion(premiere);
  }

  private abonner(destination: string, rappel: (corps: unknown) => void): StompSubscription | undefined {
    return this.client?.subscribe(destination, (message: IMessage) => rappel(JSON.parse(message.body)));
  }
}
