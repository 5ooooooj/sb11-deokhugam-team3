package com.team3.deokhugam.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class ReviewTest {

    @Test
    @DisplayName("리뷰를 생성하면 필수 값이 정상 보존되고, 좋아요·댓글 수는 0으로 시작한다")
    void createReview() {

        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        int rating = 5;
        String content = "좋은 책";


        Review review = Review.create(userId, bookId, rating, content);


        assertThat(review.getUserId()).isEqualTo(userId);
        assertThat(review.getBookId()).isEqualTo(bookId);
        assertThat(review.getRating()).isEqualTo(rating);
        assertThat(review.getContent()).isEqualTo(content);
        assertThat(review.getLikeCount()).isZero();
        assertThat(review.getCommentCount()).isZero();
    }
    @Test
    @DisplayName("평점이 1~5 범위를 벗어나면 예외가 발생한다")
    void createReview_invalidRating() {
        // given
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();


        assertThatThrownBy(() -> Review.create(userId, bookId, 6, "내용"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}