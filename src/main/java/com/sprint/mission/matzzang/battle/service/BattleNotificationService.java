package com.sprint.mission.matzzang.battle.service;

import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.notification.BattleSseEmitterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BattleNotificationService {

    private static final long DEFAULT_TIMEOUT = 60L * 60 * 1000;
    private static final String CONNECT_EVENT = "connect";
    private static final String BATTLE_REQUESTED_EVENT = "battle-requested";
    private static final String HEARTBEAT_EVENT = "heartbeat";

    private final BattleSseEmitterRepository emitterRepository;

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = emitterRepository.save(userId, new SseEmitter(DEFAULT_TIMEOUT));

        emitter.onCompletion(() -> emitterRepository.deleteByUserId(userId, emitter));
        emitter.onTimeout(() -> emitterRepository.deleteByUserId(userId, emitter));
        emitter.onError(e -> emitterRepository.deleteByUserId(userId, emitter));

        // 연결 직후 더미 이벤트를 보내 503 방지
        try {
            emitter.send(SseEmitter.event().name(CONNECT_EVENT).data("connected"));
        } catch (IOException | IllegalStateException e) {
            emitterRepository.deleteByUserId(userId, emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void notifyBattleRequested(Long opponentId, BattleResponse battle) {
        emitterRepository.findByUserId(opponentId).ifPresent(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(battle.id()))
                        .name(BATTLE_REQUESTED_EVENT)
                        .data(battle));
            } catch (IOException | IllegalStateException e) {
                log.warn("대전 신청 알림 전송 실패: userId={}", opponentId, e);
                emitterRepository.deleteByUserId(opponentId, emitter);
            }
        });
    }

    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        emitterRepository.findAll().forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(HEARTBEAT_EVENT).data("ping"));
            } catch (IOException | IllegalStateException e) {
                emitterRepository.deleteByUserId(userId, emitter);
            }
        });
    }
}
