package com.sprint.mission.matzzang.post.dto;

import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.entity.Post;
import java.time.Instant;

public record PostResponse(
        Long id,
        String title,
        String body,
        Long userId,
        PostStatus status,
        Instant createdAt
) {

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getBody(),
                post.getUserId(),
                post.getStatus(),
                post.getCreatedAt()
        );
    }
}
