package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.Nullable;

@Getter
@NoArgsConstructor
public class PopularReviewWriter implements StepExecutionListener {

  private final List<PopularReview> accumulated = new ArrayList<>();

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    accumulated.clear();
  }

  public ItemWriter<PopularReview> create() {
    return chunk -> accumulated.addAll(chunk.getItems());
  }
}
