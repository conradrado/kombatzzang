package com.sprint.mission.matzzang.auth.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        CustomUserDetails principal;
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            principal = (CustomUserDetails) authentication.getPrincipal();
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(principal.getUserId(), principal.getRole());
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken savedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .filter(RefreshToken::isUsable)
                .orElseThrow(InvalidRefreshTokenException::new);

        savedToken.revoke();

        User user = userRepository.findById(savedToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(savedToken.getUserId()));

        return issueTokens(user.getId(), user.getRole());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        RefreshToken savedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(InvalidRefreshTokenException::new);
        savedToken.revoke();
    }

    private TokenResponse issueTokens(Long userId, UserRole role) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build());

        return TokenResponse.of(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationMs());
    }
}
