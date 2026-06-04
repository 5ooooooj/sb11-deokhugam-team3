package com.team3.deokhugam.domain.review;

import com.team3.deokhugam.domain.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "review_likes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_likes_review_user",
        columnNames = {"review_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReviewLike {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "review_id",  updatable = false)
  private Review review;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  private ReviewLike(Review review, User user) {
    this.review = review;
    this.user = user;
  }

  public static ReviewLike create(Review review, User user) {
    return new ReviewLike(review, user);
  }
}