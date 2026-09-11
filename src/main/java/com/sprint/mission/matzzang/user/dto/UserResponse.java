package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String profileImageKey,
        UserRole role,
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
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}