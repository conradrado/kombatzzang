package com.sprint.mission.matzzang.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.stat.exception.UserStatNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void AccessDeniedException은_403으로_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAccessDeniedException(new AccessDeniedException("접근 권한이 없습니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    void UserStatNotFoundException은_404로_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(new UserStatNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("전적을 찾을 수 없습니다. userId=1");
    }
}
