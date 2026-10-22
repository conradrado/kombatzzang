package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.constants.Region;
import jakarta.validation.constraints.NotNull;

public record UserRegionUpdateCommand(
        @NotNull Region region
) {
}
