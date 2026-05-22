package com.team3.deokhugam.domain.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

public class BookTest {

    @Test
    @DisplayName("도서 엔티티를 생성하면 필수 정보가 정상적으로 저장됩니다.")
    void createBook() {
        // given
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
                        title,
                        author,
                        description,
                        publisher,
                        publishedDate,
                        isbn,
                        thumbnailUrl
                );

        // then
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
}
