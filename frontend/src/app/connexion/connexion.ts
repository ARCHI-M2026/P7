import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { ApiService, messageErreur } from '../noyau/api.service';
import { Compte } from '../noyau/modeles';
import { couleurAvatar, initiales } from '../noyau/outils';
import { SessionService } from '../noyau/session.service';

/** Écran de connexion de démonstration : choix d'un compte (la cible utilise Keycloak). */
@Component({
  selector: 'app-connexion',
  templateUrl: './connexion.html',
  styleUrl: './connexion.css',
})
export class Connexion implements OnInit {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);

  readonly comptes = signal<Compte[]>([]);
  readonly erreur = signal<string | null>(null);
  readonly chargement = signal(true);
  readonly clients = computed(() => this.comptes().filter((c) => c.role === 'CLIENT'));
  readonly conseillers = computed(() => this.comptes().filter((c) => c.role === 'CONSEILLER'));

  protected readonly initiales = initiales;
  protected readonly couleurAvatar = couleurAvatar;

  async ngOnInit(): Promise<void> {
    await this.charger();
  }

  async charger(): Promise<void> {
    this.chargement.set(true);
    this.erreur.set(null);
    try {
      this.comptes.set(await this.api.comptesDemo());
    } catch (e) {
      this.erreur.set(messageErreur(e));
    } finally {
      this.chargement.set(false);
    }
  }

  choisir(compte: Compte): void {
    this.session.connecter(compte);
    void this.router.navigateByUrl(compte.role === 'CLIENT' ? '/client' : '/conseiller');
  }
}
