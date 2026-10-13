import { Injectable, signal } from '@angular/core';

import { Compte } from './modeles';

const CLE = 'ycyw.compte';

/**
 * Compte connecté. Conservé dans sessionStorage : chaque onglet a sa propre session,
 * ce qui permet d'ouvrir un client et un conseiller côte à côte dans le même navigateur.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly compte = signal<Compte | null>(SessionService.lire());

  connecter(compte: Compte): void {
    this.compte.set(compte);
    try {
      sessionStorage.setItem(CLE, JSON.stringify(compte));
    } catch {
      // stockage indisponible (navigation privée stricte) : la session reste en mémoire
    }
  }

  deconnecter(): void {
    this.compte.set(null);
    try {
      sessionStorage.removeItem(CLE);
    } catch {
      // rien à faire
    }
  }

  private static lire(): Compte | null {
    try {
      const brut = sessionStorage.getItem(CLE);
      return brut ? (JSON.parse(brut) as Compte) : null;
    } catch {
      return null;
    }
  }
}
