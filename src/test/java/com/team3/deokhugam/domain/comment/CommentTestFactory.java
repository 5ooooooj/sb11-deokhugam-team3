package com.team3.deokhugam.domain.comment;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewTestFactory;
import com.team3.deokhugam.domain.user.User;
import org.springframework.test.util.ReflectionTestUtils;

public class CommentTestFactory {

  private Review review = ReviewTestFactory.review().build();
  private User user = new User("test@test.com", "테스터", "Password1!");
  private String content = "테스트 댓글 내용";

  private CommentTestFactory() {}

  public static CommentTestFactory comment() {
    return new CommentTestFactory();
  }

  public CommentTestFactory review(Review review) {
    this.review = review;
    return this;
  }

  public CommentTestFactory user(User user) {
    this.user = user;
    return this;
  }

  public CommentTestFactory content(String content) {
    this.content = content;
    return this;
  }

  public Comment build() {
    Comment comment = new Comment();
    ReflectionTestUtils.setField(comment, "review", review);
    ReflectionTestUtils.setField(comment, "user", user);
    ReflectionTestUtils.setField(comment, "content", content);
    return comment;
  }
}