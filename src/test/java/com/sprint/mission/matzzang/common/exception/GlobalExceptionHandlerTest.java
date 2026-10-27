package com.sprint.mission.matzzang.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.follow.exception.AlreadyFollowingException;
import com.sprint.mission.matzzang.follow.exception.FollowNotFoundException;
import com.sprint.mission.matzzang.follow.exception.SelfFollowException;
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
    void SelfFollowException은_400으로_응답한다() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(new SelfFollowException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("자기 자신은 팔로우할 수 없습니다.");
    }

    @Test
    void AlreadyFollowingException은_409로_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(new AlreadyFollowingException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("이미 팔로우 중인 사용자입니다.");
    }

    @Test
    void FollowNotFoundException은_404로_응답한다() {
        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(new FollowNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("팔로우 관계를 찾을 수 없습니다.");
    }
}
