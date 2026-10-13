package com.yourcaryourway.tchat.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.yourcaryourway.tchat.clients.Clients;
import com.yourcaryourway.tchat.support.ConversationCloturee;
import com.yourcaryourway.tchat.support.ConversationOuverte;

/**
 * Exemple de module découplé : le module Support ignore l'existence des notifications.
 * Il publie des événements ; ce module s'y abonne.
 */
@Component
public class NotificationsTchat {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsTchat.class);

    private final Clients clients;

    NotificationsTchat(Clients clients) {
        this.clients = clients;
    }

    @ApplicationModuleListener
    public void surOuverture(ConversationOuverte evenement) {
        clients.trouver(evenement.clientId()).ifPresent(client -> LOG.info(
                "[E-mail simulé] À {} ({}) : « Nous avons bien reçu votre demande, un conseiller vous répond dans le tchat. » (conversation {})",
                client.email(), client.langue(), evenement.conversationId()));
    }

    @ApplicationModuleListener
    public void surCloture(ConversationCloturee evenement) {
        clients.trouver(evenement.clientId()).ifPresent(client -> LOG.info(
                "[E-mail simulé] À {} ({}) : « Votre conversation est close, la transcription est disponible dans votre espace. » (conversation {})",
                client.email(), client.langue(), evenement.conversationId()));
    }
}
