package com.razonapro.razonaprobackend.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.razonapro.razonaprobackend.models.Option;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OptionDto {
    private String optionId;
    private String optionText;
    /** Solo se expone al admin o cuando el intento ya terminó */
    private Boolean isCorrect;

    public static OptionDto from(Option o) {
        return OptionDto.builder()
            .optionId(o.getOptionId())
            .optionText(o.getOptionText())
            .isCorrect(o.getIsCorrect())
            .build();
    }

    /** Versión sin revelar la respuesta correcta (durante el test) */
    public static OptionDto fromMasked(Option o) {
        return OptionDto.builder()
            .optionId(o.getOptionId())
            .optionText(o.getOptionText())
            .build();
    }
}
