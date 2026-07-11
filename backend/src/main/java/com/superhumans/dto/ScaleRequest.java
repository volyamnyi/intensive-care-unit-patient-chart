package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ScaleRequest {
    private String scaleType;
    private Integer score;
    private String subScoresJson;
    private Integer hour;
}
