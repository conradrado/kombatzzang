package com.sprint.mission.matzzang.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.notification.BattleSseEmitterRepository;
import com.sprint.mission.matzzang.user.constants.Sport;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class BattleNotificationServiceTest {

    private BattleSseEmitterRepository repository;
    private BattleNotificationService service;

    @BeforeEach
    void setUp() {
        repository = new BattleSseEmitterRepository();
        service = new BattleNotificationService(repository);
    }

    private BattleResponse battle() {
        return new BattleResponse(10L, 1L, 2L, Sport.BOXING, null, "서울", "한판", BattleStatus.REQUESTED, null, null);
    }

    @Test
    void 구독하면_저장소에_등록된다() {
        SseEmitter emitter = service.subscribe(2L);

        assertThat(repository.findByUserId(2L)).containsSame(emitter);
    }

    @Test
    void 대전_신청_알림은_구독자_emitter로_전송된다() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        repository.save(2L, emitter);

        service.notifyBattleRequested(2L, battle());

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void 구독자가_없으면_예외없이_끝난다() {
        assertThatCode(() -> service.notifyBattleRequested(2L, battle())).doesNotThrowAnyException();
    }

    @Test
    void 전송이_실패하면_저장소에서_지우고_예외를_던지지_않는다() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("broken")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        repository.save(2L, emitter);

        assertThatCode(() -> service.notifyBattleRequested(2L, battle())).doesNotThrowAnyException();

        assertThat(repository.findByUserId(2L)).isEmpty();
    }

    @Test
    void 하트비트는_모든_emitter에_전송하고_실패한_emitter는_지운다() throws IOException {
        SseEmitter healthy = mock(SseEmitter.class);
        SseEmitter broken = mock(SseEmitter.class);
        doThrow(new IOException("broken")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        repository.save(1L, healthy);
        repository.save(2L, broken);

        service.sendHeartbeat();

        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
        verify(broken).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(repository.findByUserId(1L)).isPresent();
        assertThat(repository.findByUserId(2L)).isEmpty();
    }
}
