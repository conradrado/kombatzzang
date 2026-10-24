package com.sprint.mission.matzzang.stat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserStatNotFoundException extends RuntimeException {

    public UserStatNotFoundException(Long userId) {
        super("전적을 찾을 수 없습니다. userId=" + userId);
    }
}
