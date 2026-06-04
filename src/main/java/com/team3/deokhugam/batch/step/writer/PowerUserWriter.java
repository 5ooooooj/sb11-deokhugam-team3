package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import com.team3.deokhugam.repository.dashboard.PowerUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
public class PowerUserWriter {

  private final PowerUserRepository powerUserRepository;

  public ItemWriter<PowerUser> create(Period period) {
    return chunk -> powerUserRepository.saveAll(chunk.getItems());
  }

}
