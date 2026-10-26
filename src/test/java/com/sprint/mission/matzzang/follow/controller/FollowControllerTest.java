package com.sprint.mission.matzzang.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.common.exception.GlobalExceptionHandler;
import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.service.FollowService;
import com.sprint.mission.matzzang.user.constants.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FollowControllerTest {

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowController followController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone 설정에서는 @AuthenticationPrincipal 해석기가 없으므로 직접 등록한다
        mockMvc = MockMvcBuilders.standaloneSetup(followController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        JwtPrincipal principal = new JwtPrincipal(1L, UserRole.MEMBER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 팔로우하면_201과_응답이_반환된다() throws Exception {
        given(followService.createFollow(eq(1L), any()))
                .willReturn(new FollowResponse(10L, 1L, 2L, null));

        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followeeId").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"followeeId\":null}", "{\"followeeId\":0}", "{\"followeeId\":-1}"})
    void 잘못된_followeeId이면_400이_반환되고_서비스는_호출되지_않는다(String body) throws Exception {
        mockMvc.perform(post("/api/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(followService, never()).createFollow(any(), any());
    }
}
