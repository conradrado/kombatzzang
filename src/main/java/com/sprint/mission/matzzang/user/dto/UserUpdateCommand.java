package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserUpdateCommand(
        String username,
        String profileImageKey,
        Sport sport,
        @Size(max = 50) String weightClass,
        @Size(max = 100) String activityRegion,
        @Min(0) Integer wins,
        @Min(0) Integer losses,
        UserLevel level,
        @Size(max = 500) String bio
) {
}
