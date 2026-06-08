package com.team3.deokhugam.client.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public record OcrResultDto(
    @JsonProperty("ParsedResults")
    List<OcrParseResultDto> parsedResults,

    @JsonProperty("OCRExitCode")
    int ocrExitCode,

    @JsonProperty("IsErroredOnProcessing")
    boolean erroredOnProcessing,

    @JsonProperty("ErrorMessage")
    Object errorMessage,

    @JsonProperty("ErrorDetails")
    String errorDetails,

    @JsonProperty("ProcessingTimeInMilliseconds")
    String processingTimeInMilliseconds
) {

  public boolean hasProcessingError() {
    return erroredOnProcessing || ocrExitCode == 3 || ocrExitCode == 4;
  }

  public String mergedParsedText() {
    if(parsedResults == null || parsedResults.isEmpty()) {
      return "";
    }

    return parsedResults.stream()
        .map(OcrParseResultDto::parsedText)
        .filter(Objects::nonNull)
        .filter(StringUtils::hasText)
        .collect(Collectors.joining("\n"));
  }
}
