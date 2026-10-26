package com.sprint.mission.matzzang.battle.service;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.dto.BattleCreateCommand;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.dto.BattleResultCommand;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.battle.event.BattleRequestedEvent;
import com.sprint.mission.matzzang.battle.exception.BattleNotFoundException;
import com.sprint.mission.matzzang.battle.repository.BattleRepository;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleService {

    private final BattleRepository battleRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BattleResponse createBattle(Long challengerId, BattleCreateCommand command) {
        if (challengerId.equals(command.opponentId())) {
            throw new IllegalArgumentException("자기 자신에게 대전을 신청할 수 없습니다.");
        }
        if (!userRepository.existsById(command.opponentId())) {
            throw new UserNotFoundException(command.opponentId());
        }

        Battle battle = Battle.builder()
                .challengerId(challengerId)
                .opponentId(command.opponentId())
                .sport(command.sport())
                .scheduledAt(command.scheduledAt())
                .location(command.location())
                .message(command.message())
                .status(BattleStatus.REQUESTED) // 초기 대전은 신청 상태로 생성
                .build();

        BattleResponse response = BattleResponse.from(battleRepository.save(battle));
        // 커밋 이후 상대에게 SSE 알림을 보내기 위해 이벤트 발행
        eventPublisher.publishEvent(new BattleRequestedEvent(command.opponentId(), response));
        return response;
    }

    public BattleResponse getBattle(Long battleId) {
        return BattleResponse.from(findBattleById(battleId));
    }

    @Transactional
    public BattleResponse acceptBattle(Long battleId) {
        Battle battle = findBattleById(battleId);
        battle.accept();
        return BattleResponse.from(battle);
    }

    @Transactional
    public BattleResponse rejectBattle(Long battleId) {
        Battle battle = findBattleById(battleId);
        battle.reject();
        return BattleResponse.from(battle);
    }

    @Transactional
    public BattleResponse cancelBattle(Long battleId) {
        Battle battle = findBattleById(battleId);
        battle.cancel();
        return BattleResponse.from(battle);
    }

    @Transactional
    public BattleResponse recordResult(Long battleId, BattleResultCommand command) {
        Battle battle = findBattleById(battleId);
        battle.complete(command.winnerId());

        // 결과 검증 이후 승자 승수, 패자 패수를 같은 트랜잭션에서 갱신
        User winner = findUserById(command.winnerId());
        User loser = findUserById(battle.getLoserId());
        winner.updateRecord(winner.getWins() + 1, null, null);
        loser.updateRecord(null, loser.getLosses() + 1, null);
        return BattleResponse.from(battle);
    }

    @Transactional
    public void deleteBattle(Long battleId) {
        battleRepository.delete(findBattleById(battleId));
    }

    private Battle findBattleById(Long battleId) {
        return battleRepository.findById(battleId)
                .orElseThrow(() -> new BattleNotFoundException(battleId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
