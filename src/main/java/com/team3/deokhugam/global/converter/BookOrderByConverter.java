package com.team3.deokhugam.global.converter;

import com.team3.deokhugam.dto.book.BookOrderBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class BookOrderByConverter implements Converter<String, BookOrderBy> {

  @Override
  public BookOrderBy convert(String source) {
    return BookOrderBy.from(source);
  }
}