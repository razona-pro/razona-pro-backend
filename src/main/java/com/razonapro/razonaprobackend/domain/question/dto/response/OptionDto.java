package com.razonapro.razonaprobackend.domain.question.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.razonapro.razonaprobackend.domain.question.model.Option;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionDto {
    private String optionId;
    private String optionText;
    private Boolean isCorrect;

    public static OptionDto from(Option o) {
        return OptionDto.builder()
                .optionId(o.getOptionId())
                .optionText(o.getOptionText())
                .isCorrect(o.getIsCorrect())
                .build();
    }

    public static OptionDto fromMasked(Option o) {
        return OptionDto.builder()
                .optionId(o.getOptionId())
                .optionText(o.getOptionText())
                .build();
    }
}