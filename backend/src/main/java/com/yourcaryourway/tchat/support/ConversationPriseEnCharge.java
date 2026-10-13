package com.yourcaryourway.tchat.support;

import java.util.UUID;

/** Événement de domaine : un conseiller a pris la conversation en charge. */
public record ConversationPriseEnCharge(UUID conversationId, UUID conseillerId) {
}
