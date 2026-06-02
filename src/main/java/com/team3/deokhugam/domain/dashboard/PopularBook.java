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
@Table(name = "popular_books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PopularBook {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "book_id", nullable = false)
  private UUID bookId;

  @Enumerated(EnumType.STRING)
  @Column(name = "period", nullable = false)
  private Period period;

  @Column(name = "score", nullable = false)
  private BigDecimal score;

  @Column(name = "rank", nullable = false)
  private int rank;

  @Column(name = "review_count", nullable = false)
  private int reviewCount;

  @Column(name = "rating", nullable = false)
  private BigDecimal rating;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  public void assignRank(int rank) {
    this.rank = rank;
  }
}
