package com.team3.deokhugam.domain.review;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class ReviewTestFactory {

  private User user = new User("review-factory@test.com", "리뷰작성자", "Password1!");
  private Book book = new Book(
      UUID.randomUUID(), "테스트 도서", "테스트 저자", "테스트 설명",
      "테스트 출판사", LocalDate.of(2026, 1, 1), null, null);
  private int rating = 5;
  private String content = "테스트 리뷰 내용";
  private UUID id;
  private Instant createdAt;

  private ReviewTestFactory() {
  }

  public static ReviewTestFactory review() {
    return new ReviewTestFactory();
  }

  public ReviewTestFactory user(User user) {
    this.user = user;
    return this;
  }

  public ReviewTestFactory book(Book book) {
    this.book = book;
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
    Review review = Review.create(user, book, rating, content);

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