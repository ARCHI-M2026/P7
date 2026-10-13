package com.yourcaryourway.tchat.diffusion;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.yourcaryourway.tchat.support.ConversationCloturee;
import com.yourcaryourway.tchat.support.ConversationOuverte;
import com.yourcaryourway.tchat.support.ConversationPriseEnCharge;
import com.yourcaryourway.tchat.support.ConversationResume;
import com.yourcaryourway.tchat.support.MessageEnvoye;
import com.yourcaryourway.tchat.support.MessagesLus;
import com.yourcaryourway.tchat.support.SupportService;

/**
 * Écoute les événements du module Support (après validation de la transaction, de façon asynchrone)
 * et les pousse vers les navigateurs abonnés. Si l'envoi échoue, l'événement reste « incomplet » dans
 * la table event_publication et peut être rejoué.
 */
@Component
public class DiffusionEvenements {

    static final String FILE_ATTENTE = "/topic/file-attente";
    private static final Logger LOG = LoggerFactory.getLogger(DiffusionEvenements.class);

    private final SimpMessagingTemplate stomp;
    private final SupportService support;

    DiffusionEvenements(SimpMessagingTemplate stomp, SupportService support) {
        this.stomp = stomp;
        this.support = support;
    }

    static String sujet(UUID conversationId) {
        return "/topic/conversations." + conversationId;
    }

    @ApplicationModuleListener
    public void surMessage(MessageEnvoye evenement) {
        UUID conversation = evenement.message().conversationId();
        stomp.convertAndSend(sujet(conversation), new Evenements.NouveauMessage(evenement.message(), evenement.idTemporaire()));
        // aperçu du dernier message dans la console des conseillers
        support.resume(conversation).ifPresent(r -> stomp.convertAndSend(FILE_ATTENTE, new Evenements.EtatConversation(r)));
        LOG.debug("Message {} diffusé sur {}", evenement.message().id(), sujet(conversation));
    }

    @ApplicationModuleListener
    public void surOuverture(ConversationOuverte evenement) {
        diffuserEtat(evenement.conversationId());
    }

    @ApplicationModuleListener
    public void surPriseEnCharge(ConversationPriseEnCharge evenement) {
        diffuserEtat(evenement.conversationId());
    }

    @ApplicationModuleListener
    public void surCloture(ConversationCloturee evenement) {
        diffuserEtat(evenement.conversationId());
    }

    @ApplicationModuleListener
    public void surLecture(MessagesLus evenement) {
        stomp.convertAndSend(sujet(evenement.conversationId()),
                new Evenements.Lecture(evenement.conversationId(), evenement.lecteur(), evenement.luLe()));
    }

    /** Nouvel état de la conversation + nouvelles positions de toutes les conversations en attente. */
    private void diffuserEtat(UUID conversationId) {
        support.resume(conversationId).ifPresent(this::publier);
        for (ConversationResume enAttente : support.fileAttente()) {
            if (!enAttente.id().equals(conversationId)) {
                stomp.convertAndSend(sujet(enAttente.id()), new Evenements.EtatConversation(enAttente));
            }
        }
    }

    private void publier(ConversationResume r) {
        Evenements.EtatConversation etat = new Evenements.EtatConversation(r);
        stomp.convertAndSend(sujet(r.id()), etat);
        stomp.convertAndSend(FILE_ATTENTE, etat);
    }
}
