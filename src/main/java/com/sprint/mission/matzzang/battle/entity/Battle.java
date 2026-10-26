package com.sprint.mission.matzzang.battle.entity;

import com.sprint.mission.matzzang.battle.constants.BattleStatus;
import com.sprint.mission.matzzang.common.entity.UpdatableEntity;
import com.sprint.mission.matzzang.user.constants.Sport;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "battles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Battle extends UpdatableEntity {

    @Column(name = "challenger_id", nullable = false)
    private Long challengerId;

    @Column(name = "opponent_id", nullable = false)
    private Long opponentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sport sport;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BattleStatus status;

    @Column(name = "winner_id")
    private Long winnerId;

    @Embedded
    private BattleRule rule;

    @Builder
    private Battle(Long challengerId, Long opponentId, Sport sport, Instant scheduledAt,
                   String location, String message, BattleStatus status, BattleRule rule) {
        this.rule = rule != null ? rule : BattleRule.defaultRule();
        this.challengerId = challengerId;
        this.opponentId = opponentId;
        this.sport = sport;
        this.scheduledAt = scheduledAt;
        this.location = location;
        this.message = message;
        this.status = status;
    }

    public void changeRule(BattleRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("대전 규칙은 필수입니다.");
        }
        if (status != BattleStatus.REQUESTED) {
            throw new IllegalStateException("신청 상태의 대전만 규칙을 변경할 수 있습니다. status=" + status);
        }
        this.rule = rule;
    }

    public void accept() {
        if (status != BattleStatus.REQUESTED) {
            throw new IllegalStateException("신청 상태의 대전만 수락할 수 있습니다. status=" + status);
        }
        this.status = BattleStatus.ACCEPTED;
    }

    public void reject() {
        if (status != BattleStatus.REQUESTED) {
            throw new IllegalStateException("신청 상태의 대전만 거절할 수 있습니다. status=" + status);
        }
        this.status = BattleStatus.REJECTED;
    }

    public void cancel() {
        if (status != BattleStatus.REQUESTED && status != BattleStatus.ACCEPTED) {
            throw new IllegalStateException("신청 또는 수락 상태의 대전만 취소할 수 있습니다. status=" + status);
        }
        this.status = BattleStatus.CANCELED;
    }

    public void complete(Long winnerId) {
        if (status != BattleStatus.ACCEPTED) {
            throw new IllegalStateException("수락된 대전만 결과를 기록할 수 있습니다. status=" + status);
        }
        if (!challengerId.equals(winnerId) && !opponentId.equals(winnerId)) {
            throw new IllegalArgumentException("승자는 대전 참가자여야 합니다. winnerId=" + winnerId);
        }
        this.status = BattleStatus.COMPLETED;
        this.winnerId = winnerId;
    }
}
