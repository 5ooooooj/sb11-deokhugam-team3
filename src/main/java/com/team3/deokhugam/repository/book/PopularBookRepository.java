package com.team3.deokhugam.repository.book;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.PopularBook;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PopularBookRepository extends JpaRepository<PopularBook, UUID> {

  @Modifying
  @Query("DELETE FROM PopularBook p WHERE p.period = :period")
  void deleteByPeriod(Period period);
}
