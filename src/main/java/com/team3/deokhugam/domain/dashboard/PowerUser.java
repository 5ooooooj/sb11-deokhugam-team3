package com.team3.deokhugam.domain.dashboard;

import com.team3.deokhugam.batch.global.Period;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "power_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PowerUser {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "period", nullable = false)
  private Period period;

  @Column(name = "score", nullable = false)
  private BigDecimal score;

  @Column(name = "ranking", nullable = false)
  private int ranking;

  @Column(name = "review_score_sum", nullable = false)
  private BigDecimal reviewScoreSum;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  // PopularBook이랑 메서드 이름 통일
  public void assignRank(int rank) {
    this.ranking = rank;
  }
}
