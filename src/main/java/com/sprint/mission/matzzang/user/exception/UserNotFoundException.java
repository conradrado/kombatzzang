package com.sprint.mission.matzzang.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. id=" + userId);
    }
}
