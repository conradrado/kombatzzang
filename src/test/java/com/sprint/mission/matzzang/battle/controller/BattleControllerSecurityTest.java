package com.sprint.mission.matzzang.battle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.config.SecurityConfig;
import com.sprint.mission.matzzang.auth.jwt.JwtAuthenticationFilter;
import com.sprint.mission.matzzang.auth.jwt.JwtTokenProvider;
import com.sprint.mission.matzzang.auth.security.JwtAccessDeniedHandler;
import com.sprint.mission.matzzang.auth.security.JwtAuthenticationEntryPoint;
import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.battle.repository.BattleRepository;
import com.sprint.mission.matzzang.battle.security.BattleAccessGuard;
import com.sprint.mission.matzzang.battle.service.BattleService;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BattleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-security-config-test-0123456789",
        "jwt.access-token-expiration-ms=1800000",
        "jwt.refresh-token-expiration-ms=1209600000"
})
class BattleControllerSecurityTest {

    // @Import 로 등록하면 빈 이름이 FQCN 이 되어 SpEL 의 @battleAccessGuard 를 찾지 못하므로 이름을 지정해 등록한다.
    @TestConfiguration
    static class GuardConfig {

        @Bean
        BattleAccessGuard battleAccessGuard(BattleRepository battleRepository) {
            return new BattleAccessGuard(battleRepository);
        }
    }

    // 신청자 1, 상대방 2, 제3자 3, 관리자 99
    private static final Long CHALLENGER = 1L;
    private static final Long OPPONENT = 2L;
    private static final Long OUTSIDER = 3L;
    private static final Long ADMIN = 99L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private BattleService battleService;

    @MockitoBean
    private BattleRepository battleRepository;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        Battle battle = Battle.builder()
                .challengerId(CHALLENGER)
                .opponentId(OPPONENT)
                .sport(Sport.BOXING)
                .status(BattleStatus.REQUESTED)
                .build();
        given(battleRepository.findById(10L)).willReturn(Optional.of(battle));
    }

    private String bearer(Long userId, UserRole role) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(userId, role);
    }

    private BattleResponse response(BattleStatus status) {
        return new BattleResponse(10L, CHALLENGER, OPPONENT, Sport.BOXING, null, null, null, status, null, null);
    }

    @Test
    void 토큰_없이_조회하면_401이_반환된다() throws Exception {
        mockMvc.perform(get("/api/battles/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 제3자가_조회하면_403이_반환되고_서비스는_호출되지_않는다() throws Exception {
        mockMvc.perform(get("/api/battles/10").header("Authorization", bearer(OUTSIDER, UserRole.MEMBER)))
                .andExpect(status().isForbidden());

        verify(battleService, never()).getBattle(any());
    }

    @Test
    void 신청자가_조회하면_200이_반환된다() throws Exception {
        given(battleService.getBattle(10L)).willReturn(response(BattleStatus.REQUESTED));

        mockMvc.perform(get("/api/battles/10").header("Authorization", bearer(CHALLENGER, UserRole.MEMBER)))
                .andExpect(status().isOk());
    }

    @Test
    void 신청자가_수락하면_403이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/battles/10/accept").header("Authorization", bearer(CHALLENGER, UserRole.MEMBER)))
                .andExpect(status().isForbidden());

        verify(battleService, never()).acceptBattle(any());
    }

    @Test
    void 상대방이_수락하면_200이_반환된다() throws Exception {
        given(battleService.acceptBattle(10L)).willReturn(response(BattleStatus.ACCEPTED));

        mockMvc.perform(patch("/api/battles/10/accept").header("Authorization", bearer(OPPONENT, UserRole.MEMBER)))
                .andExpect(status().isOk());
    }

    @Test
    void 상대방이_취소하면_403이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/battles/10/cancel").header("Authorization", bearer(OPPONENT, UserRole.MEMBER)))
                .andExpect(status().isForbidden());

        verify(battleService, never()).cancelBattle(any());
    }

    @Test
    void 신청자가_취소하면_200이_반환된다() throws Exception {
        given(battleService.cancelBattle(10L)).willReturn(response(BattleStatus.CANCELED));

        mockMvc.perform(patch("/api/battles/10/cancel").header("Authorization", bearer(CHALLENGER, UserRole.MEMBER)))
                .andExpect(status().isOk());
    }

    @Test
    void 제3자가_결과를_기록하면_403이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/battles/10/result")
                        .header("Authorization", bearer(OUTSIDER, UserRole.MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winnerId\":1}"))
                .andExpect(status().isForbidden());

        verify(battleService, never()).recordResult(any(), any());
    }

    @Test
    void MEMBER가_삭제하면_403이_반환되고_서비스는_호출되지_않는다() throws Exception {
        mockMvc.perform(delete("/api/battles/10").header("Authorization", bearer(CHALLENGER, UserRole.MEMBER)))
                .andExpect(status().isForbidden());

        verify(battleService, never()).deleteBattle(any());
    }

    @Test
    void ADMIN이_삭제하면_204가_반환된다() throws Exception {
        mockMvc.perform(delete("/api/battles/10").header("Authorization", bearer(ADMIN, UserRole.ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void ADMIN이_제3자_대전을_수락하면_200이_반환된다() throws Exception {
        given(battleService.acceptBattle(10L)).willReturn(response(BattleStatus.ACCEPTED));

        mockMvc.perform(patch("/api/battles/10/accept").header("Authorization", bearer(ADMIN, UserRole.ADMIN)))
                .andExpect(status().isOk());
    }
}
