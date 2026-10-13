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

import com.yourcaryourway.tchat.support.StatutConversation;
import com.yourcaryourway.tchat.support.SupportException;

/**
 * Agrégat Conversation : porte les règles de transition de statut.
 * <p>
 * {@code clientId} est une référence logique vers le module Clients (pas de clé étrangère entre modules).
 */
@Entity
@Table(schema = "support", name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "conseiller_id")
    private UUID conseillerId;

    @Column(name = "reservation_ref")
    private String reservationRef;

    private String sujet;

    @Enumerated(EnumType.STRING)
    private StatutConversation statut;

    private String langue;

    @Column(name = "ouverte_le")
    private Instant ouverteLe;

    @Column(name = "prise_en_charge_le")
    private Instant priseEnChargeLe;

    @Column(name = "close_le")
    private Instant closeLe;

    @Column(name = "derniere_activite_le")
    private Instant derniereActiviteLe;

    protected Conversation() {
        // pour JPA
    }

    public static Conversation ouvrir(UUID clientId, String sujet, String reservationRef, String langue, Instant maintenant) {
        Conversation c = new Conversation();
        c.clientId = clientId;
        c.sujet = sujet;
        c.reservationRef = reservationRef;
        c.langue = langue;
        c.statut = StatutConversation.EN_ATTENTE;
        c.ouverteLe = maintenant;
        c.derniereActiviteLe = maintenant;
        return c;
    }

    public void prendreEnCharge(UUID conseiller, Instant maintenant) {
        if (statut != StatutConversation.EN_ATTENTE) {
            throw SupportException.regle(statut == StatutConversation.CLOSE
                    ? "Cette conversation est close."
                    : "Cette conversation est déjà prise en charge par un autre conseiller.");
        }
        this.conseillerId = conseiller;
        this.statut = StatutConversation.PRISE_EN_CHARGE;
        this.priseEnChargeLe = maintenant;
        this.derniereActiviteLe = maintenant;
    }

    public void clore(Instant maintenant) {
        if (statut == StatutConversation.CLOSE) {
            throw SupportException.regle("Cette conversation est déjà close.");
        }
        this.statut = StatutConversation.CLOSE;
        this.closeLe = maintenant;
        this.derniereActiviteLe = maintenant;
    }

    public void enregistrerActivite(Instant maintenant) {
        this.derniereActiviteLe = maintenant;
    }

    public boolean estClose() {
        return statut == StatutConversation.CLOSE;
    }

    public boolean estSuiviePar(UUID conseiller) {
        return conseiller != null && conseiller.equals(conseillerId);
    }

    public UUID getId() { return id; }
    public UUID getClientId() { return clientId; }
    public UUID getConseillerId() { return conseillerId; }
    public String getReservationRef() { return reservationRef; }
    public String getSujet() { return sujet; }
    public StatutConversation getStatut() { return statut; }
    public String getLangue() { return langue; }
    public Instant getOuverteLe() { return ouverteLe; }
    public Instant getPriseEnChargeLe() { return priseEnChargeLe; }
    public Instant getCloseLe() { return closeLe; }
    public Instant getDerniereActiviteLe() { return derniereActiviteLe; }
}
