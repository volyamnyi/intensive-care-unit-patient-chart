package com.superhumans.service.scale;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApacheIiCalculator {

    public static ApacheIiScore calculate(ApacheIiInput input) {
        int aps = 0;
        aps += temperaturePoints(input.getTemperatureC());
        aps += mapPoints(input.getMeanArterialPressure());
        aps += heartRatePoints(input.getHeartRate());
        aps += respiratoryRatePoints(input.getRespiratoryRate());
        aps += oxygenationPoints(input.getFio2(), input.getPaO2(), input.getPaCO2(), input.getAaDo2());
        aps += phPoints(input.getPh(), input.getSerumHco3());
        aps += sodiumPoints(input.getSerumSodium());
        aps += potassiumPoints(input.getSerumPotassium());
        aps += creatininePoints(input.getSerumCreatinine(), input.getAcuteRenalFailure());
        aps += hematocritPoints(input.getHematocrit());
        aps += wbcPoints(input.getWhiteBloodCount());
        aps += gcsPoints(input.getGcs());

        int agePoints = ageScore(input.getAge());
        int chronicPoints = chronicHealthPoints(input.getChronicHealthType(), input.getIsEmergencySurgical());

        int total = aps + agePoints + chronicPoints;
        return ApacheIiScore.builder()
                .aps(aps)
                .agePoints(agePoints)
                .chronicPoints(chronicPoints)
                .total(total)
                .build();
    }

    private static int temperaturePoints(Double t) {
        if (t == null) return 0;
        if (t >= 41.0) return 4;
        if (t >= 39.0) return 3;
        if (t >= 38.5) return 1;
        if (t >= 36.0) return 0;
        if (t >= 34.0) return 1;
        if (t >= 32.0) return 2;
        if (t >= 30.0) return 3;
        return 4;
    }

    private static int mapPoints(Double map) {
        if (map == null) return 0;
        if (map >= 160) return 4;
        if (map >= 130) return 3;
        if (map >= 110) return 2;
        if (map >= 70) return 0;
        if (map >= 50) return 2;
        return 4;
    }

    private static int heartRatePoints(Double hr) {
        if (hr == null) return 0;
        if (hr >= 180) return 4;
        if (hr >= 140) return 3;
        if (hr >= 110) return 2;
        if (hr >= 70) return 0;
        if (hr >= 55) return 2;
        if (hr >= 40) return 3;
        return 4;
    }

    private static int respiratoryRatePoints(Double rr) {
        if (rr == null) return 0;
        if (rr >= 50) return 4;
        if (rr >= 35) return 3;
        if (rr >= 25) return 1;
        if (rr >= 12) return 0;
        if (rr >= 10) return 1;
        if (rr >= 6) return 2;
        return 4;
    }

    private static int oxygenationPoints(Double fio2, Double paO2, Double paCO2, Double aaDo2) {
        if (fio2 == null || paO2 == null) return 0;
        if (fio2 >= 0.5) {
            double aad = aaDo2 != null ? aaDo2 : (713 * fio2) - (paCO2 != null ? paCO2 / 0.8 : 0) - paO2;
            if (aad >= 500) return 4;
            if (aad >= 350) return 3;
            if (aad >= 200) return 2;
            return 0;
        } else {
            if (paO2 >= 70) return 0;
            if (paO2 >= 61) return 1;
            if (paO2 >= 55) return 2;
            if (paO2 >= 50) return 3;
            return 4;
        }
    }

    private static int phPoints(Double ph, Double hco3) {
        if (ph != null) {
            if (ph >= 7.7) return 4;
            if (ph >= 7.6) return 3;
            if (ph >= 7.5) return 1;
            if (ph >= 7.33) return 0;
            if (ph >= 7.25) return 2;
            if (ph >= 7.15) return 3;
            return 4;
        }
        if (hco3 != null) {
            if (hco3 >= 52) return 4;
            if (hco3 >= 41) return 3;
            if (hco3 >= 32) return 1;
            if (hco3 >= 22) return 0;
            if (hco3 >= 18) return 1;
            if (hco3 >= 15) return 2;
            return 4;
        }
        return 0;
    }

    private static int sodiumPoints(Double na) {
        if (na == null) return 0;
        if (na >= 180) return 4;
        if (na >= 160) return 3;
        if (na >= 155) return 2;
        if (na >= 150) return 1;
        if (na >= 130) return 0;
        if (na >= 120) return 2;
        if (na >= 111) return 3;
        return 4;
    }

    private static int potassiumPoints(Double k) {
        if (k == null) return 0;
        if (k >= 7.0) return 4;
        if (k >= 6.0) return 3;
        if (k >= 5.5) return 1;
        if (k >= 3.5) return 0;
        if (k >= 3.0) return 1;
        if (k >= 2.5) return 2;
        return 4;
    }

    private static int creatininePoints(Double cr, Boolean acuteRenalFailure) {
        if (cr == null) return 0;
        boolean acute = Boolean.TRUE.equals(acuteRenalFailure);
        if (cr >= 3.5) return acute ? 8 : 4;
        if (cr >= 2.0) return acute ? 6 : 3;
        if (cr >= 1.5) return acute ? 4 : 2;
        if (cr >= 0.6) return 0;
        return 2;
    }

    private static int hematocritPoints(Double hct) {
        if (hct == null) return 0;
        if (hct >= 60) return 4;
        if (hct >= 50) return 2;
        if (hct >= 46) return 1;
        if (hct >= 30) return 0;
        if (hct >= 20) return 2;
        return 4;
    }

    private static int wbcPoints(Double wbc) {
        if (wbc == null) return 0;
        if (wbc >= 40) return 4;
        if (wbc >= 20) return 2;
        if (wbc >= 15) return 1;
        if (wbc >= 3) return 0;
        if (wbc >= 1) return 2;
        return 4;
    }

    private static int gcsPoints(Integer gcs) {
        if (gcs == null) return 0;
        int points = 15 - gcs;
        if (points > 12) return 12;
        return points;
    }

    private static int ageScore(Integer age) {
        if (age == null) return 0;
        if (age >= 75) return 6;
        if (age >= 65) return 5;
        if (age >= 55) return 3;
        if (age >= 45) return 2;
        return 0;
    }

    private static int chronicHealthPoints(String chronicType, Boolean emergencySurgical) {
        if (chronicType == null || "NONE".equals(chronicType)) return 0;
        boolean emergency = Boolean.TRUE.equals(emergencySurgical);
        return emergency ? 5 : 2;
    }

    @Value
    @Builder
    public static class ApacheIiInput {
        Double temperatureC;
        Double meanArterialPressure;
        Double heartRate;
        Double respiratoryRate;
        Double fio2;
        Double paO2;
        Double paCO2;
        Double aaDo2;
        Double ph;
        Double serumHco3;
        Double serumSodium;
        Double serumPotassium;
        Double serumCreatinine;
        Boolean acuteRenalFailure;
        Double hematocrit;
        Double whiteBloodCount;
        Integer gcs;
        Integer age;
        String chronicHealthType;
        Boolean isEmergencySurgical;
    }

    @Value
    @Builder
    public static class ApacheIiScore {
        int aps;
        int agePoints;
        int chronicPoints;
        int total;
    }
}
