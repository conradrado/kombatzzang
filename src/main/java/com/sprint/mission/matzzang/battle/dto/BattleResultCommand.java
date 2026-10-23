package com.sprint.mission.matzzang.battle.dto;

import jakarta.validation.constraints.NotNull;

public record BattleResultCommand(
        @NotNull
        Long winnerId
) {
}
