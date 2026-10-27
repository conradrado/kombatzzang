package com.sprint.mission.matzzang.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class PostUpdateCommandTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void title과_body가_모두_null이면_위반이_없다() {
        assertThat(validator.validate(new PostUpdateCommand(null, null))).isEmpty();
    }

    @Test
    void title이_빈_문자열이면_위반이다() {
        assertThat(validator.validate(new PostUpdateCommand("", null))).isNotEmpty();
    }

    @Test
    void title이_공백이면_위반이다() {
        assertThat(validator.validate(new PostUpdateCommand("   ", null))).isNotEmpty();
    }

    @Test
    void title_255자는_위반이_없다() {
        assertThat(validator.validate(new PostUpdateCommand("a".repeat(255), null))).isEmpty();
    }

    @Test
    void title_256자는_위반이다() {
        assertThat(validator.validate(new PostUpdateCommand("a".repeat(256), null))).isNotEmpty();
    }

    @Test
    void body_10000자는_위반이_없다() {
        assertThat(validator.validate(new PostUpdateCommand(null, "a".repeat(10000)))).isEmpty();
    }

    @Test
    void body_10001자는_위반이다() {
        assertThat(validator.validate(new PostUpdateCommand(null, "a".repeat(10001)))).isNotEmpty();
    }
}
