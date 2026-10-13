package com.yourcaryourway.tchat.diffusion;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.yourcaryourway.tchat.support.Acteur;
import com.yourcaryourway.tchat.support.SupportService;

/**
 * Indicateur « en train d'écrire » : éphémère, il n'est pas enregistré en base et passe directement
 * par le courtier. Expose aussi le nom de l'instance qui porte la connexion WebSocket (démonstration multi-instance).
 */
@Controller
class SaisieStompController {

    private final SimpMessagingTemplate stomp;
    private final SupportService support;
    private final String instance;

    SaisieStompController(SimpMessagingTemplate stomp, SupportService support) {
        this.stomp = stomp;
        this.support = support;
        this.instance = nomInstance();
    }

    record Saisie(boolean actif) {
    }

    @MessageMapping("conversations.{id}.saisie")
    void saisie(@DestinationVariable UUID id, @Payload Saisie saisie, Principal utilisateur) {
        if (utilisateur == null) {
            return;
        }
        Acteur acteur = support.acteur(UUID.fromString(utilisateur.getName()));
        if (support.peutAcceder(acteur, id)) {
            stomp.convertAndSend(DiffusionEvenements.sujet(id),
                    new Evenements.Saisie(id, acteur.id(), acteur.type(), acteur.nom(), saisie.actif()));
        }
    }

    /** Réponse directe à l'abonnement /app/instance (sans passer par le courtier). */
    @SubscribeMapping("instance")
    Map<String, String> instance() {
        return Map.of("instance", instance);
    }

    private static String nomInstance() {
        String hote = System.getenv("HOSTNAME");
        if (hote != null && !hote.isBlank()) {
            return hote;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "inconnue";
        }
    }
}
