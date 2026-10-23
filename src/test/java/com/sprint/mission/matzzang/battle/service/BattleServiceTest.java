package com.sprint.mission.matzzang.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.dto.BattleCreateCommand;
import com.sprint.mission.matzzang.battle.dto.BattleResponse;
import com.sprint.mission.matzzang.battle.dto.BattleResultCommand;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.battle.exception.BattleNotFoundException;
import com.sprint.mission.matzzang.battle.repository.BattleRepository;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattleServiceTest {

    @Mock
    private BattleRepository battleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BattleService battleService;

    private Battle battle(BattleStatus status) {
        return Battle.builder()
                .challengerId(1L)
                .opponentId(2L)
                .sport(Sport.BOXING)
                .status(status)
                .build();
    }

    @Test
    void 대전을_신청하면_REQUESTED_상태와_신청자로_저장한다() {
        Instant scheduledAt = Instant.parse("2026-12-01T10:00:00Z");
        given(userRepository.existsById(2L)).willReturn(true);
        given(battleRepository.save(any(Battle.class))).willAnswer(invocation -> invocation.getArgument(0));

        BattleResponse response = battleService.createBattle(1L,
                new BattleCreateCommand(2L, Sport.MMA, scheduledAt, "서울 체육관", "한판 붙자"));

        ArgumentCaptor<Battle> captor = ArgumentCaptor.forClass(Battle.class);
        verify(battleRepository).save(captor.capture());
        Battle saved = captor.getValue();
        assertThat(saved.getChallengerId()).isEqualTo(1L);
        assertThat(saved.getOpponentId()).isEqualTo(2L);
        assertThat(saved.getSport()).isEqualTo(Sport.MMA);
        assertThat(saved.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(saved.getLocation()).isEqualTo("서울 체육관");
        assertThat(saved.getMessage()).isEqualTo("한판 붙자");
        assertThat(saved.getStatus()).isEqualTo(BattleStatus.REQUESTED);
        assertThat(response.status()).isEqualTo(BattleStatus.REQUESTED);
    }

    @Test
    void 자기_자신에게_신청하면_예외가_발생한다() {
        assertThatThrownBy(() -> battleService.createBattle(1L,
                new BattleCreateCommand(1L, Sport.BOXING, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(battleRepository, never()).save(any());
    }

    @Test
    void 상대_유저가_없으면_UserNotFoundException이_발생한다() {
        given(userRepository.existsById(2L)).willReturn(false);

        assertThatThrownBy(() -> battleService.createBattle(1L,
                new BattleCreateCommand(2L, Sport.BOXING, null, null, null)))
                .isInstanceOf(UserNotFoundException.class);

        verify(battleRepository, never()).save(any());
    }

    @Test
    void 대전을_조회하면_응답이_반환된다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.REQUESTED)));

        BattleResponse response = battleService.getBattle(1L);

        assertThat(response.challengerId()).isEqualTo(1L);
        assertThat(response.opponentId()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(BattleStatus.REQUESTED);
    }

    @Test
    void 없는_대전을_조회하면_BattleNotFoundException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.getBattle(1L)).isInstanceOf(BattleNotFoundException.class);
    }

    @Test
    void 대전을_수락하면_ACCEPTED가_된다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.REQUESTED)));

        BattleResponse response = battleService.acceptBattle(1L);

        assertThat(response.status()).isEqualTo(BattleStatus.ACCEPTED);
    }

    @Test
    void 없는_대전을_수락하면_BattleNotFoundException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.acceptBattle(1L)).isInstanceOf(BattleNotFoundException.class);
    }

    @Test
    void 이미_수락된_대전을_수락하면_IllegalStateException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.ACCEPTED)));

        assertThatThrownBy(() -> battleService.acceptBattle(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 대전을_거절하면_REJECTED가_된다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.REQUESTED)));

        BattleResponse response = battleService.rejectBattle(1L);

        assertThat(response.status()).isEqualTo(BattleStatus.REJECTED);
    }

    @Test
    void 없는_대전을_거절하면_BattleNotFoundException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.rejectBattle(1L)).isInstanceOf(BattleNotFoundException.class);
    }

    @Test
    void 수락된_대전을_거절하면_IllegalStateException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.ACCEPTED)));

        assertThatThrownBy(() -> battleService.rejectBattle(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 대전을_취소하면_CANCELED가_된다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.ACCEPTED)));

        BattleResponse response = battleService.cancelBattle(1L);

        assertThat(response.status()).isEqualTo(BattleStatus.CANCELED);
    }

    @Test
    void 없는_대전을_취소하면_BattleNotFoundException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.cancelBattle(1L)).isInstanceOf(BattleNotFoundException.class);
    }

    @Test
    void 완료된_대전을_취소하면_IllegalStateException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.COMPLETED)));

        assertThatThrownBy(() -> battleService.cancelBattle(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 결과를_기록하면_COMPLETED와_승자가_반영된다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.ACCEPTED)));

        BattleResponse response = battleService.recordResult(1L, new BattleResultCommand(2L));

        assertThat(response.status()).isEqualTo(BattleStatus.COMPLETED);
        assertThat(response.winnerId()).isEqualTo(2L);
    }

    @Test
    void 없는_대전에_결과를_기록하면_BattleNotFoundException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.recordResult(1L, new BattleResultCommand(2L)))
                .isInstanceOf(BattleNotFoundException.class);
    }

    @Test
    void 신청_상태에서_결과를_기록하면_IllegalStateException이_발생한다() {
        given(battleRepository.findById(1L)).willReturn(Optional.of(battle(BattleStatus.REQUESTED)));

        assertThatThrownBy(() -> battleService.recordResult(1L, new BattleResultCommand(2L)))
                .isInstanceOf(IllegalStateException.class);
    }
}
