import { Component, OnDestroy, OnInit, computed, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { FilDiscussion } from '../fil/fil-discussion';
import { LIBELLES_STATUT } from '../noyau/modeles';
import { couleurAvatar, dureeDepuis, horodatageCourt, initiales } from '../noyau/outils';
import { SessionService } from '../noyau/session.service';
import { NouvelleConversation } from '../nouvelle/nouvelle-conversation';
import { TchatStore } from './tchat.store';

type Onglet = 'file' | 'miennes' | 'historique';

/** Écran principal : espace client (/client) ou console conseiller (/conseiller). */
@Component({
  selector: 'app-tchat',
  imports: [FilDiscussion, NouvelleConversation],
  providers: [TchatStore],
  templateUrl: './tchat.html',
  styleUrl: './tchat.css',
  host: {
    '(document:visibilitychange)': 'surVisibilite()',
  },
})
export class Tchat implements OnInit, OnDestroy {
  protected readonly store = inject(TchatStore);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly titre = inject(Title);
  private readonly titreInitial = this.titre.getTitle();

  readonly onglet = signal<Onglet>('file');
  readonly nouvelleDemande = signal(false);

  readonly file = computed(() =>
    this.store
      .conversations()
      .filter((c) => c.statut === 'EN_ATTENTE')
      .sort((a, b) => Date.parse(a.ouverteLe) - Date.parse(b.ouverteLe)),
  );
  readonly miennes = computed(() =>
    this.store.conversations().filter((c) => c.statut === 'PRISE_EN_CHARGE' && c.conseillerId === this.store.moi().id),
  );
  readonly historique = computed(() =>
    this.store
      .conversations()
      .filter((c) => c.statut === 'CLOSE' || (c.statut === 'PRISE_EN_CHARGE' && c.conseillerId !== this.store.moi().id)),
  );
  readonly liste = computed(() => {
    if (!this.store.estConseiller()) {
      return this.store.conversations();
    }
    switch (this.onglet()) {
      case 'file':
        return this.file();
      case 'miennes':
        return this.miennes();
      default:
        return this.historique();
    }
  });
  readonly afficheFil = computed(() => this.nouvelleDemande() || this.store.selection() !== null);

  protected readonly libelles = LIBELLES_STATUT;
  protected readonly initiales = initiales;
  protected readonly couleurAvatar = couleurAvatar;
  protected readonly horodatageCourt = horodatageCourt;
  protected readonly dureeDepuis = dureeDepuis;

  constructor() {
    // nombre de messages non lus dans le titre de l'onglet
    effect(() => {
      const n = this.store.totalNonLus();
      this.titre.setTitle(n > 0 ? `(${n}) ${this.titreInitial}` : this.titreInitial);
    });
  }

  ngOnInit(): void {
    this.store.demarrer();
  }

  ngOnDestroy(): void {
    this.store.arreter();
    this.titre.setTitle(this.titreInitial);
  }

  ouvrir(id: string): void {
    this.nouvelleDemande.set(false);
    void this.store.selectionner(id);
  }

  demarrerNouvelleDemande(): void {
    void this.store.selectionner(null);
    this.nouvelleDemande.set(true);
  }

  retourALaListe(): void {
    this.nouvelleDemande.set(false);
    void this.store.selectionner(null);
  }

  deconnexion(): void {
    this.store.arreter();
    this.session.deconnecter();
    void this.router.navigateByUrl('/');
  }

  surVisibilite(): void {
    const id = this.store.selection();
    if (id && document.visibilityState === 'visible') {
      this.store.marquerLu(id);
    }
  }

  interlocuteur(c: { clientNom: string; conseillerNom: string | null }): string {
    return this.store.estConseiller() ? c.clientNom : (c.conseillerNom ?? 'Service client');
  }
}
