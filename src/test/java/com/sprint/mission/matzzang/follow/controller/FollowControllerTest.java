package com.sprint.mission.matzzang.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.common.exception.GlobalExceptionHandler;
import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.exception.AlreadyFollowingException;
import com.sprint.mission.matzzang.follow.exception.FollowNotFoundException;
import com.sprint.mission.matzzang.follow.exception.SelfFollowException;
import com.sprint.mission.matzzang.follow.service.FollowService;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 팔로우하면_201과_응답이_반환된다() throws Exception {
        given(followService.follow(1L, 2L)).willReturn(new FollowResponse(10L, 1L, 2L, null));

        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.followerId").value(1))
                .andExpect(jsonPath("$.followingId").value(2));

        verify(followService).follow(1L, 2L);
    }

    @Test
    void 자기_자신을_팔로우하면_400이_반환된다() throws Exception {
        given(followService.follow(1L, 1L)).willThrow(new SelfFollowException());

        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 이미_팔로우_중이면_409가_반환된다() throws Exception {
        given(followService.follow(1L, 2L)).willThrow(new AlreadyFollowingException());

        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void followeeId가_없으면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(followService, never()).follow(any(), any());
    }

    @Test
    void followeeId가_0이면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":0}"))
                .andExpect(status().isBadRequest());

        verify(followService, never()).follow(any(), any());
    }

    @Test
    void followeeId가_음수이면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/users/follow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":-1}"))
                .andExpect(status().isBadRequest());

        verify(followService, never()).follow(any(), any());
    }

    @Test
    void 언팔로우하면_204가_반환된다() throws Exception {
        mockMvc.perform(delete("/api/users/2/follow"))
                .andExpect(status().isNoContent());

        verify(followService).unfollow(1L, 2L);
    }

    @Test
    void 팔로우_관계가_없으면_언팔로우_시_404가_반환된다() throws Exception {
        willThrow(new FollowNotFoundException()).given(followService).unfollow(1L, 2L);

        mockMvc.perform(delete("/api/users/2/follow"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 팔로워_목록을_조회하면_200이_반환된다() throws Exception {
        given(followService.getFollowers(2L)).willReturn(List.of(new FollowResponse(10L, 1L, 2L, null)));

        mockMvc.perform(get("/api/users/2/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerId").value(1))
                .andExpect(jsonPath("$[0].followingId").value(2));
    }

    @Test
    void 팔로잉_목록을_조회하면_200이_반환된다() throws Exception {
        given(followService.getFollowings(1L)).willReturn(List.of(new FollowResponse(10L, 1L, 2L, null)));

        mockMvc.perform(get("/api/users/1/followings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerId").value(1))
                .andExpect(jsonPath("$[0].followingId").value(2));
    }
}
