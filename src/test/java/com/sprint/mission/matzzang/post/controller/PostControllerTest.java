package com.sprint.mission.matzzang.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.common.exception.GlobalExceptionHandler;
import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.dto.PostPageResponse;
import com.sprint.mission.matzzang.post.dto.PostResponse;
import com.sprint.mission.matzzang.post.exception.PostNotFoundException;
import com.sprint.mission.matzzang.post.service.PostService;
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
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone 설정에서는 @AuthenticationPrincipal 해석기가 없으므로 직접 등록한다
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
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

    private PostResponse response(String title, String body) {
        return new PostResponse(10L, title, body, 1L, PostStatus.RECRUITING, null);
    }

    @Test
    void 게시글을_생성하면_201과_응답이_반환된다() throws Exception {
        given(postService.createPost(eq(1L), any())).willReturn(response("제목", "내용"));

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"내용\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("제목"));

        verify(postService).createPost(eq(1L), any());
    }

    @Test
    void title이_누락된_생성_요청은_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"내용\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    void title이_공백인_생성_요청은_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"body\":\"내용\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    void title이_256자인_생성_요청은_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    void 게시글을_수정하면_200과_응답이_반환된다() throws Exception {
        given(postService.updatePost(eq(10L), any())).willReturn(response("수정", "내용"));

        mockMvc.perform(patch("/api/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정"));

        verify(postService).updatePost(eq(10L), any());
    }

    @Test
    void 게시글_단건을_조회하면_200과_응답이_반환된다() throws Exception {
        given(postService.getPost(10L)).willReturn(response("제목", "내용"));

        mockMvc.perform(get("/api/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("제목"));
    }

    @Test
    void 존재하지_않는_게시글을_조회하면_404가_반환된다() throws Exception {
        given(postService.getPost(99L)).willThrow(new PostNotFoundException(99L));

        mockMvc.perform(get("/api/posts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 파라미터_없이_목록을_조회하면_기본값으로_서비스를_호출한다() throws Exception {
        given(postService.getPostList(null, 0, 20)).willReturn(
                new PostPageResponse(List.of(response("제목", "내용")), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(postService).getPostList(null, 0, 20);
    }

    @Test
    void 쿼리_파라미터가_서비스로_전달된다() throws Exception {
        given(postService.getPostList(1L, 2, 5)).willReturn(
                new PostPageResponse(List.of(), 2, 5, 0, 0, false));

        mockMvc.perform(get("/api/posts").param("userId", "1").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(postService).getPostList(1L, 2, 5);
    }

    @Test
    void 잘못된_페이지_파라미터로_목록을_조회하면_400이_반환된다() throws Exception {
        given(postService.getPostList(null, 0, 1000)).willThrow(new IllegalArgumentException("잘못된 크기"));

        mockMvc.perform(get("/api/posts").param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void title이_공백인_수정_요청은_400이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }

    @Test
    void body가_10001자인_수정_요청은_400이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "a".repeat(10001) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(postService);
    }
}
