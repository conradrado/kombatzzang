package com.sprint.mission.matzzang.post.dto;

public record PostCreateCommand(
        String title,
        String body,
        Long userId
) {
}
