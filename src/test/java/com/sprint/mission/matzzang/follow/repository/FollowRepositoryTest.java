package com.sprint.mission.matzzang.follow.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.matzzang.follow.entity.Follow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    private Follow save(Long followerId, Long followingId) {
        return followRepository.saveAndFlush(
                Follow.builder().followerId(followerId).followingId(followingId).build());
    }

    @Test
    void 저장_후_exists는_방향에_따라_결과가_다르다() {
        save(1L, 2L);

        assertThat(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).isTrue();
        assertThat(followRepository.existsByFollowerIdAndFollowingId(2L, 1L)).isFalse();
    }

    @Test
    void findByFollowerIdAndFollowingId는_저장된_엔티티를_반환한다() {
        Follow saved = save(1L, 2L);

        assertThat(followRepository.findByFollowerIdAndFollowingId(1L, 2L))
                .isPresent()
                .get()
                .extracting(Follow::getId)
                .isEqualTo(saved.getId());
        assertThat(followRepository.findByFollowerIdAndFollowingId(1L, 3L)).isEmpty();
    }

    @Test
    void 목록_조회와_카운트가_정확한_결과를_반환한다() {
        save(1L, 2L);
        save(1L, 3L);
        save(4L, 2L);

        assertThat(followRepository.findAllByFollowerId(1L))
                .extracting(Follow::getFollowingId).containsExactlyInAnyOrder(2L, 3L);
        assertThat(followRepository.findAllByFollowingId(2L))
                .extracting(Follow::getFollowerId).containsExactlyInAnyOrder(1L, 4L);
        assertThat(followRepository.countByFollowerId(1L)).isEqualTo(2);
        assertThat(followRepository.countByFollowingId(2L)).isEqualTo(2);
        assertThat(followRepository.countByFollowingId(1L)).isZero();
    }

    @Test
    void 같은_관계를_중복_저장하면_DataIntegrityViolationException이_발생한다() {
        save(1L, 2L);

        assertThatThrownBy(() -> save(1L, 2L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 삭제하면_exists가_false가_된다() {
        Follow saved = save(1L, 2L);

        followRepository.delete(saved);
        followRepository.flush();

        assertThat(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).isFalse();
    }
}
