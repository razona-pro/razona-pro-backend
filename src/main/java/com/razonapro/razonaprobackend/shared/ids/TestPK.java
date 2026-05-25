package com.razonapro.razonaprobackend.shared.ids;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class TestPK implements Serializable {
    private String testId;
    private String competenceId;
}