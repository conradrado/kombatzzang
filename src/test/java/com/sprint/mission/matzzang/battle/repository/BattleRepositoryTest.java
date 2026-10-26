package com.sprint.mission.matzzang.battle.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.battle.entity.Battle;
import com.sprint.mission.matzzang.user.constants.Sport;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BattleRepositoryTest {

    @Autowired
    private BattleRepository battleRepository;

    private Battle save(Long challengerId, Long opponentId) {
        return battleRepository.saveAndFlush(Battle.builder()
                .challengerId(challengerId)
                .opponentId(opponentId)
                .sport(Sport.BOXING)
                .status(BattleStatus.REQUESTED)
                .build());
    }

    private Battle saveAccepted(Long challengerId, Long opponentId) {
        Battle battle = save(challengerId, opponentId);
        battle.accept();
        return battleRepository.saveAndFlush(battle);
    }

    private Battle saveCompleted(Long challengerId, Long opponentId, Long winnerId) {
        Battle battle = saveAccepted(challengerId, opponentId);
        battle.complete(winnerId);
        return battleRepository.saveAndFlush(battle);
    }

    @Test
    void findAllByParticipantId는_도전자와_상대_대전을_모두_id_내림차순으로_반환한다() {
        Battle asChallenger = save(1L, 2L);
        save(3L, 4L);
        Battle asOpponent = save(5L, 1L);

        List<Battle> result = battleRepository.findAllByParticipantId(1L);

        assertThat(result).extracting(Battle::getId)
                .containsExactly(asOpponent.getId(), asChallenger.getId());
    }

    @Test
    void findAllByParticipantIdAndStatus는_지정한_상태의_대전만_반환한다() {
        save(1L, 2L);
        Battle accepted = saveAccepted(3L, 1L);

        List<Battle> result = battleRepository.findAllByParticipantIdAndStatus(1L, BattleStatus.ACCEPTED);

        assertThat(result).extracting(Battle::getId).containsExactly(accepted.getId());
    }

    @Test
    void findAllByOpponentIdAndStatusOrderByIdDesc는_받은_신청만_최신순으로_반환한다() {
        Battle first = save(2L, 1L);
        Battle second = save(3L, 1L);
        save(1L, 4L);
        saveAccepted(5L, 1L);

        List<Battle> result = battleRepository
                .findAllByOpponentIdAndStatusOrderByIdDesc(1L, BattleStatus.REQUESTED);

        assertThat(result).extracting(Battle::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void existsByChallengerIdAndOpponentIdAndStatusIn은_진행_중_대전이_있을_때만_true다() {
        Set<BattleStatus> active = Set.of(BattleStatus.REQUESTED, BattleStatus.ACCEPTED);
        save(1L, 2L);

        assertThat(battleRepository
                .existsByChallengerIdAndOpponentIdAndStatusIn(1L, 2L, active)).isTrue();
        assertThat(battleRepository
                .existsByChallengerIdAndOpponentIdAndStatusIn(2L, 1L, active)).isFalse();
    }

    @Test
    void existsByChallengerIdAndOpponentIdAndStatusIn은_종료된_대전만_있으면_false다() {
        Set<BattleStatus> active = Set.of(BattleStatus.REQUESTED, BattleStatus.ACCEPTED);
        Battle canceled = save(1L, 2L);
        canceled.cancel();
        battleRepository.saveAndFlush(canceled);
        Battle rejected = save(1L, 2L);
        rejected.reject();
        battleRepository.saveAndFlush(rejected);

        assertThat(battleRepository
                .existsByChallengerIdAndOpponentIdAndStatusIn(1L, 2L, active)).isFalse();
    }

    @Test
    void countByWinnerId는_승리_횟수를_반환한다() {
        saveCompleted(1L, 2L, 1L);
        saveCompleted(3L, 1L, 1L);
        saveCompleted(1L, 4L, 4L);

        assertThat(battleRepository.countByWinnerId(1L)).isEqualTo(2L);
        assertThat(battleRepository.countByWinnerId(99L)).isZero();
    }

    @Test
    void countByParticipantIdAndStatus는_참가한_완료_대전_수를_반환한다() {
        saveCompleted(1L, 2L, 1L);
        saveCompleted(3L, 1L, 3L);
        saveAccepted(1L, 4L);
        saveCompleted(5L, 6L, 5L);

        assertThat(battleRepository.countByParticipantIdAndStatus(1L, BattleStatus.COMPLETED)).isEqualTo(2L);
        assertThat(battleRepository.countByParticipantIdAndStatus(99L, BattleStatus.COMPLETED)).isZero();
    }
}
