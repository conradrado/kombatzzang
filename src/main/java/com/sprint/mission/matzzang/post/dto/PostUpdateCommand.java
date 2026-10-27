package com.sprint.mission.matzzang.post.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostUpdateCommand(
        // null은 변경 없음(PATCH), 공백만 있는 문자열은 거부한다
        @Size(max = 255)
        @Pattern(regexp = "(?s).*\\S.*")
        String title,

        @Size(max = 10000)
        String body
) {
}
