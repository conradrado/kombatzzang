package com.sprint.mission.matzzang.battle.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.exception.BattleNotFoundException;
import com.sprint.mission.matzzang.battle.service.BattleService;
import com.sprint.mission.matzzang.common.exception.GlobalExceptionHandler;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BattleControllerTest {

    @Mock
    private BattleService battleService;

    @InjectMocks
    private BattleController battleController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standalone 설정에서는 @AuthenticationPrincipal 해석기가 없으므로 직접 등록한다
        mockMvc = MockMvcBuilders.standaloneSetup(battleController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        JwtPrincipal principal = new JwtPrincipal(1L, UserRole.MEMBER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private BattleResponse response(BattleStatus status, Long winnerId) {
        return new BattleResponse(10L, 1L, 2L, Sport.BOXING, null, "서울", "한판", status, winnerId, null);
    }

    @Test
    void 대전을_신청하면_201과_응답이_반환된다() throws Exception {
        given(battleService.createBattle(eq(1L), any())).willReturn(response(BattleStatus.REQUESTED, null));

        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentId\":2,\"sport\":\"BOXING\",\"location\":\"서울\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        verify(battleService).createBattle(eq(1L), any());
    }

    @Test
    void opponentId가_없으면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sport\":\"BOXING\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 대전을_삭제하면_204가_반환된다() throws Exception {
        mockMvc.perform(delete("/api/battles/1"))
                .andExpect(status().isNoContent());

        verify(battleService).deleteBattle(1L);
    }

    @Test
    void 대전을_조회하면_200이_반환된다() throws Exception {
        given(battleService.getBattle(10L)).willReturn(response(BattleStatus.REQUESTED, null));

        mockMvc.perform(get("/api/battles/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengerId").value(1))
                .andExpect(jsonPath("$.opponentId").value(2));
    }

    @Test
    void 대전을_수락하면_200이_반환된다() throws Exception {
        given(battleService.acceptBattle(10L)).willReturn(response(BattleStatus.ACCEPTED, null));

        mockMvc.perform(patch("/api/battles/10/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void 결과_기록에_winnerId가_없으면_400이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/battles/10/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void opponentId가_0이면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentId\":0,\"sport\":\"BOXING\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(battleService);
    }

    @Test
    void scheduledAt이_과거이면_400이_반환된다() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentId\":2,\"sport\":\"BOXING\",\"scheduledAt\":\"2000-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(battleService);
    }

    @Test
    void 대전을_거절하면_200이_반환된다() throws Exception {
        given(battleService.rejectBattle(10L)).willReturn(response(BattleStatus.REJECTED, null));

        mockMvc.perform(patch("/api/battles/10/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void 대전을_취소하면_200이_반환된다() throws Exception {
        given(battleService.cancelBattle(10L)).willReturn(response(BattleStatus.CANCELED, null));

        mockMvc.perform(patch("/api/battles/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void 결과를_기록하면_200과_승자가_반환된다() throws Exception {
        given(battleService.recordResult(eq(10L), any())).willReturn(response(BattleStatus.COMPLETED, 1L));

        mockMvc.perform(patch("/api/battles/10/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winnerId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.winnerId").value(1));
    }

    @Test
    void sport가_없으면_400이_반환되고_서비스는_호출되지_않는다() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentId\":2}"))
                .andExpect(status().isBadRequest());

        verify(battleService, never()).createBattle(any(), any());
    }

    @Test
    void location이_256자이면_400이_반환되고_서비스는_호출되지_않는다() throws Exception {
        String location = "a".repeat(256);

        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentId\":2,\"sport\":\"BOXING\",\"location\":\"" + location + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(battleService);
    }

    @Test
    void 결과_기록에_winnerId가_0이면_400이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/battles/10/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winnerId\":0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(battleService);
    }

    @Test
    void 존재하지_않는_대전을_조회하면_404가_반환된다() throws Exception {
        given(battleService.getBattle(99L)).willThrow(new BattleNotFoundException(99L));

        mockMvc.perform(get("/api/battles/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void 수락할_수_없는_상태이면_409가_반환된다() throws Exception {
        given(battleService.acceptBattle(10L)).willThrow(new IllegalStateException("신청 상태의 대전만 수락할 수 있습니다."));

        mockMvc.perform(patch("/api/battles/10/accept"))
                .andExpect(status().isConflict());
    }

    @Test
    void 자기_자신에게_신청하면_400이_반환된다() throws Exception {
        given(battleService.createBattle(eq(1L), any()))
                .willThrow(new IllegalArgumentException("자기 자신에게 신청할 수 없습니다."));

        mockMvc.perform(post("/api/battles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opponentId\":1,\"sport\":\"BOXING\"}"))
                .andExpect(status().isBadRequest());
    }
}
