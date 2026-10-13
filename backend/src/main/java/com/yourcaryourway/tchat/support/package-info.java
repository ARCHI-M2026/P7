/**
 * Module « Support client (tchat) » : conversations, messages, file d'attente des conseillers.
 * <p>
 * Interface publique (package racine) : {@link com.yourcaryourway.tchat.support.SupportService}, les vues
 * ({@code ConversationResume}, {@code MessageVue}, {@code Acteur}) et les événements de domaine
 * ({@code ConversationOuverte}, {@code MessageEnvoye}...). Les entités JPA ({@code internal}) et les
 * contrôleurs ({@code web}) ne sont pas accessibles aux autres modules.
 */
@ApplicationModule(displayName = "Support client (tchat)")
package com.yourcaryourway.tchat.support;

import org.springframework.modulith.ApplicationModule;
