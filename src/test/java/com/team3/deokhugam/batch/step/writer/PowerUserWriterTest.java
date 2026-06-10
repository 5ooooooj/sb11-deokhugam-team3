package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@ExtendWith(MockitoExtension.class)
class PowerUserWriterTest {

  private PowerUserWriter powerUserWriter;

  @BeforeEach
  void setUp() {
    powerUserWriter = new PowerUserWriter();
    powerUserWriter.beforeStep(mock(StepExecution.class));
  }

  @Test
  @DisplayName("성공: chunk 아이템을 누적")
  void write_success() throws Exception {
    List<PowerUser> items = List.of(
        createPowerUser(UUID.randomUUID(), BigDecimal.valueOf(3.0)),
        createPowerUser(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPowerUser(UUID.randomUUID(), BigDecimal.valueOf(1.0))
    );
    Chunk<PowerUser> chunk = new Chunk<>(items);
    ItemWriter<PowerUser> writer = powerUserWriter.create();

    writer.write(chunk);

    assertThat(powerUserWriter.getAccumulated()).containsExactlyElementsOf(items);
  }

  @Test
  @DisplayName("성공: 빈 chunk를 처리")
  void write_emptyChunk() throws Exception {
    Chunk<PowerUser> chunk = new Chunk<>(List.of());

    powerUserWriter.create().write(chunk);

    assertThat(powerUserWriter.getAccumulated()).isEmpty();
  }

  @Test
  @DisplayName("성공: 여러 chunk가 누적됨")
  void write_multipleChunks() throws Exception {
    PowerUser item1 = createPowerUser(UUID.randomUUID(), BigDecimal.valueOf(3.0));
    PowerUser item2 = createPowerUser(UUID.randomUUID(), BigDecimal.valueOf(5.0));
    ItemWriter<PowerUser> writer = powerUserWriter.create();

    writer.write(new Chunk<>(List.of(item1)));
    writer.write(new Chunk<>(List.of(item2)));

    assertThat(powerUserWriter.getAccumulated()).containsExactly(item1, item2);
  }

  private PowerUser createPowerUser(UUID userId, BigDecimal score) {
    return PowerUser.builder()
        .userId(userId)
        .period(Period.DAILY)
        .score(score)
        .reviewScoreSum(BigDecimal.ZERO)
        .likeCount(0)
        .commentCount(0)
        .calculatedAt(Instant.now())
        .build();
  }
}