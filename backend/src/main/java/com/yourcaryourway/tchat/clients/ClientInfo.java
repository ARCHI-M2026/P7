package com.yourcaryourway.tchat.clients;

import java.util.UUID;

/**
 * Vue publique d'un client.
 *
 * @param juridiction territoire dont la loi s'applique aux données (FR, CA-QC, US-CA, GB...)
 */
public record ClientInfo(UUID id, String prenom, String nom, String email, String langue, String juridiction) {

    public String nomComplet() {
        return prenom + " " + nom;
    }
}
