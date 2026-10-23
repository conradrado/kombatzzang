package com.sprint.mission.matzzang.battle.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BattleNotFoundException extends RuntimeException {

    public BattleNotFoundException(Long battleId) {
        super("대전을 찾을 수 없습니다. id=" + battleId);
    }
}
