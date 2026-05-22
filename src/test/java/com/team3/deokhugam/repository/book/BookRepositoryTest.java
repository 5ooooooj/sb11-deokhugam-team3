package com.team3.deokhugam.repository.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.book.Book;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
}
