package com.umeed.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Oldest first. Messages sent in the same instant are ordered by id, so the order never changes. */
    List<ChatMessage> findByClaimIdOrderByCreatedAtAscIdAsc(UUID claimId);

    /** The messages that come after the given position, in the same order as above. */
    @Query("""
            select m from ChatMessage m
            where m.claimId = :claimId
              and (m.createdAt > :createdAt or (m.createdAt = :createdAt and m.id > :id))
            order by m.createdAt asc, m.id asc
            """)
    List<ChatMessage> findAfter(
            @Param("claimId") UUID claimId, @Param("createdAt") Instant createdAt, @Param("id") UUID id);

    Optional<ChatMessage> findFirstByClaimIdOrderByCreatedAtDescIdDesc(UUID claimId);

    boolean existsByClaimId(UUID claimId);

    @Query("select distinct m.claimId from ChatMessage m where m.claimId in :claimIds")
    Set<UUID> findClaimIdsWithMessages(@Param("claimIds") Collection<UUID> claimIds);

    @Query("select count(m) from ChatMessage m where m.claimId = :claimId and m.senderId <> :userId and m.readAt is null")
    long countUnread(@Param("claimId") UUID claimId, @Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ChatMessage m set m.readAt = :now "
            + "where m.claimId = :claimId and m.senderId <> :userId and m.readAt is null")
    int markRead(@Param("claimId") UUID claimId, @Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ChatMessage m where m.senderId = :userId")
    int deleteBySenderId(@Param("userId") UUID userId);
}
