package com.sprint.mission.matzzang.battle.event;

import com.sprint.mission.matzzang.battle.service.BattleNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BattleRequestedEventListener {

    private final BattleNotificationService battleNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BattleRequestedEvent event) {
        battleNotificationService.notifyBattleRequested(event.opponentId(), event.battle());
    }
}
