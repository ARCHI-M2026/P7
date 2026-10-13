package com.yourcaryourway.tchat.support.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yourcaryourway.tchat.support.TypeAuteur;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByEnvoyeLeAsc(UUID conversationId);

    Optional<Message> findFirstByConversationIdOrderByEnvoyeLeDesc(UUID conversationId);

    long countByConversationIdAndTypeAuteurAndLuLeIsNull(UUID conversationId, TypeAuteur typeAuteur);

    @Modifying
    @Query("""
            update Message m set m.luLe = :luLe
            where m.conversationId = :conversationId and m.typeAuteur = :auteur and m.luLe is null
            """)
    int marquerLus(@Param("conversationId") UUID conversationId,
                   @Param("auteur") TypeAuteur auteur,
                   @Param("luLe") Instant luLe);
}
