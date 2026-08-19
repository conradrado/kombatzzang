package com.sprint.mission.matzzang.user.dto;

public record UserCreateCommand(
        String username,
        String email,
        String password,
        String profileImage
) {
}