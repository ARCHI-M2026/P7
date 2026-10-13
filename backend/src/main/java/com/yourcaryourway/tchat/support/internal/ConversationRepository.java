package com.yourcaryourway.tchat.support.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yourcaryourway.tchat.support.StatutConversation;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByClientIdOrderByDerniereActiviteLeDesc(UUID clientId);

    List<Conversation> findTop200ByOrderByDerniereActiviteLeDesc();

    /** File d'attente : la plus ancienne demande est servie en premier. */
    List<Conversation> findByStatutOrderByOuverteLeAsc(StatutConversation statut);

    /**
     * Verrou en écriture (SELECT ... FOR UPDATE) : si deux conseillers cliquent en même temps sur
     * « Prendre en charge », le second attend puis reçoit une erreur métier.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> verrouiller(@Param("id") UUID id);
}
