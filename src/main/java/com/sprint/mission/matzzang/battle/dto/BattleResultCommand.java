package com.sprint.mission.matzzang.battle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BattleResultCommand(
        @NotNull
        @Positive
        Long winnerId
) {
}
