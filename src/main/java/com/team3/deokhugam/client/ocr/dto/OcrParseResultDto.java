package com.team3.deokhugam.client.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OcrParseResultDto(
    @JsonProperty("FileParseExitCode")
    int fileParseExitCode,

    @JsonProperty("ParsedText")
    String parsedText,

    @JsonProperty("ErrorMessage")
    String errorMessage,

    @JsonProperty("ErrorDetails")
    String errorDetails
) {

}
