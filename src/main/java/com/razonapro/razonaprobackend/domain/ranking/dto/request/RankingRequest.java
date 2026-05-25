package com.razonapro.razonaprobackend.domain.ranking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RankingRequest {

    @NotBlank @Size(max = 20)
    private String rankingName;

    @Size(max = 100)
    private String description;

    @NotBlank
    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|GENERAL)$")
    private String periodType;

    @NotBlank
    @Pattern(regexp = "^(ALL|TRIEDS|AI_TRIEDS)$")
    private String sourceFilter;
}