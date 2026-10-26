package com.sprint.mission.matzzang.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    private final OpenAPI openAPI = new OpenApiConfig().openAPI();

    @Test
    @DisplayName("API 메타정보에 title과 version이 설정된다")
    void openAPI_info() {
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Kombatzzang API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
    }

    @Test
    @DisplayName("bearerAuth는 HTTP bearer JWT 스킴으로 등록된다")
    void openAPI_bearerAuthScheme() {
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");

        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
    }

    @Test
    @DisplayName("전역 SecurityRequirement에 bearerAuth가 포함된다")
    void openAPI_securityRequirement() {
        assertThat(openAPI.getSecurity()).anySatisfy(requirement ->
                assertThat(requirement).containsKey("bearerAuth"));
    }
}
