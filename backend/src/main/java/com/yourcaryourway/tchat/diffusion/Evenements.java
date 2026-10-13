package com.yourcaryourway.tchat.diffusion;

import java.time.Instant;
import java.util.UUID;

import com.yourcaryourway.tchat.support.ConversationResume;
import com.yourcaryourway.tchat.support.MessageVue;
import com.yourcaryourway.tchat.support.TypeAuteur;

/** Enveloppes JSON envoyées aux navigateurs ; le champ {@code type} permet l'aiguillage côté Angular. */
final class Evenements {

    private Evenements() {
    }

    record NouveauMessage(String type, MessageVue message, String idTemporaire) {
        NouveauMessage(MessageVue message, String idTemporaire) {
            this("MESSAGE", message, idTemporaire);
        }
    }

    record EtatConversation(String type, ConversationResume conversation) {
        EtatConversation(ConversationResume conversation) {
            this("CONVERSATION", conversation);
        }
    }

    record Lecture(String type, UUID conversationId, TypeAuteur lecteur, Instant luLe) {
        Lecture(UUID conversationId, TypeAuteur lecteur, Instant luLe) {
            this("LU", conversationId, lecteur, luLe);
        }
    }

    record Saisie(String type, UUID conversationId, UUID auteurId, TypeAuteur typeAuteur, String auteurNom, boolean actif) {
        Saisie(UUID conversationId, UUID auteurId, TypeAuteur typeAuteur, String auteurNom, boolean actif) {
            this("SAISIE", conversationId, auteurId, typeAuteur, auteurNom, actif);
        }
    }
}
