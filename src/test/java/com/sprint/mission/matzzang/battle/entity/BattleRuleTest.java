package com.sprint.mission.matzzang.battle.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.matzzang.user.constants.WeightClass;
import org.junit.jupiter.api.Test;

class BattleRuleTest {

    @Test
    void 정상_값으로_생성하면_필드가_저장된다() {
        BattleRule rule = BattleRule.of(5, 2, WeightClass.UNKNOWN);

        assertThat(rule.getRounds()).isEqualTo(5);
        assertThat(rule.getRoundMinutes()).isEqualTo(2);
        assertThat(rule.getWeightClass()).isEqualTo(WeightClass.UNKNOWN);
    }

    @Test
    void 경계값은_허용된다() {
        assertThat(BattleRule.of(1, 1, WeightClass.UNKNOWN).totalMinutes()).isEqualTo(1);
        assertThat(BattleRule.of(12, 10, WeightClass.UNKNOWN).totalMinutes()).isEqualTo(120);
    }

    @Test
    void 라운드_수가_범위를_벗어나면_예외가_발생한다() {
        assertThatThrownBy(() -> BattleRule.of(0, 3, WeightClass.UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BattleRule.of(13, 3, WeightClass.UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 라운드당_시간이_범위를_벗어나면_예외가_발생한다() {
        assertThatThrownBy(() -> BattleRule.of(3, 0, WeightClass.UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BattleRule.of(3, 11, WeightClass.UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 체급이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> BattleRule.of(3, 3, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 기본_규칙은_3라운드_3분_UNKNOWN이다() {
        BattleRule rule = BattleRule.defaultRule();

        assertThat(rule.getRounds()).isEqualTo(3);
        assertThat(rule.getRoundMinutes()).isEqualTo(3);
        assertThat(rule.getWeightClass()).isEqualTo(WeightClass.UNKNOWN);
    }

    @Test
    void 총_시간은_라운드_수와_라운드당_시간의_곱이다() {
        assertThat(BattleRule.of(4, 5, WeightClass.UNKNOWN).totalMinutes()).isEqualTo(20);
    }

    @Test
    void 같은_값의_규칙은_동등하다() {
        BattleRule a = BattleRule.of(3, 3, WeightClass.UNKNOWN);
        BattleRule b = BattleRule.of(3, 3, WeightClass.UNKNOWN);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(BattleRule.of(4, 3, WeightClass.UNKNOWN));
    }
}
