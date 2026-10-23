package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateCommand(
        @NotBlank
        @Size(min = 2, max = 20)
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password,

        String profileImageKey,

        Region region,

        Sport sport,

        @Size(max = 50) String weightClass,

        @Size(max = 100) String activityRegion
) {
}