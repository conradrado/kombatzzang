package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.constants.UserLevel;
import jakarta.validation.constraints.Min;

public record UserRecordUpdateCommand(
        @Min(0) Integer wins,
        @Min(0) Integer losses,
        UserLevel level
) {
}
