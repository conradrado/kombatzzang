package com.sprint.mission.matzzang.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.WeightClass;
import com.sprint.mission.matzzang.user.constants.UserLevel;
import com.sprint.mission.matzzang.user.constants.UserRole;
import org.junit.jupiter.api.Test;

class UserTest {

    private User user() {
        return User.builder()
                .username("fighter")
                .email("fighter@test.com")
                .password("encoded")
                .role(UserRole.MEMBER)
                .build();
    }

    @Test
    void level_없이_생성하면_BEGINNER이고_전적은_0이다() {
        User user = user();

        assertThat(user.getLevel()).isEqualTo(UserLevel.BEGINNER);
        assertThat(user.getWins()).isZero();
        assertThat(user.getLosses()).isZero();
    }

    @Test
    void updateProfile에_새_값을_주면_반영된다() {
        User user = user();

        user.updateProfile("new", "key", Sport.BOXING, WeightClass.LIGHT, Region.SEOUL, "안녕");

        assertThat(user.getUsername()).isEqualTo("new");
        assertThat(user.getProfileImageKey()).isEqualTo("key");
        assertThat(user.getSport()).isEqualTo(Sport.BOXING);
        assertThat(user.getWeightClass()).isEqualTo(WeightClass.LIGHT);
        assertThat(user.getActivityRegion()).isEqualTo(Region.SEOUL);
        assertThat(user.getBio()).isEqualTo("안녕");
    }

    @Test
    void updateProfile에_null을_주면_기존_값이_유지된다() {
        User user = user();
        user.updateProfile("new", "key", Sport.MMA, WeightClass.WELTER, Region.BUSAN, "bio");

        user.updateProfile(null, null, null, null, null, null);

        assertThat(user.getUsername()).isEqualTo("new");
        assertThat(user.getSport()).isEqualTo(Sport.MMA);
        assertThat(user.getWeightClass()).isEqualTo(WeightClass.WELTER);
        assertThat(user.getActivityRegion()).isEqualTo(Region.BUSAN);
        assertThat(user.getBio()).isEqualTo("bio");
    }

    @Test
    void updateRecord에_새_값을_주면_반영된다() {
        User user = user();

        user.updateRecord(3, 1, UserLevel.AMATEUR);

        assertThat(user.getWins()).isEqualTo(3);
        assertThat(user.getLosses()).isEqualTo(1);
        assertThat(user.getLevel()).isEqualTo(UserLevel.AMATEUR);
    }

    @Test
    void updateRecord에_null을_주면_기존_값이_유지된다() {
        User user = user();
        user.updateRecord(2, 2, UserLevel.PRO);

        user.updateRecord(null, null, null);

        assertThat(user.getWins()).isEqualTo(2);
        assertThat(user.getLosses()).isEqualTo(2);
        assertThat(user.getLevel()).isEqualTo(UserLevel.PRO);
    }
}
