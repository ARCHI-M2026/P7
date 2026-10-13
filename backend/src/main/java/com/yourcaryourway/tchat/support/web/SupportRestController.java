package com.yourcaryourway.tchat.support.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yourcaryourway.tchat.support.Acteur;
import com.yourcaryourway.tchat.support.ConversationResume;
import com.yourcaryourway.tchat.support.MessageVue;
import com.yourcaryourway.tchat.support.SupportService;

/**
 * API REST du tchat (/api/v1). Le temps réel passe par STOMP ; ces routes servent au chargement initial,
 * à la reprise après une coupure et aux actions (ouvrir, prendre en charge, clore).
 */
@RestController
@RequestMapping("/api/v1")
class SupportRestController {

    /** PoC : identité transmise par en-tête. Cible : jeton OIDC Keycloak validé par Spring Security. */
    static final String ENTETE_UTILISATEUR = "X-Utilisateur-Id";

    private final SupportService support;

    SupportRestController(SupportService support) {
        this.support = support;
    }

    record NouvelleConversation(
            @NotBlank @Size(max = 120) String sujet,
            @Size(max = 20) String reservationRef,
            @NotBlank @Size(max = 2000) String message) {
    }

    record NouveauMessage(@NotBlank @Size(max = 2000) String contenu, String idTemporaire) {
    }

    @GetMapping("/moi")
    Acteur moi(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur) {
        return support.acteur(utilisateur);
    }

    @GetMapping("/conversations")
    List<ConversationResume> conversations(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur) {
        return support.conversations(support.acteur(utilisateur));
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResume ouvrir(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur,
                              @Valid @RequestBody NouvelleConversation demande) {
        return support.ouvrir(support.acteur(utilisateur), demande.sujet(), demande.reservationRef(), demande.message());
    }

    @GetMapping("/conversations/{id}")
    ConversationResume conversation(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur, @PathVariable UUID id) {
        return support.conversation(support.acteur(utilisateur), id);
    }

    @GetMapping("/conversations/{id}/messages")
    List<MessageVue> messages(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur, @PathVariable UUID id) {
        return support.messages(support.acteur(utilisateur), id);
    }

    /** Alternative REST à l'envoi STOMP (utile pour les tests avec curl). */
    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageVue envoyer(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur, @PathVariable UUID id,
                       @Valid @RequestBody NouveauMessage message) {
        return support.envoyer(support.acteur(utilisateur), id, message.contenu(), message.idTemporaire());
    }

    @PostMapping("/conversations/{id}/prise-en-charge")
    ConversationResume prendreEnCharge(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur, @PathVariable UUID id) {
        return support.prendreEnCharge(support.acteur(utilisateur), id);
    }

    @PostMapping("/conversations/{id}/cloture")
    ConversationResume clore(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur, @PathVariable UUID id) {
        return support.clore(support.acteur(utilisateur), id);
    }

    @PostMapping("/conversations/{id}/lecture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void lecture(@RequestHeader(ENTETE_UTILISATEUR) UUID utilisateur, @PathVariable UUID id) {
        support.marquerLus(support.acteur(utilisateur), id);
    }
}
