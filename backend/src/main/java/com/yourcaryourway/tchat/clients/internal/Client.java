package com.yourcaryourway.tchat.clients.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Extrait de la table clients.client (le profil complet n'est pas utile au tchat). */
@Entity
@Table(schema = "clients", name = "client")
public class Client {

    @Id
    private UUID id;

    private String email;

    private String nom;

    private String prenom;

    private String langue;

    private String juridiction;

    @Column(name = "cree_le")
    private Instant creeLe;

    protected Client() {
        // pour JPA
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getLangue() { return langue; }
    public String getJuridiction() { return juridiction; }
    public Instant getCreeLe() { return creeLe; }
}
