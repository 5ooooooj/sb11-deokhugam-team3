package com.team3.deokhugam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.book.BookForbiddenException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BookTest {

    @Test
    @DisplayName("도서 엔티티를 생성하면 필수 정보가 정상적으로 저장됩니다.")
    void createBook() {
        // given
        UUID userId = UUID.randomUUID();
        String title = "그리고 아무도 없었다";
        String author = "애거서 크리스티";
        String description = "외딴 섬에 초대된 사람들이 하나씩 죽음을 맞이하는 고전 추리소설";
        String publisher = "황금가지";
        LocalDate publishedDate = LocalDate.of(2013, 12, 31);
        String isbn = "9788960177758";
        String thumbnailUrl = "https://example.com/and-then-there-were-none.jpg";

        // when
        Book book =
            new Book(
                userId,
                title,
                author,
                description,
                publisher,
                publishedDate,
                isbn,
                thumbnailUrl
            );

        // then
        assertThat(book.getUserId()).isEqualTo(userId);
        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getAuthor()).isEqualTo(author);
        assertThat(book.getDescription()).isEqualTo(description);
        assertThat(book.getPublisher()).isEqualTo(publisher);
        assertThat(book.getPublishedDate()).isEqualTo(publishedDate);
        assertThat(book.getIsbn()).isEqualTo(isbn);
        assertThat(book.getThumbnailUrl()).isEqualTo(thumbnailUrl);
        assertThat(book.getReviewCount()).isZero();
        assertThat(book.getRating()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("등록자 본인이면 도서 소유자 검증을 통과합니다.")
    void validateOwnerWithOwner() {
        // given
        UUID userId = UUID.randomUUID();

        Book book =
            new Book(
                userId,
                "그리고 아무도 없었다",
                "애거서 크리스티",
                "외딴 섬에 초대된 사람들이 하나씩 죽음을 맞이하는 고전 추리소설",
                "황금가지",
                LocalDate.of(2013, 12, 31),
                "9788960177758",
                "https://example.com/and-then-there-were-none.jpg"
            );

        // when
        book.validateOwner(userId);

        // then
        assertThat(book.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("등록자가 아니면 도서 소유자 검증에서 예외가 발생합니다.")
    void validateOwnerWithForbiddenUser() {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID requestUserId = UUID.randomUUID();

        Book book =
            new Book(
                ownerId,
                "그리고 아무도 없었다",
                "애거서 크리스티",
                "외딴 섬에 초대된 사람들이 하나씩 죽음을 맞이하는 고전 추리소설",
                "황금가지",
                LocalDate.of(2013, 12, 31),
                "9788960177758",
                "https://example.com/and-then-there-were-none.jpg"
            );

        // when, then
        assertThatThrownBy(() -> book.validateOwner(requestUserId))
            .isInstanceOf(BookForbiddenException.class);
    }
}