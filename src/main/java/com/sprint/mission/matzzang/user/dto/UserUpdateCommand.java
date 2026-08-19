package com.sprint.mission.matzzang.user.dto;

public record UserUpdateCommand(
        String username,
        String profileImage
) {
}