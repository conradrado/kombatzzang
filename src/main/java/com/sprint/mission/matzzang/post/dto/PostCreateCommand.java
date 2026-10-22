package com.sprint.mission.matzzang.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateCommand(
        @NotBlank
        @Size(max = 255)
        String title,

        String body
) {
}
