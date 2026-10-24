package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.WeightClass;
import jakarta.validation.constraints.Size;

public record UserUpdateCommand(
        String username,
        String profileImageKey,
        Sport sport,
        WeightClass weightClass,
        Region activityRegion,
        @Size(max = 500) String bio
) {
}
