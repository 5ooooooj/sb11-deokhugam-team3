package com.team3.deokhugam.repository.review;

import static com.team3.deokhugam.domain.review.ReviewTestFactory.review;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ReviewRepositoryCustomImpl.class})
class ReviewRepositoryTest {

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("리뷰를 저장하고 ID로 다시 조회할 수 있다")
  void saveAndFind() {
    Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 5, "좋은 책이에요");

    Review saved = reviewRepository.save(review);
    entityManager.flush();
    entityManager.clear();
    Review found = reviewRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getRating()).isEqualTo(5);
    assertThat(found.getContent()).isEqualTo("좋은 책이에요");
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("저장한 리뷰를 삭제할 수 있다")
  void deleteReview() {
    Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), 4, "삭제될 리뷰");
    Review saved = reviewRepository.save(review);

    reviewRepository.delete(saved);

    assertThat(reviewRepository.findById(saved.getId())).isEmpty();
  }

  @Test
  @DisplayName("userId로 필터링 검색이 동작한다")
  void searchByUserId() {
    UUID targetUserId = UUID.randomUUID();

    reviewRepository.saveAll(List.of(
        review().userId(targetUserId).content("내 리뷰 1").build(),
        review().userId(targetUserId).content("내 리뷰 2").build(),
        review().userId(UUID.randomUUID()).content("남의 리뷰").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        targetUserId, null, null, "createdAt", "DESC", null, null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(r -> r.getUserId().equals(targetUserId));
  }

  @Test
  @DisplayName("bookId로 필터링 검색이 동작한다")
  void searchByBookId() {
    UUID targetBookId = UUID.randomUUID();

    reviewRepository.saveAll(List.of(
        review().bookId(targetBookId).content("이 책 리뷰 1").build(),
        review().bookId(UUID.randomUUID()).content("다른 책 리뷰").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, targetBookId, null, "createdAt", "DESC", null, null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBookId()).isEqualTo(targetBookId);
  }

  @Test
  @DisplayName("keyword로 content 부분 일치 검색이 동작한다")
  void searchByKeyword() {
    reviewRepository.saveAll(List.of(
        review().content("repo-keyword-스프링 정말 재밌어요").build(),
        review().content("repo-keyword-자바 어렵네요").build(),
        review().content("이 책 너무 별로").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-keyword-스프링", "createdAt", "DESC", null, null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).contains("repo-keyword-스프링");
  }

  @Test
  @DisplayName("rating 기준 내림차순 정렬이 동작한다")
  void searchOrderByRatingDesc() {
    reviewRepository.saveAll(List.of(
        review().rating(2).content("repo-rating-sort 낮음").build(),
        review().rating(5).content("repo-rating-sort 높음").build(),
        review().rating(3).content("repo-rating-sort 중간").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-rating-sort", "rating", "DESC", null, null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(3);
    assertThat(result).extracting(Review::getRating).containsExactly(5, 3, 2);
  }

  @Test
  @DisplayName("limit 조건이 적용된다")
  void searchWithLimit() {
    reviewRepository.saveAll(List.of(
        review().content("repo-limit A").build(),
        review().content("repo-limit B").build(),
        review().content("repo-limit C").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-limit", "createdAt", "DESC", null, null, 2, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("논리 삭제된 리뷰는 검색에서 제외된다")
  void searchExcludesDeleted() {
    Review review1 = review().content("repo-deleted 살아있음").build();
    Review review2 = review().content("repo-deleted 삭제됨").build();
    reviewRepository.saveAll(List.of(review1, review2));

    review2.softDelete();
    reviewRepository.save(review2);

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-deleted", "createdAt", "DESC", null, null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).contains("살아있음");
  }

  @Test
  @DisplayName("검색 조건에 맞는 리뷰 전체 개수를 조회한다")
  void countBySearchCondition() {
    UUID targetUserId = UUID.randomUUID();
    reviewRepository.saveAll(List.of(
        review().userId(targetUserId).content("count A").build(),
        review().userId(targetUserId).content("count B").build(),
        review().userId(UUID.randomUUID()).content("count C").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        targetUserId, null, null, "createdAt", "DESC", null, null, 10, UUID.randomUUID()
    );

    long result = reviewRepository.count(request);

    assertThat(result).isEqualTo(2);
  }
}