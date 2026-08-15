package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "patient_state_assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientStateAssessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "record_hour", nullable = false)
    private Integer recordHour;

    @Column(name = "consciousness", length = 30)
    private String consciousness;

    @Column(name = "skin", length = 30)
    private String skin;

    @Column(name = "edema", length = 20)
    private String edema;

    @Column(name = "mucous_membranes", length = 30)
    private String mucousMembranes;

    @Column(name = "peripheral_circulation", length = 30)
    private String peripheralCirculation;

    @Column(name = "bowel_sounds", length = 30)
    private String bowelSounds;

    @Column(name = "general_condition", length = 30)
    private String generalCondition;

    @Column(name = "additional_notes", length = 500)
    private String additionalNotes;
}
