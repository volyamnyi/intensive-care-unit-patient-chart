package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hourly_vitals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HourlyVital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icu_day_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "icuDay"})
    private IcuDay icuDay;

    @Column(nullable = false)
    private Integer hour;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "cvp")
    private Integer cvp;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "ventilator_mode", length = 50)
    private String ventilatorMode;

    @Column(name = "tidal_volume")
    private Integer tidalVolume;

    @Column(name = "minute_ventilation")
    private Integer minuteVentilation;

    @Column(name = "peep")
    private Integer peep;

    @Column(name = "fio2")
    private Integer fio2;

    @Column(name = "vent_frequency")
    private Integer ventFrequency;
}
