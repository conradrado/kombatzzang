package com.sprint.mission.matzzang.post.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.post.constants.PostStatus;
import org.junit.jupiter.api.Test;

class PostTest {

    @Test
    void 게시글을_생성하면_빌더에_전달한_값으로_초기화된다() {
        Post post = Post.builder()
                .title("스파링 상대 구합니다")
                .body("주 2회 가능한 분 구해요")
                .userId(1L)
                .status(PostStatus.RECRUITING)
                .build();

        assertThat(post.getTitle()).isEqualTo("스파링 상대 구합니다");
        assertThat(post.getBody()).isEqualTo("주 2회 가능한 분 구해요");
        assertThat(post.getUserId()).isEqualTo(1L);
        assertThat(post.getStatus()).isEqualTo(PostStatus.RECRUITING);
    }

    @Test
    void updateContent은_null이_아닌_필드만_반영한다() {
        Post post = Post.builder()
                .title("원본 제목")
                .body("원본 내용")
                .userId(1L)
                .status(PostStatus.RECRUITING)
                .build();

        post.updateContent("수정된 제목", null);

        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getBody()).isEqualTo("원본 내용");
    }
}
