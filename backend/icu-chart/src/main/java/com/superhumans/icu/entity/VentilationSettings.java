package com.superhumans.icu.entity;

import jakarta.persistence.*;
import lombok.*;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "ventilation_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VentilationSettings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "record_hour", nullable = false)
    private Integer recordHour;

    @Column(name = "mode", length = 30)
    private String mode;

    @Column(name = "fio2")
    private Double fio2;

    @Column(name = "peep")
    private Double peep;

    @Column(name = "tidal_volume")
    private Double tidalVolume;

    @Column(name = "minute_volume")
    private Double minuteVolume;

    @Column(name = "pinsp")
    private Double pinsp;

    @Column(name = "psupport")
    private Double psupport;

    @Column(name = "trigger_type", length = 50)
    private String triggerType;

    @Column(name = "ie_ratio", length = 10)
    private String ieRatio;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "plateau_pressure")
    private Double plateauPressure;

    @Column(name = "mean_airway_pressure")
    private Double meanAirwayPressure;
}
