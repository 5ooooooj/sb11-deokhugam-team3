package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularReviewRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PopularReviewWriterTest {

  @Mock
  private PopularReviewRankingPersistenceService persistenceService;

  private PopularReviewWriter popularReviewWriter;

  private final LocalDateTime fixedStart = LocalDateTime.of(2026, 6, 10, 0 , 0 , 0);

  private static final double COMMENT_WEIGHT = 0.3;
  private static final double LIKE_WEIGHT = 0.7;

  @BeforeEach
  void setUp() {
    popularReviewWriter = new PopularReviewWriter(Period.DAILY, persistenceService);
    StepExecution stepExecution = mock(StepExecution.class);
    popularReviewWriter.beforeStep(stepExecution);
  }

  @Test
  @DisplayName("성공: write 수행 시 고정된 startTime 기반으로 정확한 Instant 값이 계산되어 저장되는지 검증")
  void write_validItems_savesWithExactCalculatedAt() throws Exception {
    // given
    List<PopularReviewRawData> items = List.of(
        new PopularReviewRawData(UUID.randomUUID(), 3, 5, calculateScore(3, 5))
    );

    Instant fixedInstant = fixedStart.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    ReflectionTestUtils.setField(popularReviewWriter, "calculatedAt", fixedInstant);

    // when - Chunk를 생성하여 배치의 write 로직 실행
    popularReviewWriter.create().write(new Chunk<>(items));

    // then
    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    List<PopularReview> savedReviews = captor.getValue();
    assertThat(savedReviews).isNotEmpty();

    // 단순 null 체크 대신, 설정한 LocalDateTime이 시스템 시간대에 맞춰 정확한 Instant로 변환되었는지 검증
    assertThat(savedReviews.get(0).getCalculatedAt()).isEqualTo(fixedInstant);
  }

  @Test
  @DisplayName("성공: 0건일 때 afterStep에서 deleteAndSave 호출됨")
  void afterStep_noItems_deleteAndSaveCalled() {
    StepExecution stepExecution = mock(StepExecution.class);
    doReturn(0L).when(stepExecution).getReadCount();

    popularReviewWriter.afterStep(stepExecution);

    verify(persistenceService).deleteAndSave(eq(Period.DAILY), eq(List.of()));
  }

  @Test
  @DisplayName("성공: readCount > 0이면 afterStep에서 deleteAndSave 호출 안 됨")
  void afterStep_hasItems_deleteAndSaveNotCalled() {
    StepExecution stepExecution = mock(StepExecution.class);
    doReturn(1L).when(stepExecution).getReadCount();

    popularReviewWriter.afterStep(stepExecution);

    verify(persistenceService, never()).deleteAndSave(any(), any());
  }

  @Test
  @DisplayName("성공: afterStep에 null 전달 시 예외 없이 처리됨")
  void afterStep_null_noException() {
    assertThatNoException().isThrownBy(() -> popularReviewWriter.afterStep(null));
  }

  @Test
  @DisplayName("성공: beforeStep에 null 전달 시 Instant.now()로 설정됨")
  void beforeStep_null_usesInstantNow() throws Exception {
    PopularReviewWriter writer = new PopularReviewWriter(Period.DAILY, persistenceService);
    writer.beforeStep(null);

    // 실행 직전의 시간을 오차 범위를 위해 캡처해 둠
    Instant beforeExecution = Instant.now();
    writer.beforeStep(null);

    List<PopularReviewRawData> items = List.of(
        new PopularReviewRawData(UUID.randomUUID(), 3, 5, calculateScore(3, 5))
    );
    writer.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    Instant actual = captor.getValue().get(0).getCalculatedAt();
    Instant afterExecution = Instant.now();

    assertThat(actual).isNotNull();
    assertThat(actual).isBetween(beforeExecution.minusSeconds(1), afterExecution.plusSeconds(1));
  }

  @Test
  @DisplayName("성공: chunk 아이템이 저장되고 랭킹이 부여됨")
  void write_success() throws Exception {
    List<PopularReviewRawData> items = List.of(
        new PopularReviewRawData(UUID.randomUUID(), 1, 2, BigDecimal.valueOf(1.7)),
        new PopularReviewRawData(UUID.randomUUID(), 3, 5, BigDecimal.valueOf(4.4)),
        new PopularReviewRawData(UUID.randomUUID(), 2, 1, BigDecimal.valueOf(1.3))
    );

    popularReviewWriter.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    assertThat(captor.getValue()).hasSize(3);
    assertThat(captor.getValue()).allMatch(r -> r.getRanking() > 0);
    // score 내림차순 정렬 기준으로 랭킹 검증
    List<PopularReview> saved = captor.getValue();
    assertThat(saved.get(0).getRanking()).isEqualTo(1);
    assertThat(saved.get(1).getRanking()).isEqualTo(2);
    assertThat(saved.get(2).getRanking()).isEqualTo(3);
  }

  @Test
  @DisplayName("성공: 빈 chunk 처리 시 빈 리스트로 저장됨")
  void write_emptyChunk() throws Exception {
    popularReviewWriter.create().write(new Chunk<>(List.of()));

    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }

  private BigDecimal calculateScore(int commentCount, int likeCount) {
    return BigDecimal.valueOf(commentCount * COMMENT_WEIGHT + likeCount * LIKE_WEIGHT);
  }

}