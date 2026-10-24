package com.sprint.mission.matzzang.battle.event;

import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.service.BattleNotificationService;
import com.sprint.mission.matzzang.user.constants.Sport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattleRequestedEventListenerTest {

    @Mock
    private BattleNotificationService battleNotificationService;

    @InjectMocks
    private BattleRequestedEventListener listener;

    @Test
    void 이벤트를_받으면_상대에게_알림을_보낸다() {
        BattleResponse battle = new BattleResponse(10L, 1L, 2L, Sport.BOXING, null, "서울", "한판",
                BattleStatus.REQUESTED, null, null);

        listener.handle(new BattleRequestedEvent(2L, battle));

        verify(battleNotificationService).notifyBattleRequested(2L, battle);
    }
}
