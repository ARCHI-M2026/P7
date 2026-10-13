package com.yourcaryourway.tchat.diffusion;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.yourcaryourway.tchat.support.Acteur;
import com.yourcaryourway.tchat.support.SupportException;
import com.yourcaryourway.tchat.support.SupportService;

/**
 * Sécurité du canal temps réel :
 * <ul>
 *   <li>CONNECT : l'en-tête STOMP {@code utilisateur-id} identifie l'utilisateur (cible : jeton OIDC) ;</li>
 *   <li>SUBSCRIBE : un client ne peut écouter que ses conversations ; la file d'attente est réservée aux conseillers.</li>
 * </ul>
 */
@Component
class IdentiteStompInterceptor implements ChannelInterceptor {

    static final String ENTETE_UTILISATEUR = "utilisateur-id";
    private static final Pattern SUJET_CONVERSATION = Pattern.compile("^/topic/conversations\\.([0-9a-fA-F-]{36})$");

    private final SupportService support;

    IdentiteStompInterceptor(SupportService support) {
        this.support = support;
    }

    record Utilisateur(String id) implements Principal {
        @Override
        public String getName() {
            return id;
        }
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor entetes = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (entetes == null || entetes.getCommand() == null) {
            return message;
        }
        switch (entetes.getCommand()) {
            case CONNECT, STOMP -> {
                Acteur acteur = acteur(entetes.getFirstNativeHeader(ENTETE_UTILISATEUR));
                entetes.setUser(new Utilisateur(acteur.id().toString()));
            }
            case SUBSCRIBE -> verifierAbonnement(entetes);
            default -> {
                // SEND, ACK... : l'utilisateur est contrôlé par les contrôleurs
            }
        }
        return message;
    }

    private void verifierAbonnement(StompHeaderAccessor entetes) {
        Principal utilisateur = entetes.getUser();
        String destination = entetes.getDestination();
        if (utilisateur == null || destination == null) {
            throw new MessageDeliveryException("Abonnement refusé : session non authentifiée.");
        }
        Acteur acteur = acteur(utilisateur.getName());
        if (destination.equals("/user/queue/erreurs") || destination.equals("/app/instance")) {
            return;
        }
        if (destination.equals("/topic/file-attente")) {
            if (!acteur.estConseiller()) {
                throw new MessageDeliveryException("Abonnement refusé : file d'attente réservée aux conseillers.");
            }
            return;
        }
        Matcher m = SUJET_CONVERSATION.matcher(destination);
        if (m.matches() && support.peutAcceder(acteur, UUID.fromString(m.group(1)))) {
            return;
        }
        throw new MessageDeliveryException("Abonnement refusé : " + destination);
    }

    private Acteur acteur(String id) {
        try {
            return support.acteur(UUID.fromString(id));
        } catch (IllegalArgumentException | NullPointerException | SupportException e) {
            throw new MessageDeliveryException("Connexion refusée : utilisateur inconnu.");
        }
    }
}
