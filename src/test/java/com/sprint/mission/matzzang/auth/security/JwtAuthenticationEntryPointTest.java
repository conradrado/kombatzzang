package com.sprint.mission.matzzang.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.matzzang.common.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 인증_실패시_401과_에러_응답을_반환한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("인증 실패"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = objectMapper.readValue(response.getContentAsString(), ErrorResponse.class);
        assertThat(body.status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(body.message()).isEqualTo("인증이 필요합니다.");
    }
}
