/**
 * Module « Diffusion temps réel » : configuration WebSocket/STOMP, contrôle des abonnements et
 * diffusion des événements du module Support vers les navigateurs.
 * <p>
 * En production, le courtier STOMP est RabbitMQ (relais) : un message traité par l'instance A du backend
 * atteint un navigateur connecté à l'instance B. Sans relais, un courtier en mémoire est utilisé (une seule instance).
 */
@ApplicationModule(displayName = "Diffusion temps réel")
package com.yourcaryourway.tchat.diffusion;

import org.springframework.modulith.ApplicationModule;
