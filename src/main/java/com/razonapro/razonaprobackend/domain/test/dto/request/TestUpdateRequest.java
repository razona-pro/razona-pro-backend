package com.razonapro.razonaprobackend.domain.test.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TestUpdateRequest {
    @Size(max = 50)  private String  testName;
    @Size(max = 100) private String  description;
    @Min(60)         private Integer durationSeconds;
    @Min(1)          private Integer questionsToPresent;
    @Pattern(regexp = "^(PRACTICE|EXAM|TIMED)$") private String testMode;
    private Boolean isActive;
}