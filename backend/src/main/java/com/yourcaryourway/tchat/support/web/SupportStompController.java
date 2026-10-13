package com.yourcaryourway.tchat.support.web;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.yourcaryourway.tchat.support.Acteur;
import com.yourcaryourway.tchat.support.SupportException;
import com.yourcaryourway.tchat.support.SupportService;

/**
 * Commandes reçues par WebSocket (STOMP). Le client envoie sur /app/conversations.{id}.messages ; la réponse
 * n'est pas renvoyée ici : le message enregistré est diffusé à tous les abonnés de
 * /topic/conversations.{id} par le module Diffusion, à partir de l'événement MessageEnvoye.
 */
@Controller
class SupportStompController {

    private final SupportService support;

    SupportStompController(SupportService support) {
        this.support = support;
    }

    record EnvoiMessage(String contenu, String idTemporaire) {
    }

    @MessageMapping("conversations.{id}.messages")
    void envoyer(@DestinationVariable UUID id, @Payload EnvoiMessage envoi, Principal utilisateur) {
        support.envoyer(acteur(utilisateur), id, envoi.contenu(), envoi.idTemporaire());
    }

    @MessageMapping("conversations.{id}.lu")
    void lu(@DestinationVariable UUID id, Principal utilisateur) {
        support.marquerLus(acteur(utilisateur), id);
    }

    /** Erreur renvoyée uniquement à la session émettrice, sur /user/queue/erreurs. */
    @MessageExceptionHandler(SupportException.class)
    @SendToUser(destinations = "/queue/erreurs", broadcast = false)
    Map<String, String> erreur(SupportException e) {
        return Map.of("code", e.raison().name(), "message", e.getMessage());
    }

    private Acteur acteur(Principal utilisateur) {
        if (utilisateur == null) {
            throw SupportException.nonAuthentifie();
        }
        return support.acteur(UUID.fromString(utilisateur.getName()));
    }
}
