package com.sprint.mission.matzzang.follow.service;

import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.entity.Follow;
import com.sprint.mission.matzzang.follow.exception.AlreadyFollowingException;
import com.sprint.mission.matzzang.follow.exception.FollowNotFoundException;
import com.sprint.mission.matzzang.follow.exception.SelfFollowException;
import com.sprint.mission.matzzang.follow.repository.FollowRepository;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public FollowResponse follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new SelfFollowException();
        }
        if (!userRepository.existsById(followingId)) {
            throw new UserNotFoundException(followingId);
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new AlreadyFollowingException();
        }
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();
        try {
            return FollowResponse.from(followRepository.saveAndFlush(follow));
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 exists 검사를 통과한 경우 유니크 제약 위반으로 들어온다
            throw new AlreadyFollowingException();
        }
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(FollowNotFoundException::new);
        followRepository.delete(follow);
    }

    public List<FollowResponse> getFollowings(Long userId) {
        return followRepository.findAllByFollowerId(userId).stream()
                .map(FollowResponse::from)
                .toList();
    }

    public List<FollowResponse> getFollowers(Long userId) {
        return followRepository.findAllByFollowingId(userId).stream()
                .map(FollowResponse::from)
                .toList();
    }
}
