package com.sprint.mission.matzzang.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.matzzang.user.constants.UserRole;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-key-for-unit-test-must-be-long-enough-256bit";

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider(SECRET, 1_000 * 60, 1_000 * 60 * 60);

    @Test
    void AccessToken을_발급하면_userId와_role이_포함된다() {
        String token = jwtTokenProvider.generateAccessToken(1L, UserRole.MEMBER);

        JwtPrincipal principal = jwtTokenProvider.parse(token);

        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    void RefreshToken을_발급하면_role_claim이_없다() {
        String token = jwtTokenProvider.generateRefreshToken(1L);

        JwtPrincipal principal = jwtTokenProvider.parse(token);

        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isNull();
    }

    @Test
    void 만료된_토큰을_parse하면_예외가_발생한다() {
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(SECRET, -1_000, -1_000);
        String expiredToken = expiredTokenProvider.generateAccessToken(1L, UserRole.MEMBER);

        assertThatThrownBy(() -> jwtTokenProvider.parse(expiredToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 다른_시크릿으로_서명된_토큰을_parse하면_예외가_발생한다() {
        JwtTokenProvider otherProvider =
                new JwtTokenProvider("another-jwt-secret-key-for-unit-test-must-be-long-enough", 1_000 * 60, 1_000 * 60);
        String token = otherProvider.generateAccessToken(1L, UserRole.MEMBER);

        assertThatThrownBy(() -> jwtTokenProvider.parse(token))
                .isInstanceOf(JwtException.class);
    }
}
