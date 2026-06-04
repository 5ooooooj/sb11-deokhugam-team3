package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Getter
@Component
@NoArgsConstructor
public class PopularBookWriter implements StepExecutionListener {

  private StepExecution stepExecution;
  private final List<PopularBook> accumulated = new ArrayList<>();

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    this.stepExecution = stepExecution;
    accumulated.clear();
  }

  public ItemWriter<PopularBook> create() {
    return chunk -> accumulated.addAll(chunk.getItems());
  }
}
