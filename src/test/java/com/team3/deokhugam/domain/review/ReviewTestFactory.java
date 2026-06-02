package com.team3.deokhugam.domain.review;

import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class ReviewTestFactory {

  private UUID userId = UUID.randomUUID();
  private UUID bookId = UUID.randomUUID();
  private int rating = 5;
  private String content = "테스트 리뷰 내용";
  private UUID id;
  private Instant createdAt;

  private ReviewTestFactory() {
  }

  public static ReviewTestFactory review() {
    return new ReviewTestFactory();
  }

  public ReviewTestFactory userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public ReviewTestFactory bookId(UUID bookId) {
    this.bookId = bookId;
    return this;
  }

  public ReviewTestFactory rating(int rating) {
    this.rating = rating;
    return this;
  }

  public ReviewTestFactory content(String content) {
    this.content = content;
    return this;
  }

  public ReviewTestFactory id(UUID id) {
    this.id = id;
    return this;
  }

  public ReviewTestFactory createdAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Review build() {
    Review review = Review.create(userId, bookId, rating, content);

    if (id != null) {
      ReflectionTestUtils.setField(review, "id", id);
    }

    if (createdAt != null) {
      ReflectionTestUtils.setField(review, "createdAt", createdAt);
      ReflectionTestUtils.setField(review, "updatedAt", createdAt);
    }

    return review;
  }
}