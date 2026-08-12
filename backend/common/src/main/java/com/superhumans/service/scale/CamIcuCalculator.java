package com.superhumans.service.scale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CamIcuCalculator {

    public static CamIcuResult calculate(CamIcuInput input) {
        boolean feature1 = input.isAcuteOnset();
        boolean feature2 = input.isInattention();
        boolean feature3 = input.isDisorganizedThinking();
        boolean feature4 = input.isAlteredConsciousness();

        boolean delirium = feature1 && feature2 && (feature3 || feature4);

        return CamIcuResult.builder()
                .delirium(delirium)
                .feature1(feature1)
                .feature2(feature2)
                .feature3(feature3)
                .feature4(feature4)
                .build();
    }

    @Value
    @Builder
    public static class CamIcuInput {
        boolean acuteOnset;
        boolean inattention;
        boolean disorganizedThinking;
        boolean alteredConsciousness;
    }

    @Value
    @Builder
    public static class CamIcuResult {
        boolean delirium;
        boolean feature1;
        boolean feature2;
        boolean feature3;
        boolean feature4;
    }
}
