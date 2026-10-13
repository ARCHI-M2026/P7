package com.yourcaryourway.tchat.diffusion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Paramètres du relais STOMP vers RabbitMQ (préfixe app.stomp.relais). */
@ConfigurationProperties(prefix = "app.stomp.relais")
public record RelaisProperties(boolean actif, String hote, int port, String login, String motDePasse) {
}
