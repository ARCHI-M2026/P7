export type Role = 'CLIENT' | 'CONSEILLER';
export type TypeAuteur = Role | 'SYSTEME';
export type StatutConversation = 'EN_ATTENTE' | 'PRISE_EN_CHARGE' | 'CLOSE';

export interface Compte {
  id: string;
  nom: string;
  role: Role;
  detail: string;
}

export interface ConversationResume {
  id: string;
  sujet: string;
  reservationRef: string | null;
  statut: StatutConversation;
  langue: string;
  clientId: string;
  clientNom: string;
  clientJuridiction: string | null;
  conseillerId: string | null;
  conseillerNom: string | null;
  ouverteLe: string;
  priseEnChargeLe: string | null;
  closeLe: string | null;
  derniereActiviteLe: string;
  dernierMessage: string | null;
  dernierAuteur: TypeAuteur | null;
  positionFile: number | null;
  nonLus: number | null;
}

export interface MessageVue {
  id: string;
  conversationId: string;
  typeAuteur: TypeAuteur;
  auteurId: string | null;
  auteurNom: string;
  contenu: string;
  envoyeLe: string;
  luLe: string | null;
}

// Message affiché : ajoute l'état d'envoi (affichage optimiste avant la confirmation du serveur).
export interface MessageAffiche extends MessageVue {
  etat: 'envoi' | 'envoye' | 'echec';
  idTemporaire?: string;
}

// Événements reçus sur /topic/conversations.{id} et /topic/file-attente.
export type Evenement =
  | { type: 'MESSAGE'; message: MessageVue; idTemporaire: string | null }
  | { type: 'CONVERSATION'; conversation: ConversationResume }
  | { type: 'LU'; conversationId: string; lecteur: TypeAuteur; luLe: string }
  | { type: 'SAISIE'; conversationId: string; auteurId: string; typeAuteur: TypeAuteur; auteurNom: string; actif: boolean };

export interface ErreurTempsReel {
  code: string;
  message: string;
}

export const LIBELLES_STATUT: Record<StatutConversation, string> = {
  EN_ATTENTE: 'En attente',
  PRISE_EN_CHARGE: 'En cours',
  CLOSE: 'Close',
};

export const SUJETS = [
  'Modifier une réservation',
  'Annulation et remboursement',
  'Paiement',
  'Accessibilité',
  'Retrait ou retour du véhicule',
  'Autre question',
];
