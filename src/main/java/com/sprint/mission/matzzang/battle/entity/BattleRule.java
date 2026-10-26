package com.sprint.mission.matzzang.battle.entity;

import com.sprint.mission.matzzang.user.constants.WeightClass;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleRule {

    public static final int MIN_ROUNDS = 1;
    public static final int MAX_ROUNDS = 12;
    public static final int MIN_ROUND_MINUTES = 1;
    public static final int MAX_ROUND_MINUTES = 10;
    public static final int DEFAULT_ROUNDS = 3;
    public static final int DEFAULT_ROUND_MINUTES = 3;

    @Column(name = "round_count", nullable = false)
    private int rounds;

    @Column(name = "round_minutes", nullable = false)
    private int roundMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_class", nullable = false, length = 50)
    private WeightClass weightClass;

    private BattleRule(int rounds, int roundMinutes, WeightClass weightClass) {
        this.rounds = rounds;
        this.roundMinutes = roundMinutes;
        this.weightClass = weightClass;
    }

    public static BattleRule of(int rounds, int roundMinutes, WeightClass weightClass) {
        if (rounds < MIN_ROUNDS || rounds > MAX_ROUNDS) {
            throw new IllegalArgumentException(
                    "라운드 수는 " + MIN_ROUNDS + "~" + MAX_ROUNDS + " 사이여야 합니다. rounds=" + rounds);
        }
        if (roundMinutes < MIN_ROUND_MINUTES || roundMinutes > MAX_ROUND_MINUTES) {
            throw new IllegalArgumentException(
                    "라운드당 시간(분)은 " + MIN_ROUND_MINUTES + "~" + MAX_ROUND_MINUTES
                            + " 사이여야 합니다. roundMinutes=" + roundMinutes);
        }
        if (weightClass == null) {
            throw new IllegalArgumentException("체급은 필수입니다. weightClass=null");
        }
        return new BattleRule(rounds, roundMinutes, weightClass);
    }

    public static BattleRule defaultRule() {
        return new BattleRule(DEFAULT_ROUNDS, DEFAULT_ROUND_MINUTES, WeightClass.UNKNOWN);
    }

    public int totalMinutes() {
        return rounds * roundMinutes;
    }
}
