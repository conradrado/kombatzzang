package com.sprint.mission.matzzang.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.matzzang.common.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JwtAccessDeniedHandlerTest {

    private final JwtAccessDeniedHandler accessDeniedHandler = new JwtAccessDeniedHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 권한이_없으면_403과_에러_응답을_반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("접근 거부"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.status()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(body.message()).isEqualTo("접근 권한이 없습니다.");
    }
}
