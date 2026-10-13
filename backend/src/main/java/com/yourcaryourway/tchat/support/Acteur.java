package com.yourcaryourway.tchat.support;

import java.util.UUID;

/**
 * Utilisateur authentifié du tchat (client ou conseiller).
 * <p>
 * Dans la cible, l'identité vient du jeton OpenID Connect émis par Keycloak ; dans la preuve de concept,
 * elle est transmise par l'en-tête {@code X-Utilisateur-Id}.
 */
public record Acteur(UUID id, TypeAuteur type, String nom, String detail) {

    public boolean estClient() {
        return type == TypeAuteur.CLIENT;
    }

    public boolean estConseiller() {
        return type == TypeAuteur.CONSEILLER;
    }
}
