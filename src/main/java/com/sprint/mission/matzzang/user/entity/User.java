package com.sprint.mission.matzzang.user.entity;

import com.sprint.mission.matzzang.common.entity.UpdatableEntity;
import com.sprint.mission.matzzang.user.constants.UserRole;
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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends UpdatableEntity {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "profile_image")
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Builder
    private User(String username, String email, String password, String profileImage, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileImage = profileImage;
        this.role = role;
    }

    public void updateProfile(String username, String profileImage) {
        if (username != null) {
            this.username = username;
        }
        if (profileImage != null) {
            this.profileImage = profileImage;
        }
    }
}