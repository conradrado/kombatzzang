package com.sprint.mission.matzzang.follow.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class FollowCreateRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void followeeId가_null이면_위반이_발생한다() {
        assertThat(validator.validate(new FollowCreateRequest(null))).isNotEmpty();
    }

    @Test
    void followeeId가_0이면_위반이_발생한다() {
        assertThat(validator.validate(new FollowCreateRequest(0L))).isNotEmpty();
    }

    @Test
    void followeeId가_음수이면_위반이_발생한다() {
        assertThat(validator.validate(new FollowCreateRequest(-1L))).isNotEmpty();
    }

    @Test
    void followeeId가_양수이면_위반이_없다() {
        assertThat(validator.validate(new FollowCreateRequest(2L))).isEmpty();
    }
}
