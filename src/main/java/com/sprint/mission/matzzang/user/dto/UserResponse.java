package com.sprint.mission.matzzang.user.dto;

import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.contants.UserRole;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String profileImage,
        UserRole role,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileImage(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}