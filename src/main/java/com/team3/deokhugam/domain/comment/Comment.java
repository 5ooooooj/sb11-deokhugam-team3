package com.team3.deokhugam.domain.comment;

import com.team3.deokhugam.domain.base.SoftDeletableEntity;
import com.team3.deokhugam.exception.comment.CommentForbiddenException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends SoftDeletableEntity {

  @Column(name = "review_id", nullable = false, updatable = false)
  private UUID reviewId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  // 정적 팩토리 메서드
  public static Comment create(UUID reviewId, UUID userId, String content) {
    Comment comment = new Comment();
    comment.reviewId = reviewId;
    comment.userId = userId;
    comment.content = content;
    return comment;
  }

  // 내용 수정
  public void updateContent(String content) {
    this.content = content;
  }

  // 본인 확인
  public void validateOwner(UUID requestUserId) {
    if (!this.userId.equals(requestUserId)) {
      throw new CommentForbiddenException();
    }
  }
}