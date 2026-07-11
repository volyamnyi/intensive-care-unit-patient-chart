package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentDTO {
    private Integer documentID;
    private LocalDateTime documentCreationDate;
    private String documentUserLogin;
    private String documentTemplateID;
    private String documentTemplateName;
    private String documentKindCode;
    private String documentKindName;
    private String documentApproveStatusCode;
    private String documentApproveStatusName;
    private String documentUrl;
}
