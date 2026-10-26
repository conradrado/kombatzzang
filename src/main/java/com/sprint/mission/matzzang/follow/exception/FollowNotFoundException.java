package com.sprint.mission.matzzang.follow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FollowNotFoundException extends RuntimeException {

    public FollowNotFoundException() {
        super("팔로우 관계를 찾을 수 없습니다.");
    }
}
