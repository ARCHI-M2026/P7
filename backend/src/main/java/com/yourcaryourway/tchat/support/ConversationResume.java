package com.yourcaryourway.tchat.support;

import java.time.Instant;
import java.util.UUID;

/**
 * Vue d'une conversation pour les listes et l'en-tête du fil.
 *
 * @param positionFile position dans la file d'attente (1 = prochaine prise en charge), {@code null} hors attente
 * @param nonLus       messages de l'autre partie non lus par le lecteur, {@code null} quand la vue est diffusée
 */
public record ConversationResume(
        UUID id,
        String sujet,
        String reservationRef,
        StatutConversation statut,
        String langue,
        UUID clientId,
        String clientNom,
        String clientJuridiction,
        UUID conseillerId,
        String conseillerNom,
        Instant ouverteLe,
        Instant priseEnChargeLe,
        Instant closeLe,
        Instant derniereActiviteLe,
        String dernierMessage,
        TypeAuteur dernierAuteur,
        Integer positionFile,
        Long nonLus) {
}
