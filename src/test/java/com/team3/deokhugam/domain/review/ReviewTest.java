package com.team3.deokhugam.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewTest {

    @Test
    @DisplayName("리뷰를 생성하면 필수 값이 정상 보존되고, 좋아요·댓글 수는 0으로 시작한다")
    void createReview() {
        User user = mock(User.class);
        Book book = mock(Book.class);
        int rating = 5;
        String content = "좋은 책";

        Review review = Review.create(user, book, rating, content);

        assertThat(review.getUser()).isEqualTo(user);
        assertThat(review.getBook()).isEqualTo(book);
        assertThat(review.getRating()).isEqualTo(rating);
        assertThat(review.getContent()).isEqualTo(content);
        assertThat(review.getLikeCount()).isZero();
        assertThat(review.getCommentCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 6, 100})
    @DisplayName("평점이 1~5 범위를 벗어나면 예외가 발생한다")
    void create_invalidRating(int invalidRating) {
        User user = mock(User.class);
        Book book = mock(Book.class);

        assertThatThrownBy(() -> Review.create(user, book, invalidRating, "내용"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("좋아요 수를 증가시키면 1 늘어난다")
    void increaseLikeCount() {
        Review review = Review.create(mock(User.class), mock(Book.class), 5, "내용");

        review.increaseLikeCount();

        assertThat(review.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("좋아요 수를 감소시키면 1 줄어든다")
    void decreaseLikeCount() {
        Review review = Review.create(mock(User.class), mock(Book.class), 5, "내용");
        review.increaseLikeCount();

        review.decreaseLikeCount();

        assertThat(review.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("좋아요 수는 0 미만으로 내려가지 않는다")
    void decreaseLikeCount_notBelowZero() {
        Review review = Review.create(mock(User.class), mock(Book.class), 5, "내용");

        review.decreaseLikeCount();

        assertThat(review.getLikeCount()).isZero();
    }

    @Test
    @DisplayName("댓글 수를 증가·감소시킬 수 있다")
    void changeCommentCount() {
        Review review = Review.create(mock(User.class), mock(Book.class), 5, "내용");

        review.increaseCommentCount();
        review.increaseCommentCount();
        review.decreaseCommentCount();

        assertThat(review.getCommentCount()).isEqualTo(1);
    }
}