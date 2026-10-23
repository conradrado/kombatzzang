package com.sprint.mission.matzzang.battle.security;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.battle.repository.BattleRepository;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.util.function.BiPredicate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BattleAccessGuard {

    private final BattleRepository battleRepository;

    public boolean isChallengerOrAdmin(Authentication authentication, Long battleId) {
        return check(authentication, battleId, (battle, userId) -> battle.getChallengerId().equals(userId));
    }

    public boolean isOpponentOrAdmin(Authentication authentication, Long battleId) {
        return check(authentication, battleId, (battle, userId) -> battle.getOpponentId().equals(userId));
    }

    public boolean isParticipantOrAdmin(Authentication authentication, Long battleId) {
        return check(authentication, battleId, (battle, userId) ->
                battle.getChallengerId().equals(userId) || battle.getOpponentId().equals(userId));
    }

    private boolean check(Authentication authentication, Long battleId, BiPredicate<Battle, Long> rule) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return false;
        }
        if (principal.role() == UserRole.ADMIN) {
            return true;
        }
        // 대전이 없으면 통과시켜 서비스에서 BattleNotFoundException(404)을 던지게 한다
        return battleRepository.findById(battleId)
                .map(battle -> rule.test(battle, principal.userId()))
                .orElse(true);
    }
}
