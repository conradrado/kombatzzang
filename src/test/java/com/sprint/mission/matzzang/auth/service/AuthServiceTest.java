package com.sprint.mission.matzzang.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.auth.dto.LoginRequest;
import com.sprint.mission.matzzang.auth.dto.RefreshRequest;
import com.sprint.mission.matzzang.auth.dto.TokenResponse;
import com.sprint.mission.matzzang.auth.entity.RefreshToken;
import com.sprint.mission.matzzang.auth.exception.InvalidCredentialsException;
import com.sprint.mission.matzzang.auth.exception.InvalidRefreshTokenException;
import com.sprint.mission.matzzang.auth.jwt.JwtTokenProvider;
import com.sprint.mission.matzzang.auth.repository.RefreshTokenRepository;
import com.sprint.mission.matzzang.auth.security.CustomUserDetails;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomUserDetails customUserDetails;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtTokenProvider, refreshTokenRepository, userRepository);
    }

    @Test
    void 로그인에_성공하면_AccessToken과_RefreshToken을_발급하고_저장한다() {
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(customUserDetails);
        given(customUserDetails.getUserId()).willReturn(1L);
        given(customUserDetails.getRole()).willReturn(UserRole.MEMBER);
        given(jwtTokenProvider.generateAccessToken(1L, UserRole.MEMBER)).willReturn("access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(1_800_000L);
        given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1_209_600_000L);

        TokenResponse response = authService.login(new LoginRequest("test@test.com", "password"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(1_800L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getToken()).isEqualTo("refresh-token");
    }

    @Test
    void 이메일_또는_비밀번호가_올바르지_않으면_InvalidCredentialsException이_발생한다() {
        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("인증 실패"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@test.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void 유효한_RefreshToken으로_재발급하면_기존_토큰은_폐기되고_새_토큰이_발급된다() {
        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .token("old-refresh-token")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        User user = User.builder().username("tester").email("test@test.com").password("encoded").role(UserRole.MEMBER).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(refreshTokenRepository.findByToken("old-refresh-token")).willReturn(Optional.of(savedToken));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.generateAccessToken(1L, UserRole.MEMBER)).willReturn("new-access-token");
        given(jwtTokenProvider.generateRefreshToken(1L)).willReturn("new-refresh-token");
        given(jwtTokenProvider.getAccessTokenExpirationMs()).willReturn(1_800_000L);
        given(jwtTokenProvider.getRefreshTokenExpirationMs()).willReturn(1_209_600_000L);

        TokenResponse response = authService.refresh(new RefreshRequest("old-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(savedToken.isUsable()).isFalse();
    }

    @Test
    void 존재하지_않는_RefreshToken으로_재발급하면_InvalidRefreshTokenException이_발생한다() {
        given(refreshTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void 이미_폐기된_RefreshToken으로_재발급하면_InvalidRefreshTokenException이_발생한다() {
        RefreshToken revokedToken = RefreshToken.builder()
                .userId(1L)
                .token("revoked-token")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        revokedToken.revoke();
        given(refreshTokenRepository.findByToken("revoked-token")).willReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("revoked-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void RefreshToken의_사용자가_존재하지_않으면_UserNotFoundException이_발생한다() {
        RefreshToken savedToken = RefreshToken.builder()
                .userId(999L)
                .token("orphan-token")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByToken("orphan-token")).willReturn(Optional.of(savedToken));
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("orphan-token")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void 로그아웃하면_RefreshToken이_폐기된다() {
        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .token("logout-token")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByToken("logout-token")).willReturn(Optional.of(savedToken));

        authService.logout(new RefreshRequest("logout-token"));

        assertThat(savedToken.isUsable()).isFalse();
    }

    @Test
    void 존재하지_않는_토큰으로_로그아웃하면_InvalidRefreshTokenException이_발생한다() {
        given(refreshTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(new RefreshRequest("unknown-token")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
