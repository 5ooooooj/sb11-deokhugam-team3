package com.team3.deokhugam.domain.review;

import com.team3.deokhugam.batch.global.Period;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "popular_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopularReview {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "review_id", nullable = false)
  private UUID reviewID;

  @Enumerated
  @Column(name = "period", nullable = false)
  private Period period;

  @Column(name = "score", nullable = false)
  private double score;

  @Column(name = "rank", nullable = false)
  private int rank;

  @Column(name = "like_count", nullable = false)
  private int likeCount;

  @Column(name = "comment_count", nullable = false)
  private int commentCount;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;
}
