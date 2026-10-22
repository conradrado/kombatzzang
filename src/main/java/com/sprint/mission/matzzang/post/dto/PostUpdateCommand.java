package com.sprint.mission.matzzang.post.dto;

import jakarta.validation.constraints.Size;

public record PostUpdateCommand(
        @Size(max = 255)
        String title,

        String body
) {
}
