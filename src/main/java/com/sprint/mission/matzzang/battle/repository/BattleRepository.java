package com.sprint.mission.matzzang.battle.repository;

import com.sprint.mission.matzzang.battle.entity.Battle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleRepository extends JpaRepository<Battle, Long> {
}
