package com.sprint.mission.matzzang.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.entity.Post;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    private Post save(Long userId) {
        return postRepository.saveAndFlush(Post.builder()
                .title("제목")
                .body("본문")
                .userId(userId)
                .status(PostStatus.RECRUITING)
                .build());
    }

    @Test
    void 전체_목록은_최신순으로_조회된다() {
        Post first = save(1L);
        Post second = save(1L);
        Post third = save(2L);

        Page<Post> page = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Post::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void 페이징_정보가_첫_페이지와_마지막_페이지에서_정확하다() {
        for (int i = 0; i < 5; i++) {
            save(1L);
        }

        Page<Post> first = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2));
        Page<Post> last = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(2, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(last.getContent()).hasSize(1);
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void 작성자_필터는_해당_작성자의_게시글만_최신순으로_반환한다() {
        Post a = save(1L);
        save(2L);
        Post b = save(1L);
        save(2L);

        Page<Post> page = postRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(
                1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Post::getUserId).containsOnly(1L);
        assertThat(page.getContent()).extracting(Post::getId)
                .containsExactly(b.getId(), a.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 게시글이_없는_작성자는_빈_페이지를_반환한다() {
        save(1L);

        Page<Post> page = postRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(
                99L, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}
