package com.sprint.mission.matzzang.auth.jwt;

import com.sprint.mission.matzzang.user.constants.UserRole;

public record JwtPrincipal(Long userId, UserRole role) {
}
