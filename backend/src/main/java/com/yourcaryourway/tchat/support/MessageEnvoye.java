package com.yourcaryourway.tchat.support;

/**
 * Événement de domaine : un message a été enregistré.
 *
 * @param idTemporaire identifiant généré par le navigateur de l'émetteur, pour remplacer sa bulle
 *                     « en cours d'envoi » par le message confirmé (peut être {@code null})
 */
public record MessageEnvoye(MessageVue message, String idTemporaire) {
}
