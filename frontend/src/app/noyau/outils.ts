/** Petites fonctions d'affichage partagées. */

const PALETTE = ['#1C4FA6', '#B0301F', '#1E7B45', '#6D1F9E', '#9A5B00', '#0E6A7A'];

export function initiales(nom: string): string {
  return nom
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((partie) => partie[0]!.toUpperCase())
    .join('');
}

// Couleur d'avatar stable pour un même identifiant.
export function couleurAvatar(id: string | null | undefined): string {
  let somme = 0;
  for (const caractere of id ?? '') {
    somme = (somme * 31 + caractere.charCodeAt(0)) >>> 0;
  }
  return PALETTE[somme % PALETTE.length]!;
}

export function heure(iso: string): string {
  return new Date(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

export function jourRelatif(iso: string): string {
  const date = new Date(iso);
  const aujourdhui = new Date();
  const hier = new Date();
  hier.setDate(aujourdhui.getDate() - 1);
  if (date.toDateString() === aujourdhui.toDateString()) {
    return "Aujourd'hui";
  }
  if (date.toDateString() === hier.toDateString()) {
    return 'Hier';
  }
  return date.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
}

// Heure pour aujourd'hui, sinon date courte (listes de conversations).
export function horodatageCourt(iso: string): string {
  const date = new Date(iso);
  return date.toDateString() === new Date().toDateString()
    ? heure(iso)
    : date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
}

export function dureeDepuis(iso: string, maintenant: number): string {
  const minutes = Math.max(0, Math.floor((maintenant - new Date(iso).getTime()) / 60000));
  if (minutes < 1) {
    return "moins d'une minute";
  }
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const heures = Math.floor(minutes / 60);
  return heures < 24 ? `${heures} h ${String(minutes % 60).padStart(2, '0')}` : `${Math.floor(heures / 24)} j`;
}

// Identifiant aléatoire ; crypto.randomUUID n'existe qu'en contexte sécurisé (HTTPS ou localhost).
export function identifiantTemporaire(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' && isSecureContext) {
    return crypto.randomUUID();
  }
  return 'tmp-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
}

export function telechargerTexte(nomFichier: string, contenu: string): void {
  const url = URL.createObjectURL(new Blob([contenu], { type: 'text/plain;charset=utf-8' }));
  const lien = document.createElement('a');
  lien.href = url;
  lien.download = nomFichier;
  lien.click();
  URL.revokeObjectURL(url);
}
