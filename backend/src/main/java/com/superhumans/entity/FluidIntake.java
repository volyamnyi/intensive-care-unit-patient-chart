package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fluid_intake")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FluidIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icu_day_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "icuDay"})
    private IcuDay icuDay;

    @Column(nullable = false)
    private Integer hour;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(name = "volume_ordered")
    private Integer volumeOrdered;

    @Column(name = "volume_actual")
    private Integer volumeActual;

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;
}
