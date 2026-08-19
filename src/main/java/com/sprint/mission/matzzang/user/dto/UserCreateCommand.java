package com.sprint.mission.matzzang.user.dto;

public record UserCreateRequest(
        String username,
        String email,
        String password,
        String profileImage
) {
}