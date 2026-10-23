package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserLevel;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String profileImageKey,
        UserRole role,
        Region region,
        Sport sport,
        String weightClass,
        String activityRegion,
        int wins,
        int losses,
        UserLevel level,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageKey(),
                user.getRole(),
                user.getRegion(),
                user.getSport(),
                user.getWeightClass(),
                user.getActivityRegion(),
                user.getWins(),
                user.getLosses(),
                user.getLevel(),
                user.getBio(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}