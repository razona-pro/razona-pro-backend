package com.razonapro.razonaprobackend.models.ids;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class AiTriedId implements Serializable {
    private String programId;
    private String studentId;
    private String aiTriedId;
}
