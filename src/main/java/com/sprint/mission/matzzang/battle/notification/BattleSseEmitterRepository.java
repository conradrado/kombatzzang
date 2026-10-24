package com.sprint.mission.matzzang.battle.notification;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class BattleSseEmitterRepository {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter save(Long userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        return emitter;
    }

    public Optional<SseEmitter> findByUserId(Long userId) {
        return Optional.ofNullable(emitters.get(userId));
    }

    public void deleteByUserId(Long userId) {
        emitters.remove(userId);
    }

    // 같은 emitter 일 때만 지워, 재구독으로 교체된 새 emitter 를 보호한다
    public void deleteByUserId(Long userId, SseEmitter emitter) {
        emitters.remove(userId, emitter);
    }

    public Map<Long, SseEmitter> findAll() {
        return Map.copyOf(emitters);
    }
}
