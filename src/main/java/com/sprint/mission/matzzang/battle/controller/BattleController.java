package com.sprint.mission.matzzang.battle.controller;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.battle.dto.BattleCreateCommand;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.dto.BattleResultCommand;
import com.sprint.mission.matzzang.battle.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Battle", description = "대전 관리 API")
@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    @Operation(summary = "대전 신청", description = "로그인한 사용자를 신청자로 상대에게 대전을 신청한다.")
    @ApiResponse(responseCode = "201", description = "생성됨")
    @PostMapping
    public ResponseEntity<BattleResponse> createBattle(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody BattleCreateCommand command
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(battleService.createBattle(principal.userId(), command));
    }

    @Operation(summary = "대전 조회", description = "대전 참가자 또는 ADMIN만 조회할 수 있다.")
    @PreAuthorize("@battleAccessGuard.isParticipantOrAdmin(authentication, #battleId)")
    @GetMapping("/{battleId}")
    public ResponseEntity<BattleResponse> getBattle(@Parameter(description = "대전 ID") @PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.getBattle(battleId));
    }

    @Operation(summary = "대전 수락", description = "상대방 본인 또는 ADMIN만 수락할 수 있다.")
    @PreAuthorize("@battleAccessGuard.isOpponentOrAdmin(authentication, #battleId)")
    @PatchMapping("/{battleId}/accept")
    public ResponseEntity<BattleResponse> acceptBattle(@Parameter(description = "대전 ID") @PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.acceptBattle(battleId));
    }

    @Operation(summary = "대전 거절", description = "상대방 본인 또는 ADMIN만 거절할 수 있다.")
    @PreAuthorize("@battleAccessGuard.isOpponentOrAdmin(authentication, #battleId)")
    @PatchMapping("/{battleId}/reject")
    public ResponseEntity<BattleResponse> rejectBattle(@Parameter(description = "대전 ID") @PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.rejectBattle(battleId));
    }

    @Operation(summary = "대전 취소", description = "신청자 본인 또는 ADMIN만 취소할 수 있다.")
    @PreAuthorize("@battleAccessGuard.isChallengerOrAdmin(authentication, #battleId)")
    @PatchMapping("/{battleId}/cancel")
    public ResponseEntity<BattleResponse> cancelBattle(@Parameter(description = "대전 ID") @PathVariable Long battleId) {
        return ResponseEntity.ok(battleService.cancelBattle(battleId));
    }

    @Operation(summary = "대전 결과 기록", description = "대전 참가자 또는 ADMIN만 결과를 기록할 수 있다.")
    @PreAuthorize("@battleAccessGuard.isParticipantOrAdmin(authentication, #battleId)")
    @PatchMapping("/{battleId}/result")
    public ResponseEntity<BattleResponse> recordResult(
            @Parameter(description = "대전 ID") @PathVariable Long battleId,
            @Valid @RequestBody BattleResultCommand command
    ) {
        return ResponseEntity.ok(battleService.recordResult(battleId, command));
    }

    @Operation(summary = "대전 전적 삭제", description = "ADMIN만 삭제할 수 있다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{battleId}")
    public ResponseEntity<Void> deleteBattle(@Parameter(description = "대전 ID") @PathVariable Long battleId) {
        battleService.deleteBattle(battleId);
        return ResponseEntity.noContent().build();
    }
}
