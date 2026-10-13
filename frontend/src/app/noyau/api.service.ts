import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { Compte, ConversationResume, MessageVue } from './modeles';

// Client de l'API REST /api/v1 (chargements, actions) ; le temps réel passe par TempsReelService.
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  comptesDemo(): Promise<Compte[]> {
    return firstValueFrom(this.http.get<Compte[]>(`${this.base}/demo/comptes`));
  }

  conversations(): Promise<ConversationResume[]> {
    return firstValueFrom(this.http.get<ConversationResume[]>(`${this.base}/conversations`));
  }

  messages(conversationId: string): Promise<MessageVue[]> {
    return firstValueFrom(this.http.get<MessageVue[]>(`${this.base}/conversations/${conversationId}/messages`));
  }

  ouvrir(sujet: string, reservationRef: string | null, message: string): Promise<ConversationResume> {
    return firstValueFrom(
      this.http.post<ConversationResume>(`${this.base}/conversations`, { sujet, reservationRef, message }),
    );
  }

  envoyer(conversationId: string, contenu: string, idTemporaire: string): Promise<MessageVue> {
    return firstValueFrom(
      this.http.post<MessageVue>(`${this.base}/conversations/${conversationId}/messages`, { contenu, idTemporaire }),
    );
  }

  prendreEnCharge(conversationId: string): Promise<ConversationResume> {
    return firstValueFrom(
      this.http.post<ConversationResume>(`${this.base}/conversations/${conversationId}/prise-en-charge`, {}),
    );
  }

  clore(conversationId: string): Promise<ConversationResume> {
    return firstValueFrom(this.http.post<ConversationResume>(`${this.base}/conversations/${conversationId}/cloture`, {}));
  }

  marquerLus(conversationId: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${this.base}/conversations/${conversationId}/lecture`, {}));
  }
}

// Message lisible à partir d'une erreur HTTP (ProblemDetail renvoyé par le backend).
export function messageErreur(erreur: unknown): string {
  if (erreur instanceof HttpErrorResponse) {
    const detail = (erreur.error as { detail?: string } | null)?.detail;
    if (detail) {
      return detail;
    }
    if (erreur.status === 0) {
      return 'Le serveur est injoignable. Vérifiez votre connexion.';
    }
    return `Erreur ${erreur.status}.`;
  }
  return 'Une erreur inattendue est survenue.';
}
