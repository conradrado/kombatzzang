package com.sprint.mission.matzzang.follow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyFollowingException extends RuntimeException {

    public AlreadyFollowingException() {
        super("이미 팔로우 중인 사용자입니다.");
    }
}
