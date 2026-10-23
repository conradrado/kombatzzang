package com.sprint.mission.matzzang.battle.dto;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.user.constants.Sport;
import java.time.Instant;

public record BattleResponse(
        Long id,
        Long challengerId,
        Long opponentId,
        Sport sport,
        Instant scheduledAt,
        String location,
        String message,
        BattleStatus status,
        Long winnerId,
        Instant createdAt
) {

    public static BattleResponse from(Battle battle) {
        return new BattleResponse(
                battle.getId(),
                battle.getChallengerId(),
                battle.getOpponentId(),
                battle.getSport(),
                battle.getScheduledAt(),
                battle.getLocation(),
                battle.getMessage(),
                battle.getStatus(),
                battle.getWinnerId(),
                battle.getCreatedAt()
        );
    }
}
