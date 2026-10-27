package com.sprint.mission.matzzang.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.entity.Follow;
import com.sprint.mission.matzzang.follow.exception.AlreadyFollowingException;
import com.sprint.mission.matzzang.follow.exception.FollowNotFoundException;
import com.sprint.mission.matzzang.follow.exception.SelfFollowException;
import com.sprint.mission.matzzang.follow.repository.FollowRepository;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    private Follow follow(Long followerId, Long followingId) {
        return Follow.builder().followerId(followerId).followingId(followingId).build();
    }

    @Test
    void 팔로우하면_관계가_저장되고_응답이_반환된다() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);
        given(followRepository.saveAndFlush(any(Follow.class))).willAnswer(invocation -> invocation.getArgument(0));

        FollowResponse response = followService.follow(1L, 2L);

        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFollowerId()).isEqualTo(1L);
        assertThat(captor.getValue().getFollowingId()).isEqualTo(2L);
        assertThat(response.followerId()).isEqualTo(1L);
        assertThat(response.followingId()).isEqualTo(2L);
    }

    @Test
    void 자기_자신을_팔로우하면_SelfFollowException이_발생한다() {
        assertThatThrownBy(() -> followService.follow(1L, 1L))
                .isInstanceOf(SelfFollowException.class);

        verify(followRepository, never()).saveAndFlush(any());
    }

    @Test
    void 대상_유저가_없으면_UserNotFoundException이_발생한다() {
        given(userRepository.existsById(2L)).willReturn(false);

        assertThatThrownBy(() -> followService.follow(1L, 2L))
                .isInstanceOf(UserNotFoundException.class);

        verify(followRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_팔로우_중이면_AlreadyFollowingException이_발생한다() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> followService.follow(1L, 2L))
                .isInstanceOf(AlreadyFollowingException.class);

        verify(followRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시에_중복_팔로우되어_유니크_제약에_걸리면_AlreadyFollowingException이_발생한다() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);
        given(followRepository.saveAndFlush(any(Follow.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> followService.follow(1L, 2L))
                .isInstanceOf(AlreadyFollowingException.class);
    }

    @Test
    void 언팔로우하면_관계가_삭제된다() {
        Follow existing = follow(1L, 2L);
        given(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).willReturn(Optional.of(existing));

        followService.unfollow(1L, 2L);

        verify(followRepository).delete(existing);
    }

    @Test
    void 팔로우_관계가_없으면_언팔로우_시_FollowNotFoundException이_발생한다() {
        given(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> followService.unfollow(1L, 2L))
                .isInstanceOf(FollowNotFoundException.class);

        verify(followRepository, never()).delete(any());
    }

    @Test
    void 팔로잉_목록을_조회하면_응답_리스트로_매핑된다() {
        given(followRepository.findAllByFollowerId(1L)).willReturn(List.of(follow(1L, 2L), follow(1L, 3L)));

        List<FollowResponse> result = followService.getFollowings(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FollowResponse::followerId).containsOnly(1L);
        assertThat(result).extracting(FollowResponse::followingId).containsExactly(2L, 3L);
    }

    @Test
    void 팔로워_목록을_조회하면_응답_리스트로_매핑된다() {
        given(followRepository.findAllByFollowingId(2L)).willReturn(List.of(follow(1L, 2L), follow(3L, 2L)));

        List<FollowResponse> result = followService.getFollowers(2L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FollowResponse::followingId).containsOnly(2L);
        assertThat(result).extracting(FollowResponse::followerId).containsExactly(1L, 3L);
    }
}
