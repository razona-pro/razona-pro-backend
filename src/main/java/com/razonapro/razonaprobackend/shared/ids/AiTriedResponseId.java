package com.razonapro.razonaprobackend.shared.ids;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class AiTriedResponseId implements Serializable {
    private String programId;
    private String studentId;
    private String aiTriedId;
    private String aiTriedResponseId;
}
