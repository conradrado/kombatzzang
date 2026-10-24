package com.sprint.mission.matzzang.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.config.SecurityConfig;
import com.sprint.mission.matzzang.auth.dto.TokenResponse;
import com.sprint.mission.matzzang.auth.exception.InvalidCredentialsException;
import com.sprint.mission.matzzang.auth.exception.InvalidRefreshTokenException;
import com.sprint.mission.matzzang.auth.jwt.JwtAuthenticationFilter;
import com.sprint.mission.matzzang.auth.jwt.JwtTokenProvider;
import com.sprint.mission.matzzang.auth.security.JwtAccessDeniedHandler;
import com.sprint.mission.matzzang.auth.security.JwtAuthenticationEntryPoint;
import com.sprint.mission.matzzang.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-auth-controller-test-0123456789",
        "jwt.access-token-expiration-ms=1800000",
        "jwt.refresh-token-expiration-ms=1209600000"
})
class AuthControllerTest {

    private static final String LOGIN_BODY = "{\"email\":\"f@test.com\",\"password\":\"password1\"}";
    private static final String REFRESH_BODY = "{\"refreshToken\":\"refresh-token\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void 올바른_이메일과_비밀번호로_로그인하면_200과_토큰이_반환된다() throws Exception {
        given(authService.login(any())).willReturn(TokenResponse.of("access", "refresh", 1800000L));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    void 인증정보가_틀리면_401과_메시지가_반환된다() throws Exception {
        given(authService.login(any())).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void 이메일_형식이_아니면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\",\"password\":\"password1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 비밀번호가_비어있으면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"f@test.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 토큰을_재발급하면_200과_새_토큰이_반환된다() throws Exception {
        given(authService.refresh(any())).willReturn(TokenResponse.of("new-access", "new-refresh", 1800000L));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void 유효하지_않은_리프레시_토큰으로_재발급하면_401이_반환된다() throws Exception {
        given(authService.refresh(any())).willThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않거나 만료된 Refresh Token 입니다."));
    }

    @Test
    void 로그아웃하면_204가_반환된다() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REFRESH_BODY))
                .andExpect(status().isNoContent());
    }

    @Test
    void 로그아웃시_리프레시_토큰이_비어있으면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
