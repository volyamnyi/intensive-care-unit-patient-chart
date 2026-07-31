package com.superhumans.util;

public final class ClinicalConstants {
    private ClinicalConstants() {}

    public static final double TEMPERATURE_MIN = 34.0;
    public static final double TEMPERATURE_MAX = 42.0;
    public static final String TEMPERATURE_MIN_STR = "34.0";
    public static final String TEMPERATURE_MAX_STR = "42.0";

    public static final int HEART_RATE_MIN = 0;
    public static final int HEART_RATE_MAX = 300;

    public static final int RESPIRATORY_RATE_MIN = 0;
    public static final int RESPIRATORY_RATE_MAX = 60;

    public static final int SYSTOLIC_BP_MIN = 50;
    public static final int SYSTOLIC_BP_MAX = 250;

    public static final int DIASTOLIC_BP_MIN = 30;
    public static final int DIASTOLIC_BP_MAX = 150;

    public static final double SPO2_MIN = 50.0;
    public static final double SPO2_MAX = 100.0;
    public static final String SPO2_MIN_STR = "50.0";
    public static final String SPO2_MAX_STR = "100.0";

    public static final double GLUCOSE_MIN = 1.0;
    public static final double GLUCOSE_MAX = 30.0;
    public static final String GLUCOSE_MIN_STR = "1.0";
    public static final String GLUCOSE_MAX_STR = "30.0";

    public static final double URINE_OUTPUT_MIN = 0.0;
    public static final String URINE_OUTPUT_MIN_STR = "0.0";

    public static final double DRAIN_OUTPUT_MIN = 0.0;
    public static final String DRAIN_OUTPUT_MIN_STR = "0.0";

    // painScore
    public static final int PAIN_SCORE_MIN = 0;
    public static final int PAIN_SCORE_MAX = 10;

    // etco2 (end-tidal CO2, mmHg)
    public static final double ETCO2_MIN = 0.0;
    public static final double ETCO2_MAX = 100.0;
    public static final String ETCO2_MIN_STR = "0.0";
    public static final String ETCO2_MAX_STR = "100.0";

    // fio2 (fraction of inspired oxygen, %)
    public static final double FIO2_MIN = 21.0;
    public static final double FIO2_MAX = 100.0;
    public static final String FIO2_MIN_STR = "21.0";
    public static final String FIO2_MAX_STR = "100.0";

    // GCS (Glasgow Coma Scale, 3-15)
    public static final int GCS_MIN = 3;
    public static final int GCS_MAX = 15;
    public static final String GCS_MIN_STR = "3";
    public static final String GCS_MAX_STR = "15";

    // Vasopressors (мкг/кг/хв)
    public static final double VASOPRESSOR_MIN = 0.0;
    public static final double VASOPRESSOR_MAX = 100.0;
    public static final String VASOPRESSOR_MIN_STR = "0.0";
    public static final String VASOPRESSOR_MAX_STR = "100.0";

    // cvp (central venous pressure, mmHg)
    public static final double CVP_MIN = 0.0;
    public static final double CVP_MAX = 30.0;
    public static final String CVP_MIN_STR = "0.0";
    public static final String CVP_MAX_STR = "30.0";
}
