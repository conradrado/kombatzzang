package com.sprint.mission.matzzang.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.dto.PostCreateCommand;
import com.sprint.mission.matzzang.post.dto.PostResponse;
import com.sprint.mission.matzzang.post.dto.PostUpdateCommand;
import com.sprint.mission.matzzang.post.entity.Post;
import com.sprint.mission.matzzang.post.exception.PostNotFoundException;
import com.sprint.mission.matzzang.post.repository.PostRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void 게시글을_생성하면_모집중_상태로_저장된다() {
        PostCreateCommand command = new PostCreateCommand("스파링 상대 구합니다", "내용", 1L);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.createPost(command);

        assertThat(response.title()).isEqualTo("스파링 상대 구합니다");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PostStatus.RECRUITING);
    }

    @Test
    void 존재하지_않는_게시글을_조회하면_예외가_발생한다() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(1L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void 게시글을_수정하면_변경된_내용이_반영된다() {
        Post post = Post.builder()
                .title("원본 제목")
                .body("원본 내용")
                .userId(1L)
                .status(PostStatus.RECRUITING)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        PostResponse response = postService.updatePost(1L, new PostUpdateCommand("수정된 제목", null));

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.body()).isEqualTo("원본 내용");
    }

    @Test
    void 게시글을_삭제하면_레포지토리에서_삭제된다() {
        Post post = Post.builder()
                .title("제목")
                .userId(1L)
                .status(PostStatus.RECRUITING)
                .build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        postService.deletePost(1L);

        verify(postRepository).delete(post);
    }
}
