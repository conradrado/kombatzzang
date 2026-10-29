package com.sprint.mission.matzzang.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.dto.PostCreateCommand;
import com.sprint.mission.matzzang.post.dto.PostPageResponse;
import com.sprint.mission.matzzang.post.dto.PostResponse;
import com.sprint.mission.matzzang.post.dto.PostUpdateCommand;
import com.sprint.mission.matzzang.post.entity.Post;
import com.sprint.mission.matzzang.post.exception.PostNotFoundException;
import com.sprint.mission.matzzang.post.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

    @Test
    void 게시글_단건을_조회하면_응답이_반환된다() {
        given(postRepository.findById(1L)).willReturn(Optional.of(post("제목", "내용")));

        PostResponse response = postService.getPost(1L);

        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.body()).isEqualTo("내용");
        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    void 존재하지_않는_게시글을_조회하면_예외가_발생한다() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
                .isInstanceOf(PostNotFoundException.class);
    }

    @Test
    void 작성자_없이_목록을_조회하면_전체_최신순_페이지가_반환된다() {
        PageRequest pageable = PageRequest.of(0, 2);
        given(postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable))
                .willReturn(new PageImpl<>(List.of(post("a", "내용"), post("b", "내용")), pageable, 5));

        PostPageResponse response = postService.getPostList(null, 0, 2);

        assertThat(response.content()).hasSize(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void 작성자를_지정하면_해당_작성자의_목록을_조회한다() {
        PageRequest pageable = PageRequest.of(1, 5);
        given(postRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(3L, pageable))
                .willReturn(new PageImpl<>(List.of(post("a", "내용")), pageable, 6));

        PostPageResponse response = postService.getPostList(3L, 1, 5);

        assertThat(response.content()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        verify(postRepository).findAllByUserIdOrderByCreatedAtDescIdDesc(3L, pageable);
        verify(postRepository, never()).findAllByOrderByCreatedAtDescIdDesc(any());
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    void 잘못된_페이지_파라미터로_목록을_조회하면_예외가_발생한다(int page, int size) {
        assertThatThrownBy(() -> postService.getPostList(null, page, size))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(postRepository);
    }
}
