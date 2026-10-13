package com.yourcaryourway.tchat.support;

import java.util.UUID;

/** Événement de domaine : un client a ouvert une conversation (elle entre dans la file d'attente). */
public record ConversationOuverte(UUID conversationId, UUID clientId) {
}
