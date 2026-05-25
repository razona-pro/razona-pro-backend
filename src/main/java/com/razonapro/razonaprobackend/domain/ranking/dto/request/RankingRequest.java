package com.razonapro.razonaprobackend.domain.ranking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RankingRequest {
    @NotBlank @Size(max = 20)  private String rankingName;
    @Size(max = 100)           private String description;

    @NotBlank
    @Pattern(regexp = "^(WEEKLY|MONTHLY|GENERAL)$")
    private String periodType;

    @NotBlank
    @Pattern(regexp = "^(ALL|TRIEDS|AI_TRIEDS)$")
    private String sourceFilter;
}
