package com.yourcaryourway.tchat.support;

import java.time.Instant;
import java.util.UUID;

/** Événement de domaine : le lecteur a lu les messages de l'autre partie (accusé de lecture). */
public record MessagesLus(UUID conversationId, TypeAuteur lecteur, Instant luLe) {
}
