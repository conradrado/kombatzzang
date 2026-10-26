package com.sprint.mission.matzzang.follow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FollowCreateRequest(
        @NotNull
        @Positive
        Long followeeId
) {
}
