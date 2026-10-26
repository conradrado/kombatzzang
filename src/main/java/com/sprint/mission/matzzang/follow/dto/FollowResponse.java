package com.sprint.mission.matzzang.follow.dto;

import com.sprint.mission.matzzang.follow.entity.Follow;
import java.time.Instant;

public record FollowResponse(
        Long id,
        Long followerId,
        Long followeeId,
        Instant createdAt
) {

    public static FollowResponse from(Follow follow) {
        return new FollowResponse(
                follow.getId(),
                follow.getFollowerId(),
                follow.getFolloweeId(),
                follow.getCreatedAt()
        );
    }
}
