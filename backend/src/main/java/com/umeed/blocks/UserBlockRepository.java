package com.umeed.blocks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

interface UserBlockRepository extends JpaRepository<UserBlock, UserBlock.Key> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(UUID blockerId);

    @Query("select b.blockedId from UserBlock b where b.blockerId = :userId")
    Set<UUID> findBlockedIdsByBlockerId(@Param("userId") UUID userId);

    @Query("select b.blockerId from UserBlock b where b.blockedId = :userId")
    Set<UUID> findBlockerIdsByBlockedId(@Param("userId") UUID userId);

    @Query("select count(b) > 0 from UserBlock b where (b.blockerId = :a and b.blockedId = :b) "
            + "or (b.blockerId = :b and b.blockedId = :a)")
    boolean existsEitherWay(@Param("a") UUID userId, @Param("b") UUID otherUserId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserBlock b where b.blockerId = :blockerId and b.blockedId = :blockedId")
    int deletePair(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserBlock b where b.blockerId = :userId or b.blockedId = :userId")
    int deleteAllInvolving(@Param("userId") UUID userId);
}
