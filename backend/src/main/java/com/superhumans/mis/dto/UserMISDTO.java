package com.superhumans.mis.dto;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserMisDTO {
    private UUID id;
    private String login;
    private String fullName;
    private String shortName;
    private String specialityCode;
    private String specialityName;
    private String email;
    private String phone;
}
