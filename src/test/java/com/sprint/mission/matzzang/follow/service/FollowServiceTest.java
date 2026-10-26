package com.sprint.mission.matzzang.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.follow.dto.FollowCreateRequest;
import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.entity.Follow;
import com.sprint.mission.matzzang.follow.repository.FollowRepository;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowService followService;

    @Test
    void 팔로우하면_저장한다() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(false);
        given(followRepository.save(any(Follow.class))).willAnswer(invocation -> invocation.getArgument(0));

        FollowResponse response = followService.createFollow(1L, new FollowCreateRequest(2L));

        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(captor.capture());
        assertThat(captor.getValue().getFollowerId()).isEqualTo(1L);
        assertThat(captor.getValue().getFolloweeId()).isEqualTo(2L);
        assertThat(response.followerId()).isEqualTo(1L);
        assertThat(response.followeeId()).isEqualTo(2L);
    }

    @Test
    void 자기_자신을_팔로우하면_예외가_발생한다() {
        assertThatThrownBy(() -> followService.createFollow(1L, new FollowCreateRequest(1L)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(followRepository, never()).save(any());
    }

    @Test
    void 대상_유저가_없으면_예외가_발생한다() {
        given(userRepository.existsById(2L)).willReturn(false);

        assertThatThrownBy(() -> followService.createFollow(1L, new FollowCreateRequest(2L)))
                .isInstanceOf(UserNotFoundException.class);

        verify(followRepository, never()).save(any());
    }

    @Test
    void 이미_팔로우_중이면_예외가_발생한다() {
        given(userRepository.existsById(2L)).willReturn(true);
        given(followRepository.existsByFollowerIdAndFolloweeId(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> followService.createFollow(1L, new FollowCreateRequest(2L)))
                .isInstanceOf(IllegalStateException.class);

        verify(followRepository, never()).save(any());
    }
}
