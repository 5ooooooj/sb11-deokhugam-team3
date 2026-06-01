package com.team3.deokhugam.repository.book;

import static com.team3.deokhugam.domain.book.BookTestFactory.book;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCursor;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class BookRepositoryTest {

  @Autowired
  private BookRepository bookRepository;

  @Test
  @DisplayName("도서를 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    // given
    Book book = book()
        .title("그리고 아무도 없었다")
        .author("애거서 크리스티")
        .description("외딴 섬에 초대된 사람들이 하나씩 죽음을 맞이하는 고전 추리소설")
        .publisher("황금가지")
        .publishedDate(LocalDate.of(2013, 12, 31))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/and-then-there-were-none.jpg")
        .build();

    // when
    Book savedBook = bookRepository.save(book);

    // then
    assertThat(bookRepository.findById(savedBook.getId())).isPresent();
  }

  @Test
  @DisplayName("keyword로 title 부분 일치 검색이 동작")
  void searchByKeywordWithTitle() {
    // given
    Book javaBook = book()
        .title("repo-keyword-java-20 실전 자바")
        .author("강우진자바")
        .description("자바 검색 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 5, 27))
        .isbn("9780000002001")
        .thumbnailUrl("https://example.com/java.jpg")
        .build();

    Book springBook = book()
        .title("repo-keyword-spring-20 실전 스프링")
        .author("강우진스프링")
        .description("스프링 검색 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2025, 12, 25))
        .isbn("9780000002002")
        .thumbnailUrl("https://example.com/spring.jpg")
        .build();

    bookRepository.saveAll(List.of(javaBook, springBook));

    BookSearchRequest request = BookSearchRequest.of(
        "repo-keyword-java-20",
        "title",
        "ASC",
        null,
        10
    );

    // when
    List<Book> result = bookRepository.search(request);

    // then
    assertThat(result)
        .extracting(Book::getTitle)
        .containsExactly("repo-keyword-java-20 실전 자바");
  }

  @Test
  @DisplayName("orderBy가 title이고 direction이 ASC이면 제목 오름차순으로 정렬")
  void searchOrderByTitleAsc() {
    // given
    Book bookC = book()
        .title("repo-title-sort-20 C")
        .author("작가 C")
        .description("제목 정렬 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2024, 1, 3))
        .isbn("9780000002013")
        .thumbnailUrl("https://example.com/c.jpg")
        .build();

    Book bookA = book()
        .title("repo-title-sort-20 A")
        .author("작가 A")
        .description("제목 정렬 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2024, 1, 1))
        .isbn("9780000002011")
        .thumbnailUrl("https://example.com/a.jpg")
        .build();

    Book bookB = book()
        .title("repo-title-sort-20 B")
        .author("작가 B")
        .description("제목 정렬 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2024, 1, 2))
        .isbn("9780000002012")
        .thumbnailUrl("https://example.com/b.jpg")
        .build();

    bookRepository.saveAll(List.of(bookC, bookA, bookB));

    BookSearchRequest request = BookSearchRequest.of(
        "repo-title-sort-20",
        "title",
        "ASC",
        null,
        10
    );

    // when
    List<Book> result = bookRepository.search(request);

    // then
    assertThat(result)
        .extracting(Book::getTitle)
        .containsExactly(
            "repo-title-sort-20 A",
            "repo-title-sort-20 B",
            "repo-title-sort-20 C"
        );
  }

  @Test
  @DisplayName("limit 조건이 적용")
  void searchWithLimit() {
    // given
    Book bookA = book()
        .title("repo-limit-20 A")
        .author("작가 A")
        .description("limit 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000002021")
        .thumbnailUrl("https://example.com/limit-a.jpg")
        .build();

    Book bookB = book()
        .title("repo-limit-20 B")
        .author("작가 B")
        .description("limit 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 2))
        .isbn("9780000002022")
        .thumbnailUrl("https://example.com/limit-b.jpg")
        .build();

    Book bookC = book()
        .title("repo-limit-20 C")
        .author("작가 C")
        .description("limit 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 3))
        .isbn("9780000002023")
        .thumbnailUrl("https://example.com/limit-c.jpg")
        .build();

    bookRepository.saveAll(List.of(bookA, bookB, bookC));

    BookSearchRequest request = BookSearchRequest.of(
        "repo-limit-20",
        "title",
        "ASC",
        null,
        2
    );

    // when
    List<Book> result = bookRepository.search(request);

    // then
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(Book::getTitle)
        .containsExactly(
            "repo-limit-20 A",
            "repo-limit-20 B"
        );
  }

  @Test
  @DisplayName("검색 조건에 맞는 도서 전체 개수를 조회")
  void countBySearchCondition() {
    // given
    Book javaBook = book()
        .title("repo-count-java-20 실전 자바")
        .author("작가 A")
        .description("count 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000002031")
        .thumbnailUrl("https://example.com/count-java.jpg")
        .build();

    Book javaBasicBook = book()
        .title("repo-count-java-20 자바 입문")
        .author("작가 B")
        .description("count 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 2))
        .isbn("9780000002032")
        .thumbnailUrl("https://example.com/count-java-basic.jpg")
        .build();

    Book springBook = book()
        .title("repo-count-spring-20 실전 스프링")
        .author("작가 C")
        .description("count 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 3))
        .isbn("9780000002033")
        .thumbnailUrl("https://example.com/count-spring.jpg")
        .build();

    bookRepository.saveAll(List.of(javaBook, javaBasicBook, springBook));

    BookSearchRequest request = BookSearchRequest.of(
        "repo-count-java-20",
        "title",
        "ASC",
        null,
        10
    );

    // when
    long result = bookRepository.count(request);

    // then
    assertThat(result).isEqualTo(2);
  }

  @Test
  @DisplayName("count는 cursor 이후 개수가 아니라 검색 조건에 맞는 전체 개수를 조회")
  void countDoesNotApplyCursorCondition() {
    // given
    Book firstBook = book()
        .title("repo-count-cursor-20 A")
        .author("작가 A")
        .description("count cursor 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000002041")
        .thumbnailUrl("https://example.com/count-cursor-a.jpg")
        .build();

    Book secondBook = book()
        .title("repo-count-cursor-20 B")
        .author("작가 B")
        .description("count cursor 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 2))
        .isbn("9780000002042")
        .thumbnailUrl("https://example.com/count-cursor-b.jpg")
        .build();

    Book thirdBook = book()
        .title("repo-count-cursor-20 C")
        .author("작가 C")
        .description("count cursor 테스트용 도서입니다.")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 3))
        .isbn("9780000002043")
        .thumbnailUrl("https://example.com/count-cursor-c.jpg")
        .build();

    bookRepository.saveAll(List.of(firstBook, secondBook, thirdBook));
    bookRepository.flush();

    BookSearchRequest firstPageRequest = BookSearchRequest.of(
        "repo-count-cursor-20",
        "title",
        "ASC",
        null,
        2
    );

    List<Book> firstPage = bookRepository.search(firstPageRequest);
    Book lastBookOfFirstPage = firstPage.get(firstPage.size() - 1);

    String cursor = BookCursor.encode(
        lastBookOfFirstPage.getTitle(),
        lastBookOfFirstPage.getCreatedAt(),
        lastBookOfFirstPage.getId()
    );

    BookSearchRequest secondPageRequest = BookSearchRequest.of(
        "repo-count-cursor-20",
        "title",
        "ASC",
        cursor,
        2
    );

    // when
    long result = bookRepository.count(secondPageRequest);

    // then
    assertThat(result).isEqualTo(3);
  }

  @Test
  @DisplayName("cursor token 이후의 도서 목록을 조회한다")
  void searchWithCursorToken() {
    // given
    Book book1 = book()
        .title("가나다라")
        .author("작가1")
        .description("설명1")
        .publisher("출판사1")
        .publishedDate(LocalDate.of(2024, 1, 1))
        .isbn("1111111111")
        .thumbnailUrl("thumbnail1")
        .build();

    Book book2 = book()
        .title("마바사아")
        .author("작가2")
        .description("설명2")
        .publisher("출판사2")
        .publishedDate(LocalDate.of(2024, 1, 2))
        .isbn("2222222222")
        .thumbnailUrl("thumbnail2")
        .build();

    Book book3 = book()
        .title("자차카타")
        .author("작가3")
        .description("설명3")
        .publisher("출판사3")
        .publishedDate(LocalDate.of(2024, 1, 3))
        .isbn("3333333333")
        .thumbnailUrl("thumbnail3")
        .build();

    bookRepository.saveAll(List.of(book1, book2, book3));
    bookRepository.flush();

    BookSearchRequest firstRequest = BookSearchRequest.of(
        null,
        "title",
        "ASC",
        null,
        2
    );

    List<Book> firstPage = bookRepository.search(firstRequest);
    Book lastBookOfFirstPage = firstPage.get(firstPage.size() - 1);

    String cursor = BookCursor.encode(
        lastBookOfFirstPage.getTitle(),
        lastBookOfFirstPage.getCreatedAt(),
        lastBookOfFirstPage.getId()
    );

    BookSearchRequest secondRequest = BookSearchRequest.of(
        null,
        "title",
        "ASC",
        cursor,
        2
    );

    // when
    List<Book> result = bookRepository.search(secondRequest);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("자차카타");
  }

  @Test
  @DisplayName("출간일 기준 내림차순으로 도서 목록을 조회")
  void searchOrderByPublishedDateDesc() {
    // given
    Book oldBook = book()
        .title("오래된 책")
        .author("작가1")
        .description("설명1")
        .publisher("출판사1")
        .publishedDate(LocalDate.of(2001, 1, 1))
        .isbn("1111111111")
        .thumbnailUrl("thumbnail1")
        .build();

    Book newBook = book()
        .title("최신 책")
        .author("작가2")
        .description("설명2")
        .publisher("출판사2")
        .publishedDate(LocalDate.of(2011, 1, 1))
        .isbn("2222222222")
        .thumbnailUrl("thumbnail2")
        .build();

    bookRepository.saveAll(List.of(oldBook, newBook));
    bookRepository.flush();

    BookSearchRequest request = BookSearchRequest.of(
        null,
        "publishedDate",
        "DESC",
        null,
        10
    );

    // when
    List<Book> result = bookRepository.search(request);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("최신 책");
    assertThat(result.get(1).getTitle()).isEqualTo("오래된 책");
  }

  @Test
  @DisplayName("평점 기준 내림차순으로 도서 목록을 조회")
  void searchOrderByRatingDesc() {
    // given
    Book lowRatingBook = book()
        .title("평점 낮은 책")
        .author("작가1")
        .description("설명1")
        .publisher("출판사1")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("1111111111")
        .thumbnailUrl("thumbnail1")
        .rating(new BigDecimal("2.5"))
        .build();

    Book highRatingBook = book()
        .title("평점 높은 책")
        .author("작가2")
        .description("설명2")
        .publisher("출판사2")
        .publishedDate(LocalDate.of(2026, 1, 2))
        .isbn("2222222222")
        .thumbnailUrl("thumbnail2")
        .rating(new BigDecimal("4.8"))
        .build();

    bookRepository.saveAll(List.of(lowRatingBook, highRatingBook));
    bookRepository.flush();

    BookSearchRequest request = BookSearchRequest.of(
        null,
        "rating",
        "DESC",
        null,
        10
    );

    // when
    List<Book> result = bookRepository.search(request);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("평점 높은 책");
    assertThat(result.get(1).getTitle()).isEqualTo("평점 낮은 책");
  }

  @Test
  @DisplayName("리뷰 수 기준 내림차순으로 도서 목록을 조회한다")
  void searchOrderByReviewCountDesc() {
    // given
    Book lowReviewCountBook = book()
        .title("리뷰 적은 책")
        .author("작가1")
        .description("설명1")
        .publisher("출판사1")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("1111111111")
        .thumbnailUrl("thumbnail1")
        .reviewCount(1)
        .build();

    Book highReviewCountBook = book()
        .title("리뷰 많은 책")
        .author("작가2")
        .description("설명2")
        .publisher("출판사2")
        .publishedDate(LocalDate.of(2026, 1, 2))
        .isbn("2222222222")
        .thumbnailUrl("thumbnail2")
        .reviewCount(10)
        .build();

    bookRepository.saveAll(List.of(lowReviewCountBook, highReviewCountBook));
    bookRepository.flush();

    BookSearchRequest request = BookSearchRequest.of(
        null,
        "reviewCount",
        "DESC",
        null,
        10
    );

    // when
    List<Book> result = bookRepository.search(request);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTitle()).isEqualTo("리뷰 많은 책");
    assertThat(result.get(1).getTitle()).isEqualTo("리뷰 적은 책");
  }
}