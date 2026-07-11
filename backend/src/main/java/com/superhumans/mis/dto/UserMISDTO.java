package com.superhumans.mis.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserMISDTO {
    private String userLogin;
    private String userShortName;
    private String userName;
    private String userSpecialityCode;
    private String userSpecialityName;
    private String userEmail;
    private String userPhone;
}
