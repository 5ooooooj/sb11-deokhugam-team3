package com.team3.deokhugam.domain.review;

import com.team3.deokhugam.domain.base.SoftDeletableEntity;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    private Book book;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    private Review(User user, Book book, int rating, String content) {
        this.user = user;
        this.book = book;
        this.rating = rating;
        this.content = content;
    }

    public static Review create(User user, Book book, int rating, String content) {
        if (user == null || book == null) {
            throw new IllegalArgumentException("작성자와 도서는 필수입니다.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다. 입력값: " + rating);
        }
        return new Review(user, book, rating, content);
    }


    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    public UUID getBookId() {
        return book != null ? book.getId() : null;
    }

    public void update(int rating, String content) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다. 입력값: " + rating);
        }
        this.rating = rating;
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }
}