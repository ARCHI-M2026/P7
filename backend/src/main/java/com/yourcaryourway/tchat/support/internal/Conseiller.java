package com.yourcaryourway.tchat.support.internal;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Conseiller du service client (la colonne langues n'est pas utile à la preuve de concept). */
@Entity
@Table(schema = "support", name = "conseiller")
public class Conseiller {

    @Id
    private UUID id;

    private String nom;

    private String zone;

    protected Conseiller() {
        // pour JPA
    }

    public UUID getId() { return id; }
    public String getNom() { return nom; }
    public String getZone() { return zone; }
}
