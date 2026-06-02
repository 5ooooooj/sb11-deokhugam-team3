package com.team3.deokhugam.dto.review;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;


@Component
public class ReviewOrderByConverter implements Converter<String, ReviewOrderBy> {

  @Override
  public ReviewOrderBy convert(String source) {
    return ReviewOrderBy.from(source);
  }
}
