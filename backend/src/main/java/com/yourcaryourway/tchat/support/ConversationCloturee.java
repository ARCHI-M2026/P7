package com.yourcaryourway.tchat.support;

import java.util.UUID;

/** Événement de domaine : la conversation est close (par le client ou par le conseiller). */
public record ConversationCloturee(UUID conversationId, UUID clientId, TypeAuteur closePar) {
}
