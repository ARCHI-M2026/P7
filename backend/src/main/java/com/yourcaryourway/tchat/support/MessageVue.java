package com.yourcaryourway.tchat.support;

import java.time.Instant;
import java.util.UUID;

public record MessageVue(
        UUID id,
        UUID conversationId,
        TypeAuteur typeAuteur,
        UUID auteurId,
        String auteurNom,
        String contenu,
        Instant envoyeLe,
        Instant luLe) {
}
