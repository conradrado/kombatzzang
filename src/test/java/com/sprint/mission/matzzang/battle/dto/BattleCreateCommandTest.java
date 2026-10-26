package com.sprint.mission.matzzang.battle.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.user.constants.Sport;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class BattleCreateCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private BattleCreateCommand command(Long opponentId, Sport sport, Instant scheduledAt, String location,
                                        String message) {
        return new BattleCreateCommand(opponentId, sport, scheduledAt, location, message);
    }

    @Test
    void 올바른_요청이면_위반이_없다() {
        BattleCreateCommand command = command(2L, Sport.BOXING, Instant.now().plus(1, ChronoUnit.DAYS), "서울", "한판");

        assertThat(validator.validate(command)).isEmpty();
    }

    @Test
    void opponentId가_null이면_위반이_발생한다() {
        assertThat(validator.validate(command(null, Sport.BOXING, null, null, null))).isNotEmpty();
    }

    @Test
    void opponentId가_0이면_위반이_발생한다() {
        assertThat(validator.validate(command(0L, Sport.BOXING, null, null, null))).isNotEmpty();
    }

    @Test
    void opponentId가_음수이면_위반이_발생한다() {
        assertThat(validator.validate(command(-1L, Sport.BOXING, null, null, null))).isNotEmpty();
    }

    @Test
    void sport가_null이면_위반이_발생한다() {
        assertThat(validator.validate(command(2L, null, null, null, null))).isNotEmpty();
    }

    @Test
    void scheduledAt이_null이면_위반이_없다() {
        assertThat(validator.validate(command(2L, Sport.BOXING, null, null, null))).isEmpty();
    }

    @Test
    void scheduledAt이_과거이면_위반이_발생한다() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

        assertThat(validator.validate(command(2L, Sport.BOXING, past, null, null))).isNotEmpty();
    }

    @Test
    void location이_256자이면_위반이_발생한다() {
        assertThat(validator.validate(command(2L, Sport.BOXING, null, "a".repeat(256), null))).isNotEmpty();
    }

    @Test
    void message가_1001자이면_위반이_발생한다() {
        assertThat(validator.validate(command(2L, Sport.BOXING, null, null, "a".repeat(1001)))).isNotEmpty();
    }

    @Test
    void message가_1000자이면_위반이_없다() {
        assertThat(validator.validate(command(2L, Sport.BOXING, null, null, "a".repeat(1000)))).isEmpty();
    }
}
