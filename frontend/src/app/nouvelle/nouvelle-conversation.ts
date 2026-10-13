import { Component, ElementRef, afterNextRender, computed, inject, output, signal, viewChild } from '@angular/core';

import { SUJETS } from '../noyau/modeles';
import { TchatStore } from '../tchat/tchat.store';

const FORMAT_RESERVATION = /^YCYW-\d{4}-\d{6}$/;

/** Formulaire d'ouverture d'une conversation (PF-11 étape 1, US-25). */
@Component({
  selector: 'app-nouvelle-conversation',
  templateUrl: './nouvelle-conversation.html',
  styleUrl: './nouvelle-conversation.css',
})
export class NouvelleConversation {
  private readonly store = inject(TchatStore);

  readonly terminee = output<void>();
  readonly annulee = output<void>();

  private readonly premierChamp = viewChild<ElementRef<HTMLSelectElement>>('premierChamp');

  readonly sujets = SUJETS;
  readonly sujet = signal(SUJETS[0]!);
  readonly reservation = signal('');
  readonly message = signal('');
  readonly envoiEnCours = signal(false);
  readonly tentative = signal(false);

  readonly reservationValide = computed(() => {
    const ref = this.reservation().trim().toUpperCase();
    return ref === '' || FORMAT_RESERVATION.test(ref);
  });
  readonly messageValide = computed(() => this.message().trim().length > 0);

  constructor() {
    afterNextRender(() => this.premierChamp()?.nativeElement.focus());
  }

  valeur(evenement: Event): string {
    return (evenement.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
  }

  async envoyer(): Promise<void> {
    this.tentative.set(true);
    if (!this.reservationValide() || !this.messageValide() || this.envoiEnCours()) {
      return;
    }
    this.envoiEnCours.set(true);
    const ref = this.reservation().trim().toUpperCase();
    const ok = await this.store.ouvrir(this.sujet(), ref || null, this.message());
    this.envoiEnCours.set(false);
    if (ok) {
      this.terminee.emit();
    }
  }
}
