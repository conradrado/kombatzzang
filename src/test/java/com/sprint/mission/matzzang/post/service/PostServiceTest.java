package com.sprint.mission.matzzang.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private Post post(String title, String body) {
        return Post.builder().title(title).body(body).userId(1L).status(PostStatus.RECRUITING).build();
    }

    @Test
    void 게시글을_생성하면_RECRUITING_상태와_작성자로_저장한다() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.createPost(7L, new PostCreateCommand("제목", "내용"));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getTitle()).isEqualTo("제목");
        assertThat(saved.getBody()).isEqualTo("내용");
        assertThat(saved.getStatus()).isEqualTo(PostStatus.RECRUITING);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(PostStatus.RECRUITING);
    }

    @Test
    void 게시글을_수정하면_title과_body가_반영된다() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post("제목", "내용")));

        PostResponse response = postService.updatePost(1L, new PostUpdateCommand("새 제목", "새 내용"));

        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.body()).isEqualTo("새 내용");
    }

    @Test
    void 게시글_수정시_null_필드는_유지된다() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post("제목", "내용")));

        PostResponse response = postService.updatePost(1L, new PostUpdateCommand(null, "새 내용"));

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.body()).isEqualTo("새 내용");
    }

    @Test
    void 존재하지_않는_게시글을_수정하면_예외가_발생한다() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(99L, new PostUpdateCommand("제목", null)))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void 게시글을_삭제하면_repository의_delete를_호출한다() {
        Post post = post("제목", "내용");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));

        postService.deletePost(1L);

        verify(postRepository).delete(post);
    }

    @Test
    void 존재하지_않는_게시글을_삭제하면_예외가_발생한다() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(99L))
                .isInstanceOf(PostNotFoundException.class);
    }
}
