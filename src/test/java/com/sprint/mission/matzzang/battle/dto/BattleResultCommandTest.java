package com.sprint.mission.matzzang.battle.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class BattleResultCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void winnerId가_null이면_위반이_발생한다() {
        assertThat(validator.validate(new BattleResultCommand(null))).isNotEmpty();
    }

    @Test
    void winnerId가_0이면_위반이_발생한다() {
        assertThat(validator.validate(new BattleResultCommand(0L))).isNotEmpty();
    }

    @Test
    void winnerId가_음수이면_위반이_발생한다() {
        assertThat(validator.validate(new BattleResultCommand(-1L))).isNotEmpty();
    }

    @Test
    void winnerId가_양수이면_위반이_없다() {
        assertThat(validator.validate(new BattleResultCommand(2L))).isEmpty();
    }
}
