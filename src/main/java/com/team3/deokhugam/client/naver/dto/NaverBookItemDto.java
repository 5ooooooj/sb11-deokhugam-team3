package com.team3.deokhugam.client.naver.dto;

public record NaverBookItemDto(
    String title,
    String link,
    String image,
    String author,
    String discount,
    String publisher,
    String isbn,
    String description,
    String pubdate
) {

}
