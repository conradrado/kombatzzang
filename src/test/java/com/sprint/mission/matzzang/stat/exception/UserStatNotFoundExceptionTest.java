package com.sprint.mission.matzzang.stat.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class UserStatNotFoundExceptionTest {

    @Test
    void 메시지에_userId가_포함된다() {
        UserStatNotFoundException exception = new UserStatNotFoundException(1L);

        assertThat(exception.getMessage()).isEqualTo("전적을 찾을 수 없습니다. userId=1");
    }

    @Test
    void ResponseStatus는_NOT_FOUND이다() {
        ResponseStatus responseStatus = UserStatNotFoundException.class.getAnnotation(ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void RuntimeException을_상속한다() {
        assertThat(new UserStatNotFoundException(1L)).isInstanceOf(RuntimeException.class);
    }
}
