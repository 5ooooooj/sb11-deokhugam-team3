package com.team3.deokhugam.repository.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.global.config.JpaAuditingConfig;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

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
    Book book =
        new Book(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에 초대된 사람들이 하나씩 죽음을 맞이하는 고전 추리소설",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/and-then-there-were-none.jpg"
        );

    // when
    Book savedBook = bookRepository.save(book);

    // then
    assertThat(bookRepository.findById(savedBook.getId())).isPresent();
  }

  @Test
  @DisplayName("keyword로 title 부분 일치 검색이 동작")
  void searchByKeywordWithTitle() {
    // given
    Book javaBook =
        book(
            "repo-keyword-java-20 실전 자바",
            "강우진자바",
            "자바 검색 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 5, 27),
            "9780000002001", "https://example.com/java.jpg"
        );

    Book springBook =
        book(
            "repo-keyword-spring-20 실전 스프링",
            "강우진스프링",
            "스프링 검색 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2025, 12, 25),
            "9780000002002",
            "https://example.com/spring.jpg"
        );

    bookRepository.saveAll(List.of(javaBook, springBook));

    BookSearchRequest request =
        BookSearchRequest.of(
            "repo-keyword-java-20",
            "title",
            "ASC",
            null,
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
    Book bookC =
        book(
            "repo-title-sort-20 C",
            "작가 C",
            "제목 정렬 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2024, 1, 3),
            "9780000002013",
            "https://example.com/c.jpg"
        );

    Book bookA =
        book(
            "repo-title-sort-20 A",
            "작가 A",
            "제목 정렬 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2024, 1, 1),
            "9780000002011",
            "https://example.com/b.jpg"
        );

    Book bookB =
        book(
            "repo-title-sort-20 B",
            "작가 B",
            "제목 정렬 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2024, 1, 2),
            "9780000002012",
            "https://example.com/b.jpg"
        );

    bookRepository.saveAll(List.of(bookC, bookA, bookB));

    BookSearchRequest request =
        BookSearchRequest.of(
            "repo-title-sort-20",
            "title",
            "ASC",
            null,
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
    Book bookA =
        book(
            "repo-limit-20 A",
            "작가 A",
            "limit 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 1, 1),
            "9780000002021",
            "https://example.com/limit-a.jpg"
        );

    Book bookB =
        book(
            "repo-limit-20 B",
            "작가 B",
            "limit 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 1, 2),
            "9780000002022",
            "https://example.com/limit-b.jpg"
        );

    Book bookC =
        book(
            "repo-limit-20 C",
            "작가 C",
            "limit 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 1, 3),
            "9780000002023",
            "https://example.com/limit-c.jpg"
        );

    bookRepository.saveAll(List.of(bookA, bookB, bookC));

    BookSearchRequest request =
        BookSearchRequest.of(
            "repo-limit-20",
            "title",
            "ASC",
            null,
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
    Book javaBook =
        book(
            "repo-count-java-20 실전 자바",
            "작가 A",
            "count 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 1, 1),
            "9780000002031",
            "https://example.com/count-java.jpg"
        );

    Book javaBasicBook =
        book(
            "repo-count-java-20 자바 입문",
            "작가 B",
            "count 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 1, 2),
            "9780000002032",
            "https://example.com/count-java-basic.jpg"
        );

    Book springBook =
        book(
            "repo-count-spring-20 실전 스프링",
            "작가 C",
            "count 테스트용 도서입니다.",
            "테스트출판사",
            LocalDate.of(2026, 1, 3),
            "9780000002033",
            "https://example.com/count-spring.jpg"
        );

    bookRepository.saveAll(List.of(javaBook, javaBasicBook, springBook));

    BookSearchRequest request =
        BookSearchRequest.of(
            "repo-count-java-20",
            "title",
            "ASC",
            null,
            null,
            10
        );

    // when
    long result = bookRepository.count(request);

    // then
    assertThat(result).isEqualTo(2);
  }

  @Test
  @DisplayName("cursor와 after 이후의 도서 목록을 조회한다")
  void searchWithCursorAndAfter() {
    // given
    Book book1 = book(
        "가나다라",
        "작가1",
        "설명1",
        "출판사1",
        LocalDate.of(2024, 1, 1),
        "1111111111",
        "thumbnail1"
    );

    Book book2 = book(
        "마바사아",
        "작가2",
        "설명2",
        "출판사2",
        LocalDate.of(2024, 1, 2),
        "2222222222",
        "thumbnail2"
    );

    Book book3 = book(
        "자차카타",
        "작가3",
        "설명3",
        "출판사3",
        LocalDate.of(2024, 1, 3),
        "3333333333",
        "thumbnail3"
    );

    bookRepository.saveAll(List.of(book1, book2, book3));
    bookRepository.flush();

    BookSearchRequest firstRequest = BookSearchRequest.of(
        null,
        "title",
        "ASC",
        null,
        null,
        2
    );

    List<Book> firstPage = bookRepository.search(firstRequest);
    Book lastBookOfFirstPage = firstPage.get(firstPage.size() - 1);

    BookSearchRequest secondRequest = BookSearchRequest.of(
        null,
        "title",
        "ASC",
        lastBookOfFirstPage.getTitle(),
        lastBookOfFirstPage.getCreatedAt(),
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
    Book oldBook = book(
        "오래된 책",
        "작가1",
        "설명1",
        "출판사1",
        LocalDate.of(2001, 1, 1),
        "1111111111",
        "thumbnail1"
    );

    Book newBook = book(
        "최신 책",
        "작가2",
        "설명2",
        "출판사2",
        LocalDate.of(2011, 1, 1),
        "2222222222",
        "thumbnail2"
    );

    bookRepository.saveAll(List.of(oldBook, newBook));
    bookRepository.flush();

    BookSearchRequest request = BookSearchRequest.of(
        null,
        "publishedDate",
        "DESC",
        null,
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
    Book lowRatingBook = book(
        "평점 낮은 책",
        "작가1",
        "설명1",
        "출판사1",
        LocalDate.of(2026, 1, 1),
        "1111111111",
        "thumbnail1"
    );

    Book highRatingBook = book(
        "평점 높은 책",
        "작가2",
        "설명2",
        "출판사2",
        LocalDate.of(2026, 1, 2),
        "2222222222",
        "thumbnail2"
    );

    ReflectionTestUtils.setField(lowRatingBook, "rating", new BigDecimal("2.5"));
    ReflectionTestUtils.setField(highRatingBook, "rating", new BigDecimal("4.8"));

    bookRepository.saveAll(List.of(lowRatingBook, highRatingBook));
    bookRepository.flush();

    BookSearchRequest request = BookSearchRequest.of(
        null,
        "rating",
        "DESC",
        null,
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
    Book lowReviewCountBook = book(
        "리뷰 적은 책",
        "작가1",
        "설명1",
        "출판사1",
        LocalDate.of(2026, 1, 1),
        "1111111111",
        "thumbnail1"
    );

    Book highReviewCountBook = book(
        "리뷰 많은 책",
        "작가2",
        "설명2",
        "출판사2",
        LocalDate.of(2026, 1, 2),
        "2222222222",
        "thumbnail2"
    );

    ReflectionTestUtils.setField(lowReviewCountBook, "reviewCount", 1);
    ReflectionTestUtils.setField(highReviewCountBook, "reviewCount", 10);

    bookRepository.saveAll(List.of(lowReviewCountBook, highReviewCountBook));
    bookRepository.flush();

    BookSearchRequest request = BookSearchRequest.of(
        null,
        "reviewCount",
        "DESC",
        null,
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

  private Book book(
      String title,
      String author,
      String description,
      String publisher,
      LocalDate publishedDate,
      String isbn,
      String thumbnailUrl
  ) {
    return new Book(
        title,
        author,
        description,
        publisher,
        publishedDate,
        isbn,
        thumbnailUrl
    );
  }
}
