package com.sprint.mission.matzzang.auth.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.jwt.JwtAuthenticationFilter;
import com.sprint.mission.matzzang.auth.jwt.JwtTokenProvider;
import com.sprint.mission.matzzang.auth.security.JwtAccessDeniedHandler;
import com.sprint.mission.matzzang.auth.security.JwtAuthenticationEntryPoint;
import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserLevel;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.constants.WeightClass;
import com.sprint.mission.matzzang.user.controller.UserController;
import com.sprint.mission.matzzang.user.dto.UserResponse;
import com.sprint.mission.matzzang.user.security.UserAccessGuard;
import com.sprint.mission.matzzang.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-security-config-test-0123456789",
        "jwt.access-token-expiration-ms=1800000",
        "jwt.refresh-token-expiration-ms=1209600000"
})
class SecurityConfigTest {

    // @Import 로 등록하면 빈 이름이 FQCN 이 되어 SpEL 의 @userAccessGuard 를 찾지 못하므로 이름을 지정해 등록한다.
    @TestConfiguration
    static class GuardConfig {

        @Bean
        UserAccessGuard userAccessGuard() {
            return new UserAccessGuard();
        }
    }

    private static final String UPDATE_BODY = "{\"bio\":\"안녕\"}";
    private static final String CREATE_BODY =
            "{\"username\":\"fighter\",\"email\":\"f@test.com\",\"password\":\"password1\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private String bearer(Long userId, UserRole role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(userId, role);
    }

    private UserResponse response(Long id) {
        return new UserResponse(id, "fighter", "f@test.com", null, UserRole.MEMBER, null,
                Sport.BOXING, WeightClass.LIGHT, Region.SEOUL, 3, 1, UserLevel.AMATEUR, "안녕", null, null);
    }

    @Test
    void 토큰_없이_보호된_경로에_접근하면_401과_JSON이_반환된다() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    void 잘못된_토큰으로_접근하면_401이_반환된다() throws Exception {
        mockMvc.perform(get("/api/users/1").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 리프레시_토큰을_액세스_토큰으로_사용하면_401이_반환된다() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        mockMvc.perform(get("/api/users/1").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 회원가입은_토큰_없이도_401이_아니다() throws Exception {
        given(userService.createUser(any())).willReturn(response(1L));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void MEMBER가_조회하면_200이_반환된다() throws Exception {
        given(userService.getUser(1L)).willReturn(response(1L));

        mockMvc.perform(get("/api/users/1").header("Authorization", bearer(1L, UserRole.MEMBER)))
                .andExpect(status().isOk());
    }

    @Test
    void MEMBER가_타인을_수정하면_403이_반환되고_서비스는_호출되지_않는다() throws Exception {
        mockMvc.perform(patch("/api/users/2")
                        .header("Authorization", bearer(1L, UserRole.MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    void MEMBER가_본인을_수정하면_200이_반환된다() throws Exception {
        given(userService.updateUser(eq(1L), any())).willReturn(response(1L));

        mockMvc.perform(patch("/api/users/1")
                        .header("Authorization", bearer(1L, UserRole.MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void ADMIN이_타인을_수정하면_200이_반환된다() throws Exception {
        given(userService.updateUser(eq(2L), any())).willReturn(response(2L));

        mockMvc.perform(patch("/api/users/2")
                        .header("Authorization", bearer(99L, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void MEMBER가_타인을_삭제하면_403이_반환된다() throws Exception {
        mockMvc.perform(delete("/api/users/2").header("Authorization", bearer(1L, UserRole.MEMBER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
    }

    @Test
    void ADMIN이_타인을_삭제하면_204가_반환된다() throws Exception {
        mockMvc.perform(delete("/api/users/2").header("Authorization", bearer(99L, UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }
}
