package com.sprint.mission.matzzang.battle.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class BattleSseEmitterRepositoryTest {

    private final BattleSseEmitterRepository repository = new BattleSseEmitterRepository();

    @Test
    void 저장한_emitter를_userId로_조회한다() {
        SseEmitter emitter = new SseEmitter();

        repository.save(1L, emitter);

        assertThat(repository.findByUserId(1L)).containsSame(emitter);
        assertThat(repository.findAll()).containsEntry(1L, emitter);
    }

    @Test
    void 없는_userId를_조회하면_비어있다() {
        assertThat(repository.findByUserId(1L)).isEmpty();
    }

    @Test
    void 삭제하면_조회되지_않는다() {
        repository.save(1L, new SseEmitter());

        repository.deleteByUserId(1L);

        assertThat(repository.findByUserId(1L)).isEmpty();
    }

    @Test
    void 같은_userId로_다시_저장하면_기존_emitter를_교체한다() {
        SseEmitter second = new SseEmitter();
        repository.save(1L, new SseEmitter());

        repository.save(1L, second);

        assertThat(repository.findByUserId(1L)).containsSame(second);
        assertThat(repository.findAll()).hasSize(1);
    }
}
