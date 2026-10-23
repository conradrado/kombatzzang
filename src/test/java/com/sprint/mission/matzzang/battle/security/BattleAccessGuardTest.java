package com.sprint.mission.matzzang.battle.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.battle.repository.BattleRepository;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class BattleAccessGuardTest {

    @Mock
    private Authentication authentication;

    @Mock
    private BattleRepository battleRepository;

    @InjectMocks
    private BattleAccessGuard battleAccessGuard;

    // 신청자 1, 상대방 2
    private Battle battle() {
        return Battle.builder()
                .challengerId(1L)
                .opponentId(2L)
                .sport(Sport.BOXING)
                .status(BattleStatus.REQUESTED)
                .build();
    }

    private void loginAs(Long userId, UserRole role) {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(userId, role));
    }

    private void battleExists() {
        given(battleRepository.findById(10L)).willReturn(Optional.of(battle()));
    }

    // isChallengerOrAdmin

    @Test
    void 신청자_본인이면_isChallengerOrAdmin이_true다() {
        loginAs(1L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isChallengerOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 신청자가_아니면_isChallengerOrAdmin이_false다() {
        loginAs(2L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isChallengerOrAdmin(authentication, 10L)).isFalse();
    }

    @Test
    void ADMIN이면_isChallengerOrAdmin이_true다() {
        loginAs(3L, UserRole.ADMIN);

        assertThat(battleAccessGuard.isChallengerOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 대전이_없으면_isChallengerOrAdmin이_true다() {
        loginAs(3L, UserRole.MEMBER);
        given(battleRepository.findById(10L)).willReturn(Optional.empty());

        assertThat(battleAccessGuard.isChallengerOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void Authentication이_null이면_isChallengerOrAdmin이_false다() {
        assertThat(battleAccessGuard.isChallengerOrAdmin(null, 10L)).isFalse();
    }

    @Test
    void principal이_JwtPrincipal이_아니면_isChallengerOrAdmin이_false다() {
        given(authentication.getPrincipal()).willReturn("anonymousUser");

        assertThat(battleAccessGuard.isChallengerOrAdmin(authentication, 10L)).isFalse();
    }

    // isOpponentOrAdmin

    @Test
    void 상대방_본인이면_isOpponentOrAdmin이_true다() {
        loginAs(2L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isOpponentOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 상대방이_아니면_isOpponentOrAdmin이_false다() {
        loginAs(1L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isOpponentOrAdmin(authentication, 10L)).isFalse();
    }

    @Test
    void ADMIN이면_isOpponentOrAdmin이_true다() {
        loginAs(3L, UserRole.ADMIN);

        assertThat(battleAccessGuard.isOpponentOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 대전이_없으면_isOpponentOrAdmin이_true다() {
        loginAs(3L, UserRole.MEMBER);
        given(battleRepository.findById(10L)).willReturn(Optional.empty());

        assertThat(battleAccessGuard.isOpponentOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void Authentication이_null이면_isOpponentOrAdmin이_false다() {
        assertThat(battleAccessGuard.isOpponentOrAdmin(null, 10L)).isFalse();
    }

    @Test
    void principal이_JwtPrincipal이_아니면_isOpponentOrAdmin이_false다() {
        given(authentication.getPrincipal()).willReturn("anonymousUser");

        assertThat(battleAccessGuard.isOpponentOrAdmin(authentication, 10L)).isFalse();
    }

    // isParticipantOrAdmin

    @Test
    void 신청자면_isParticipantOrAdmin이_true다() {
        loginAs(1L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isParticipantOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 상대방이면_isParticipantOrAdmin이_true다() {
        loginAs(2L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isParticipantOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 참가자가_아니면_isParticipantOrAdmin이_false다() {
        loginAs(3L, UserRole.MEMBER);
        battleExists();

        assertThat(battleAccessGuard.isParticipantOrAdmin(authentication, 10L)).isFalse();
    }

    @Test
    void ADMIN이면_isParticipantOrAdmin이_true다() {
        loginAs(3L, UserRole.ADMIN);

        assertThat(battleAccessGuard.isParticipantOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 대전이_없으면_isParticipantOrAdmin이_true다() {
        loginAs(3L, UserRole.MEMBER);
        given(battleRepository.findById(10L)).willReturn(Optional.empty());

        assertThat(battleAccessGuard.isParticipantOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void Authentication이_null이면_isParticipantOrAdmin이_false다() {
        assertThat(battleAccessGuard.isParticipantOrAdmin(null, 10L)).isFalse();
    }

    @Test
    void principal이_JwtPrincipal이_아니면_isParticipantOrAdmin이_false다() {
        given(authentication.getPrincipal()).willReturn("anonymousUser");

        assertThat(battleAccessGuard.isParticipantOrAdmin(authentication, 10L)).isFalse();
    }
}
