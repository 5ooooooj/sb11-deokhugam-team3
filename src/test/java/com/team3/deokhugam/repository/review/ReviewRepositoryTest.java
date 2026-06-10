package com.team3.deokhugam.repository.review;

import static com.team3.deokhugam.domain.review.ReviewTestFactory.review;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.review.ReviewOrderBy;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
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

  private User persistUser() {
    return entityManager.persist(
        new User("review-repo-" + UUID.randomUUID() + "@test.com", "리뷰테스터", "Password1!"));
  }

  private Book persistBook() {
    return entityManager.persist(new Book(
        UUID.randomUUID(), "테스트 도서", "테스트 저자", "테스트 설명",
        "테스트 출판사", LocalDate.of(2026, 1, 1), null, null));
  }

  @Test
  @DisplayName("리뷰를 저장하고 ID로 다시 조회할 수 있다")
  void saveAndFind() {
    Review review = review().user(persistUser()).book(persistBook())
        .rating(5).content("좋은 책이에요").build();

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
    Review review = review().user(persistUser()).book(persistBook())
        .rating(4).content("삭제될 리뷰").build();
    Review saved = reviewRepository.save(review);

    reviewRepository.delete(saved);

    assertThat(reviewRepository.findById(saved.getId())).isEmpty();
  }

  @Test
  @DisplayName("userId로 필터링 검색이 동작한다")
  void searchByUserId() {
    User targetUser = persistUser();
    User otherUser = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(targetUser).book(book).content("내 리뷰 1").build(),
        review().user(targetUser).book(book).content("내 리뷰 2").build(),
        review().user(otherUser).book(book).content("남의 리뷰").build()
    ));

    UUID targetUserId = targetUser.getId();
    ReviewSearchRequest request = ReviewSearchRequest.of(
        targetUserId, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(r -> r.getUserId().equals(targetUserId));
  }

  @Test
  @DisplayName("bookId로 필터링 검색이 동작한다")
  void searchByBookId() {
    User user = persistUser();
    Book targetBook = persistBook();
    Book otherBook = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(targetBook).content("이 책 리뷰 1").build(),
        review().user(user).book(otherBook).content("다른 책 리뷰").build()
    ));

    UUID targetBookId = targetBook.getId();
    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, targetBookId, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBookId()).isEqualTo(targetBookId);
  }

  @Test
  @DisplayName("keyword로 content 부분 일치 검색이 동작한다")
  void searchByKeyword() {
    User user = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).content("repo-keyword-스프링 정말 재밌어요").build(),
        review().user(user).book(book).content("repo-keyword-자바 어렵네요").build(),
        review().user(user).book(book).content("이 책 너무 별로").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-keyword-스프링",
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).contains("repo-keyword-스프링");
  }

  @Test
  @DisplayName("rating 기준 내림차순 정렬이 동작한다")
  void searchOrderByRatingDesc() {
    User user = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).rating(2).content("repo-rating-sort 낮음").build(),
        review().user(user).book(book).rating(5).content("repo-rating-sort 높음").build(),
        review().user(user).book(book).rating(3).content("repo-rating-sort 중간").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-rating-sort",
        ReviewOrderBy.RATING, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(3);
    assertThat(result).extracting(Review::getRating).containsExactly(5, 3, 2);
  }

  @Test
  @DisplayName("limit 조건이 적용된다")
  void searchWithLimit() {
    User user = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).content("repo-limit A").build(),
        review().user(user).book(book).content("repo-limit B").build(),
        review().user(user).book(book).content("repo-limit C").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-limit",
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 2, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("논리 삭제된 리뷰는 검색에서 제외된다")
  void searchExcludesDeleted() {
    User user = persistUser();
    Book book = persistBook();

    Review review1 = review().user(user).book(book).content("repo-deleted 살아있음").build();
    Review review2 = review().user(user).book(book).content("repo-deleted 삭제됨").build();
    reviewRepository.saveAll(List.of(review1, review2));

    review2.softDelete();
    reviewRepository.save(review2);

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-deleted",
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).contains("살아있음");
  }

  @Test
  @DisplayName("검색 조건에 맞는 리뷰 전체 개수를 조회한다")
  void countBySearchCondition() {
    User targetUser = persistUser();
    User otherUser = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(targetUser).book(book).content("count A").build(),
        review().user(targetUser).book(book).content("count B").build(),
        review().user(otherUser).book(book).content("count C").build()
    ));

    UUID targetUserId = targetUser.getId();
    ReviewSearchRequest request = ReviewSearchRequest.of(
        targetUserId, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    long result = reviewRepository.count(request);

    assertThat(result).isEqualTo(2);
  }

  @Test
  @DisplayName("커서 페이지네이션 - createdAt 커서가 디코드되어 필터가 동작한다")
  void searchWithCreatedAtCursor() {
    User user = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).content("cursor-asc A").build(),
        review().user(user).book(book).content("cursor-asc B").build()
    ));

    String raw = Instant.now().plusSeconds(60) + "|" + UUID.randomUUID();
    String cursor = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "cursor-asc",
        ReviewOrderBy.CREATED_AT, Sort.Direction.ASC,
        cursor, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("커서 페이지네이션 - RATING 3-part 커서가 디코드되어 필터가 동작한다")
  void searchWithRatingCursor() {
    User user = persistUser();
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).rating(5).content("rating-cursor A").build(),
        review().user(user).book(book).rating(3).content("rating-cursor B").build()
    ));

    String raw = 6 + "|" + Instant.now() + "|" + UUID.randomUUID();
    String cursor = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "rating-cursor",
        ReviewOrderBy.RATING, Sort.Direction.ASC,
        cursor, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("커서 페이지네이션 - 잘못된 커서(Base64 형식 아님)는 INVALID_INPUT 예외")
  void searchWithInvalidCursor() {
    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        "!@#$ not-base64 !@#$", 10, UUID.randomUUID()
    );

    assertThatThrownBy(() -> reviewRepository.search(request))
        .isInstanceOf(DeokhugamException.class);
  }

  @Test
  @DisplayName("커서 페이지네이션 - RATING 커서가 part 개수 부족이면 INVALID_INPUT 예외")
  void searchWithMalformedRatingCursor() {
    String raw = Instant.now() + "|" + UUID.randomUUID();
    String cursor = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, null,
        ReviewOrderBy.RATING, Sort.Direction.DESC,
        cursor, 10, UUID.randomUUID()
    );

    assertThatThrownBy(() -> reviewRepository.search(request))
        .isInstanceOf(DeokhugamException.class);
  }

  @Test
  @DisplayName("keyword로 작성자 닉네임 부분 일치 검색이 동작한다")
  void searchByKeyword_nickname() {
    User user = entityManager.persist(
        new User("nick-search-" + UUID.randomUUID() + "@test.com", "repo-닉네임검색대상", "Password1!"));
    Book book = persistBook();

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).content("아무 내용").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-닉네임검색대상",
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("keyword로 도서 제목 부분 일치 검색이 동작한다")
  void searchByKeyword_bookTitle() {
    User user = persistUser();
    Book book = entityManager.persist(new Book(
        UUID.randomUUID(), "repo-도서제목검색대상", "테스트 저자", "테스트 설명",
        "테스트 출판사", LocalDate.of(2026, 1, 1), null, null));

    reviewRepository.saveAll(List.of(
        review().user(user).book(book).content("아무 내용").build()
    ));

    ReviewSearchRequest request = ReviewSearchRequest.of(
        null, null, "repo-도서제목검색대상",
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, UUID.randomUUID()
    );

    List<Review> result = reviewRepository.search(request);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("likeCount 증가 시 updatedAt이 변경되지 않는다")
  void incrementLikeCount_doesNotChangeUpdatedAt() {
    Review review = review().user(persistUser()).book(persistBook()).build();
    Review saved = reviewRepository.save(review);
    entityManager.flush();
    entityManager.clear();

    Instant before = reviewRepository.findById(saved.getId()).orElseThrow().getUpdatedAt();
    entityManager.clear();

    reviewRepository.incrementLikeCount(saved.getId());
    entityManager.flush();
    entityManager.clear();

    Review found = reviewRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getLikeCount()).isEqualTo(1);
    assertThat(found.getUpdatedAt()).isEqualTo(before);
  }
}