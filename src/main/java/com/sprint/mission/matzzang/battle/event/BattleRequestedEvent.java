package com.sprint.mission.matzzang.battle.event;

import com.sprint.mission.matzzang.battle.dto.BattleResponse;

public record BattleRequestedEvent(Long opponentId, BattleResponse battle) {
}
