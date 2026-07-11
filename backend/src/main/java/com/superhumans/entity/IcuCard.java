package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "icu_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IcuCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_name", nullable = false, length = 200)
    private String patientName;

    @Column(name = "medical_card_number", length = 50)
    private String medicalCardNumber;

    @Column(name = "admission_date", nullable = false)
    private LocalDateTime admissionDate;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "apache_ii")
    private Integer apacheIi;

    @Column(name = "sofa")
    private Integer sofa;

    @Column(name = "patient_height")
    private Integer patientHeight;

    @Column(name = "patient_weight")
    private Integer patientWeight;

    @Column(name = "ideal_body_weight")
    private Integer idealBodyWeight;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "rh_factor", length = 10)
    private String rhFactor;

    @Column(name = "patient_sex_code", length = 1)
    private String patientSexCode;

    @Column(name = "patient_birth_date")
    private java.time.LocalDate patientBirthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "icuCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("icuCard")
    private List<IcuDay> icuDays = new ArrayList<>();

    @OneToMany(mappedBy = "icuCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("icuCard")
    private List<Prescription> prescriptions = new ArrayList<>();
}
