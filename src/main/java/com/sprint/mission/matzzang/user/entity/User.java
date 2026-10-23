package com.sprint.mission.matzzang.user.entity;

import com.sprint.mission.matzzang.common.entity.UpdatableEntity;
import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserLevel;
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

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 20)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport", length = 30)
    private Sport sport;

    @Column(name = "weight_class", length = 50)
    private String weightClass;

    @Column(name = "activity_region", length = 100)
    private String activityRegion;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserLevel level = UserLevel.BEGINNER;

    @Column(length = 500)
    private String bio;

    @Builder
    private User(String username, String email, String password, String profileImageKey, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileImageKey = profileImageKey;
        this.role = role;
    }

    public void updateRegion(Region region) {
        this.region = region;
    }

    public void updateProfile(String username, String profileImageKey) {
        updateProfile(username, profileImageKey, null, null, null, null, null, null, null);
    }

    public void updateProfile(String username, String profileImageKey, Sport sport, String weightClass,
                              String activityRegion, Integer wins, Integer losses, UserLevel level, String bio) {
        if (username != null) {
            this.username = username;
        }
        if (profileImageKey != null) {
            this.profileImageKey = profileImageKey;
        }
        if (sport != null) {
            this.sport = sport;
        }
        if (weightClass != null) {
            this.weightClass = weightClass;
        }
        if (activityRegion != null) {
            this.activityRegion = activityRegion;
        }
        if (wins != null) {
            this.wins = wins;
        }
        if (losses != null) {
            this.losses = losses;
        }
        if (level != null) {
            this.level = level;
        }
        if (bio != null) {
            this.bio = bio;
        }
    }
}