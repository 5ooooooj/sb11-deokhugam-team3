package com.team3.deokhugam.repository.dashboard;

import static com.team3.deokhugam.domain.book.BookTestFactory.book;
import static com.team3.deokhugam.domain.review.ReviewTestFactory.review;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.repository.BaseRepositoryTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

@DisplayName("PopularReview 레포지토리 테스트")
class PopularReviewRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private PopularReviewRepository popularReviewRepository;

  @Autowired
  private TestEntityManager em;

  private UUID reviewId1;
  private UUID reviewId2;

  @BeforeEach
  void setUp() {
    Book book = em.persistAndFlush(book()
        .title("도서1")
        .author("저자1")
        .description("설명1")
        .publisher("출판사1")
        .publishedDate(LocalDate.of(2013, 12, 30))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/book1.jpg")
        .build());

    User user = em.persistAndFlush(new User("test1@test.com", "유저1", "encoded"));

    Review review1 = em.persistAndFlush(review()
        .book(book)
        .user(user)
        .rating(5)
        .content("리뷰내용1")
        .build());

    Review review2 = em.persistAndFlush(review()
        .book(book)
        .user(user)
        .rating(4)
        .content("리뷰내용2")
        .build());

    reviewId1 = review1.getId();
    reviewId2 = review2.getId();

    em.persist(PopularReview.builder()
        .reviewId(reviewId1)
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(90))
        .ranking(1)
        .likeCount(10)
        .commentCount(5)
        .calculatedAt(Instant.now())
        .build());

    em.persist(PopularReview.builder()
        .reviewId(reviewId2)
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(80))
        .ranking(2)
        .likeCount(8)
        .commentCount(3)
        .calculatedAt(Instant.now())
        .build());

    em.persist(PopularReview.builder()
        .reviewId(reviewId1)
        .period(Period.WEEKLY)
        .score(BigDecimal.valueOf(95))
        .ranking(1)
        .likeCount(15)
        .commentCount(7)
        .calculatedAt(Instant.now())
        .build());

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("성공: DAILY period 인기 리뷰 rank 오름차순 조회")
  void findPopularReviewsByPeriod_daily_success() {
    List<PopularReviewDto> result = popularReviewRepository
        .findPopularReviewsByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).rank()).isEqualTo(1);
    assertThat(result.get(1).rank()).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: period 필터 - DAILY 조회 시 WEEKLY 데이터 미포함")
  void findPopularReviewsByPeriod_filtersByPeriod() {
    List<PopularReviewDto> result = popularReviewRepository
        .findPopularReviewsByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result).allMatch(dto -> dto.period() == Period.DAILY);
  }

  @Test
  @DisplayName("성공: limit 적용 - 1개만 조회")
  void findPopularReviewsByPeriod_limitApplied() {
    List<PopularReviewDto> result = popularReviewRepository
        .findPopularReviewsByPeriod(Period.DAILY, PageRequest.of(0, 1));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).rank()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 데이터 없는 period 조회 시 빈 리스트 반환")
  void findPopularReviewsByPeriod_emptyResult() {
    List<PopularReviewDto> result = popularReviewRepository
        .findPopularReviewsByPeriod(Period.MONTHLY, PageRequest.of(0, 10));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("성공: Review, Book, User 정보가 함께 조회된다")
  void findPopularReviewsByPeriod_includesJoinedInfo() {
    List<PopularReviewDto> result = popularReviewRepository
        .findPopularReviewsByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result.get(0).userNickname()).isEqualTo("유저1");
    assertThat(result.get(0).bookTitle()).isEqualTo("도서1");
    assertThat(result.get(0).reviewContent()).isEqualTo("리뷰내용1");
  }

  @Test
  @DisplayName("성공: countByPeriod - DAILY 2개 반환")
  void countByPeriod_success() {
    int count = popularReviewRepository.countByPeriod(Period.DAILY);

    assertThat(count).isEqualTo(2);
  }
}