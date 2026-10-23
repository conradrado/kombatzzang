package com.sprint.mission.matzzang.battle.controller;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.battle.service.BattleNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Battle", description = "대전 관리 API")
@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
public class BattleNotificationController {

    private final BattleNotificationService battleNotificationService;

    @Operation(summary = "대전 알림 구독", description = "SSE로 대전 신청 알림을 실시간으로 받는다.")
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(battleNotificationService.subscribe(principal.userId()));
    }
}
