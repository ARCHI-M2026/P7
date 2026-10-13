package com.yourcaryourway.tchat.support.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.yourcaryourway.tchat.support.TypeAuteur;

@Entity
@Table(schema = "support", name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_auteur", nullable = false)
    private TypeAuteur typeAuteur;

    @Column(name = "auteur_id")
    private UUID auteurId;

    @Column(columnDefinition = "text", nullable = false)
    private String contenu;

    @Column(name = "envoye_le", nullable = false)
    private Instant envoyeLe;

    @Column(name = "lu_le")
    private Instant luLe;

    protected Message() {
        // pour JPA
    }

    public static Message de(UUID conversationId, TypeAuteur type, UUID auteurId, String contenu, Instant maintenant) {
        Message m = new Message();
        m.conversationId = conversationId;
        m.typeAuteur = type;
        m.auteurId = type == TypeAuteur.SYSTEME ? null : auteurId;
        m.contenu = contenu;
        m.envoyeLe = maintenant;
        return m;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public TypeAuteur getTypeAuteur() { return typeAuteur; }
    public UUID getAuteurId() { return auteurId; }
    public String getContenu() { return contenu; }
    public Instant getEnvoyeLe() { return envoyeLe; }
    public Instant getLuLe() { return luLe; }
}
