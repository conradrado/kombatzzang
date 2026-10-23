package com.sprint.mission.matzzang.battle.dto;

import com.sprint.mission.matzzang.user.constants.Sport;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record BattleCreateCommand(
        @NotNull
        Long opponentId,

        @NotNull
        Sport sport,

        Instant scheduledAt,

        @Size(max = 255)
        String location,

        String message
) {
}
