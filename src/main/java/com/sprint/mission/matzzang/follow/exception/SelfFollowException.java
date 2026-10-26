package com.sprint.mission.matzzang.follow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SelfFollowException extends RuntimeException {

    public SelfFollowException() {
        super("자기 자신은 팔로우할 수 없습니다.");
    }
}
