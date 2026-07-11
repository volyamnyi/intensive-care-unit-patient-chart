package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "icu_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IcuDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icu_card_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "icuCard"})
    private IcuCard icuCard;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DayStatus status;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "escalation_sent")
    private Boolean escalationSent;

    @Column(name = "apache_ii")
    private Integer apacheIi;

    @Column(name = "sofa")
    private Integer sofa;

    @OneToMany(mappedBy = "icuDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("icuDay")
    private List<HourlyVital> hourlyVitals = new ArrayList<>();

    @OneToMany(mappedBy = "icuDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("icuDay")
    private List<FluidIntake> fluidIntakes = new ArrayList<>();

    @OneToMany(mappedBy = "icuDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("icuDay")
    private List<FluidOutput> fluidOutputs = new ArrayList<>();

    @OneToMany(mappedBy = "icuDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("icuDay")
    private List<ScaleAssessment> scaleAssessments = new ArrayList<>();
}
