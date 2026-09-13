package com.sprint.mission.matzzang.post.entity;

import com.sprint.mission.matzzang.common.entity.BaseEntity;
import com.sprint.mission.matzzang.post.constants.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @Builder
    private Post(String title, String body, Long userId, PostStatus status) {
        this.title = title;
        this.body = body;
        this.userId = userId;
        this.status = status;
    }

    public void updateContent(String title, String body) {
        if (title != null) {
            this.title = title;
        }
        if (body != null) {
            this.body = body;
        }
    }
}
