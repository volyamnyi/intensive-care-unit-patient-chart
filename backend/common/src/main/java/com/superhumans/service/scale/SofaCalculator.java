package com.superhumans.service.scale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SofaCalculator {

    public static SofaScore calculate(SofaInput input) {
        int respiration = respirationScore(input.getPaO2(), input.getFio2(), input.getOnVentilator());
        int coagulation = coagulationScore(input.getPlatelets());
        int liver = liverScore(input.getBilirubin());
        int cardiovascular = cardiovascularScore(input.getMap(), input.getDopamine(), input.getDobutamine(),
                input.getNorepinephrine(), input.getEpinephrine());
        int cns = cnsScore(input.getGcs());
        int renal = renalScore(input.getCreatinine(), input.getUrineOutput());

        int total = respiration + coagulation + liver + cardiovascular + cns + renal;
        return SofaScore.builder()
                .respiration(respiration)
                .coagulation(coagulation)
                .liver(liver)
                .cardiovascular(cardiovascular)
                .cns(cns)
                .renal(renal)
                .total(total)
                .build();
    }

    private static int respirationScore(Double paO2, Double fio2, Boolean onVentilator) {
        if (paO2 == null || fio2 == null || fio2 == 0) return 0;
        double ratio = paO2 / (fio2 / 100.0);
        if (ratio > 400) return 0;
        if (ratio >= 300) return 1;
        if (ratio >= 200) return 2;
        boolean vent = Boolean.TRUE.equals(onVentilator);
        if (ratio >= 100 && vent) return 3;
        if (ratio < 100 && vent) return 4;
        return 0;
    }

    private static int coagulationScore(Double platelets) {
        if (platelets == null) return 0;
        if (platelets > 150) return 0;
        if (platelets >= 100) return 1;
        if (platelets >= 50) return 2;
        if (platelets >= 20) return 3;
        return 4;
    }

    private static int liverScore(Double bilirubin) {
        if (bilirubin == null) return 0;
        if (bilirubin < 1.2) return 0;
        if (bilirubin <= 1.9) return 1;
        if (bilirubin <= 5.9) return 2;
        if (bilirubin <= 11.9) return 3;
        return 4;
    }

    private static int cardiovascularScore(Double map, Double dopamine, Double dobutamine,
                                           Double norepinephrine, Double epinephrine) {
        if (dopamine != null && dopamine > 15) return 4;
        if (norepinephrine != null && norepinephrine > 0.1) return 4;
        if (epinephrine != null && epinephrine > 0.1) return 4;
        if (dopamine != null && dopamine > 5) return 3;
        if (norepinephrine != null && norepinephrine <= 0.1) return 3;
        if (epinephrine != null && epinephrine <= 0.1) return 3;
        if (dopamine != null && dopamine <= 5) return 2;
        if (dobutamine != null && dobutamine > 0) return 2;
        if (map != null && map < 70) return 1;
        return 0;
    }

    private static int cnsScore(Integer gcs) {
        if (gcs == null) return 0;
        if (gcs == 15) return 0;
        if (gcs >= 13) return 1;
        if (gcs >= 10) return 2;
        if (gcs >= 6) return 3;
        return 4;
    }

    private static int renalScore(Double creatinine, Double urineOutput) {
        if (creatinine == null && urineOutput == null) return 0;
        if (urineOutput != null && urineOutput < 200) return 4;
        if (creatinine != null && creatinine > 5.0) return 4;
        if (urineOutput != null && urineOutput < 500) return 3;
        if (creatinine != null && creatinine >= 3.5) return 3;
        if (creatinine != null && creatinine >= 2.0) return 2;
        if (creatinine != null && creatinine >= 1.2) return 1;
        return 0;
    }

    @Value
    @Builder
    public static class SofaInput {
        Double paO2;
        Double fio2;
        Boolean onVentilator;
        Double platelets;
        Double bilirubin;
        Double map;
        Double dopamine;
        Double dobutamine;
        Double norepinephrine;
        Double epinephrine;
        Integer gcs;
        Double creatinine;
        Double urineOutput;
    }

    @Value
    @Builder
    public static class SofaScore {
        int respiration;
        int coagulation;
        int liver;
        int cardiovascular;
        int cns;
        int renal;
        int total;
    }
}


