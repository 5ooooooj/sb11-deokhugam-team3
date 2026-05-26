package com.team3.deokhugam.repository.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.review.Review;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("local")
class ReviewRepositoryTest {

  @Autowired
  private ReviewRepository reviewRepository;

  @Test
  @DisplayName("리뷰를 저장하고 ID로 다시 조회할 수 있다")
  void saveAndFind() {
    // given - 저장할 리뷰 준비
    Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 5, "좋은 책이에요");

    // when - 저장하고 ID로 다시 조회
    Review saved = reviewRepository.save(review);
    Review found = reviewRepository.findById(saved.getId()).orElseThrow();

    // then - 저장된 값이 그대로인지 확인
    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getRating()).isEqualTo(5);
    assertThat(found.getContent()).isEqualTo("좋은 책이에요");
    assertThat(found.getCreatedAt()).isNotNull();  // 자동 기록 확인
  }

  @Test
  @DisplayName("저장한 리뷰를 삭제할 수 있다")
  void deleteReview() {
    // given - 리뷰 저장
    Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "삭제될 리뷰");
    Review saved = reviewRepository.save(review);

    // when - 삭제
    reviewRepository.delete(saved);

    // then - 조회하면 없어야 함
    assertThat(reviewRepository.findById(saved.getId())).isEmpty();
  }
}