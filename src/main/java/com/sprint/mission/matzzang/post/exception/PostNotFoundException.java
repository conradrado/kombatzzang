package com.sprint.mission.matzzang.post.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long postId) {
        super("게시글을 찾을 수 없습니다. id=" + postId);
    }
}
