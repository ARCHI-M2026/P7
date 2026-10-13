package com.yourcaryourway.tchat.support;

/** Erreur métier ou d'accès du module Support ; traduite en code HTTP ou en message STOMP d'erreur. */
public class SupportException extends RuntimeException {

    public enum Raison {
        NON_AUTHENTIFIE,
        ACCES_REFUSE,
        INTROUVABLE,
        REGLE_METIER
    }

    private final Raison raison;

    private SupportException(Raison raison, String message) {
        super(message);
        this.raison = raison;
    }

    public Raison raison() {
        return raison;
    }

    public static SupportException nonAuthentifie() {
        return new SupportException(Raison.NON_AUTHENTIFIE, "Utilisateur inconnu : reconnectez-vous.");
    }

    public static SupportException accesRefuse(String message) {
        return new SupportException(Raison.ACCES_REFUSE, message);
    }

    public static SupportException introuvable() {
        return new SupportException(Raison.INTROUVABLE, "Conversation introuvable.");
    }

    public static SupportException regle(String message) {
        return new SupportException(Raison.REGLE_METIER, message);
    }
}
