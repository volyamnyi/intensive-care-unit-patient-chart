package com.superhumans.service.scale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BradenCalculator {

    public static BradenResult calculate(BradenInput input) {
        int total = input.getSensoryPerception() + input.getMoisture()
                + input.getActivity() + input.getMobility()
                + input.getNutrition() + input.getFrictionShear();

        String riskCategory;
        if (total >= 19) riskCategory = "Low";
        else if (total >= 15) riskCategory = "Mild";
        else if (total >= 13) riskCategory = "Moderate";
        else if (total >= 10) riskCategory = "High";
        else riskCategory = "VeryHigh";

        return BradenResult.builder()
                .sensoryPerception(input.getSensoryPerception())
                .moisture(input.getMoisture())
                .activity(input.getActivity())
                .mobility(input.getMobility())
                .nutrition(input.getNutrition())
                .frictionShear(input.getFrictionShear())
                .total(total)
                .riskCategory(riskCategory)
                .build();
    }

    @Value
    @Builder
    public static class BradenInput {
        int sensoryPerception;
        int moisture;
        int activity;
        int mobility;
        int nutrition;
        int frictionShear;
    }

    @Value
    @Builder
    public static class BradenResult {
        int sensoryPerception;
        int moisture;
        int activity;
        int mobility;
        int nutrition;
        int frictionShear;
        int total;
        String riskCategory;
    }
}
