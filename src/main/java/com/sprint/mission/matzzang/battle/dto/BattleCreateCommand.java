package com.sprint.mission.matzzang.battle.dto;

import com.sprint.mission.matzzang.user.constants.Sport;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record BattleCreateCommand(
        @NotNull
        @Positive
        Long opponentId,

        @NotNull
        Sport sport,

        @Future
        Instant scheduledAt,

        @Size(max = 255)
        String location,

        @Size(max = 1000)
        String message
) {
}
