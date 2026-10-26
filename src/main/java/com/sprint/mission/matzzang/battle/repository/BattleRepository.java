package com.sprint.mission.matzzang.battle.repository;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.entity.Battle;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleRepository extends JpaRepository<Battle, Long> {

    @Query("SELECT b FROM Battle b WHERE b.challengerId = :userId OR b.opponentId = :userId ORDER BY b.id DESC")
    List<Battle> findAllByParticipantId(@Param("userId") Long userId);

    @Query("SELECT b FROM Battle b WHERE (b.challengerId = :userId OR b.opponentId = :userId) AND b.status = :status ORDER BY b.id DESC")
    List<Battle> findAllByParticipantIdAndStatus(@Param("userId") Long userId, @Param("status") BattleStatus status);

    List<Battle> findAllByOpponentIdAndStatusOrderByIdDesc(Long opponentId, BattleStatus status);

    boolean existsByChallengerIdAndOpponentIdAndStatusIn(Long challengerId, Long opponentId, Collection<BattleStatus> statuses);

    long countByWinnerId(Long winnerId);

    @Query("SELECT COUNT(b) FROM Battle b WHERE (b.challengerId = :userId OR b.opponentId = :userId) AND b.status = :status")
    long countByParticipantIdAndStatus(@Param("userId") Long userId, @Param("status") BattleStatus status);
}
