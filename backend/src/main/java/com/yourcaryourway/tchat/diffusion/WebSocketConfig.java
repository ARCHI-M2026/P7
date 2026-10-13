package com.yourcaryourway.tchat.diffusion;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Destinations :
 * <ul>
 *   <li>/app/conversations.{id}.messages, .lu, .saisie : commandes envoyées par les navigateurs ;</li>
 *   <li>/topic/conversations.{id} : événements d'une conversation (messages, statut, lecture, saisie) ;</li>
 *   <li>/topic/file-attente : mises à jour pour la console des conseillers ;</li>
 *   <li>/user/queue/erreurs : erreurs propres à la session.</li>
 * </ul>
 * Le séparateur « . » (et non « / ») est celui attendu par RabbitMQ pour les clés de routage.
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(RelaisProperties.class)
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final RelaisProperties relais;
    private final IdentiteStompInterceptor identite;

    WebSocketConfig(RelaisProperties relais, IdentiteStompInterceptor identite) {
        this.relais = relais;
        this.identite = identite;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket natif (sans SockJS) : pas besoin d'affinité de session entre les instances.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (relais.actif()) {
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relais.hote())
                    .setRelayPort(relais.port())
                    .setVirtualHost("/")
                    .setClientLogin(relais.login())
                    .setClientPasscode(relais.motDePasse())
                    .setSystemLogin(relais.login())
                    .setSystemPasscode(relais.motDePasse());
        } else {
            registry.enableSimpleBroker("/topic", "/queue");
        }
        registry.setApplicationDestinationPrefixes("/app");
        registry.setPathMatcher(new AntPathMatcher("."));
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(identite);
    }
}
