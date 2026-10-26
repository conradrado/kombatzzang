package com.sprint.mission.matzzang.follow.repository;

import com.sprint.mission.matzzang.follow.entity.Follow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findAllByFollowerId(Long followerId);

    List<Follow> findAllByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);
}
