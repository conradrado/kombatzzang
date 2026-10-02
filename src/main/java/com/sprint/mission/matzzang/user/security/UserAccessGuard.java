package com.sprint.mission.matzzang.user.security;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.user.constants.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {

    public boolean isSelfOrAdmin(Authentication authentication, Long userId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return false;
        }
        return principal.role() == UserRole.ADMIN || principal.userId().equals(userId);
    }
}
