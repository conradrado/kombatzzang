package com.sprint.mission.matzzang.battle.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.WeightClass;
import org.junit.jupiter.api.Test;

class BattleTest {

    private Battle battle(BattleStatus status) {
        return Battle.builder()
                .challengerId(1L)
                .opponentId(2L)
                .sport(Sport.BOXING)
                .status(status)
                .build();
    }

    @Test
    void 신청_상태에서_수락하면_ACCEPTED가_된다() {
        Battle battle = battle(BattleStatus.REQUESTED);

        battle.accept();

        assertThat(battle.getStatus()).isEqualTo(BattleStatus.ACCEPTED);
    }

    @Test
    void 신청_상태가_아니면_수락할_수_없다() {
        Battle battle = battle(BattleStatus.ACCEPTED);

        assertThatThrownBy(battle::accept).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 신청_상태에서_거절하면_REJECTED가_된다() {
        Battle battle = battle(BattleStatus.REQUESTED);

        battle.reject();

        assertThat(battle.getStatus()).isEqualTo(BattleStatus.REJECTED);
    }

    @Test
    void 신청_상태가_아니면_거절할_수_없다() {
        Battle battle = battle(BattleStatus.COMPLETED);

        assertThatThrownBy(battle::reject).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 신청_또는_수락_상태에서_취소하면_CANCELED가_된다() {
        Battle requested = battle(BattleStatus.REQUESTED);
        Battle accepted = battle(BattleStatus.ACCEPTED);

        requested.cancel();
        accepted.cancel();

        assertThat(requested.getStatus()).isEqualTo(BattleStatus.CANCELED);
        assertThat(accepted.getStatus()).isEqualTo(BattleStatus.CANCELED);
    }

    @Test
    void 종료된_대전은_취소할_수_없다() {
        assertThatThrownBy(() -> battle(BattleStatus.REJECTED).cancel()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> battle(BattleStatus.CANCELED).cancel()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> battle(BattleStatus.COMPLETED).cancel()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 수락_상태에서_결과를_기록하면_COMPLETED와_승자가_저장된다() {
        Battle battle = battle(BattleStatus.ACCEPTED);

        battle.complete(2L);

        assertThat(battle.getStatus()).isEqualTo(BattleStatus.COMPLETED);
        assertThat(battle.getWinnerId()).isEqualTo(2L);
    }

    @Test
    void 수락_상태가_아니면_결과를_기록할_수_없다() {
        Battle battle = battle(BattleStatus.REQUESTED);

        assertThatThrownBy(() -> battle.complete(1L)).isInstanceOf(IllegalStateException.class);
        assertThat(battle.getWinnerId()).isNull();
    }

    @Test
    void 참가자가_아닌_승자로_결과를_기록하면_예외가_발생한다() {
        Battle battle = battle(BattleStatus.ACCEPTED);

        assertThatThrownBy(() -> battle.complete(99L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(battle.getStatus()).isEqualTo(BattleStatus.ACCEPTED);
    }

    @Test
    void 결과를_기록하면_승자가_아닌_참가자가_패자가_된다() {
        Battle opponentWin = battle(BattleStatus.ACCEPTED);
        Battle challengerWin = battle(BattleStatus.ACCEPTED);

        opponentWin.complete(2L);
        challengerWin.complete(1L);

        assertThat(opponentWin.getLoserId()).isEqualTo(1L);
        assertThat(challengerWin.getLoserId()).isEqualTo(2L);
    }

    @Test
    void 완료_전에는_패자가_없다() {
        assertThat(battle(BattleStatus.ACCEPTED).getLoserId()).isNull();
    }

    @Test
    void 규칙_없이_생성하면_기본_규칙이_적용된다() {
        Battle battle = battle(BattleStatus.REQUESTED);

        assertThat(battle.getRule()).isEqualTo(BattleRule.defaultRule());
    }

    @Test
    void 규칙을_지정하면_그대로_저장된다() {
        BattleRule rule = BattleRule.of(5, 2, WeightClass.UNKNOWN);

        Battle battle = Battle.builder()
                .challengerId(1L)
                .opponentId(2L)
                .sport(Sport.BOXING)
                .status(BattleStatus.REQUESTED)
                .rule(rule)
                .build();

        assertThat(battle.getRule()).isEqualTo(rule);
    }

    @Test
    void 신청_상태에서_규칙을_변경할_수_있다() {
        Battle battle = battle(BattleStatus.REQUESTED);
        BattleRule rule = BattleRule.of(5, 2, WeightClass.UNKNOWN);

        battle.changeRule(rule);

        assertThat(battle.getRule()).isEqualTo(rule);
    }

    @Test
    void 신청_상태가_아니면_규칙을_변경할_수_없다() {
        Battle battle = battle(BattleStatus.ACCEPTED);

        assertThatThrownBy(() -> battle.changeRule(BattleRule.defaultRule()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 규칙이_null이면_변경할_수_없다() {
        Battle battle = battle(BattleStatus.REQUESTED);

        assertThatThrownBy(() -> battle.changeRule(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
