package com.sprint.mission.matzzang.follow.service;

import com.sprint.mission.matzzang.follow.dto.FollowCreateRequest;
import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.entity.Follow;
import com.sprint.mission.matzzang.follow.repository.FollowRepository;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public FollowResponse createFollow(Long followerId, FollowCreateRequest request) {
        Long followeeId = request.followeeId();
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다. userId=" + followerId);
        }
        if (!userRepository.existsById(followeeId)) {
            throw new UserNotFoundException(followeeId);
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new IllegalStateException("이미 팔로우 중인 사용자입니다. followeeId=" + followeeId);
        }
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .build();
        return FollowResponse.from(followRepository.save(follow));
    }
}
