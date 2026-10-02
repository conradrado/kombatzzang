package com.sprint.mission.matzzang.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.user.constants.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserAccessGuardTest {

    @Mock
    private Authentication authentication;

    private final UserAccessGuard userAccessGuard = new UserAccessGuard();

    @Test
    void 본인_ID로_요청하면_true를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(1L, UserRole.MEMBER));

        assertThat(userAccessGuard.isSelfOrAdmin(authentication, 1L)).isTrue();
    }

    @Test
    void 다른_사용자_ID로_요청하면_false를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(1L, UserRole.MEMBER));

        assertThat(userAccessGuard.isSelfOrAdmin(authentication, 2L)).isFalse();
    }

    @Test
    void ADMIN이면_다른_사용자_ID여도_true를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(1L, UserRole.ADMIN));

        assertThat(userAccessGuard.isSelfOrAdmin(authentication, 2L)).isTrue();
    }

    @Test
    void Authentication이_null이면_false를_반환한다() {
        assertThat(userAccessGuard.isSelfOrAdmin(null, 1L)).isFalse();
    }

    @Test
    void principal이_JwtPrincipal이_아니면_false를_반환한다() {
        given(authentication.getPrincipal()).willReturn("anonymousUser");

        assertThat(userAccessGuard.isSelfOrAdmin(authentication, 1L)).isFalse();
    }
}
