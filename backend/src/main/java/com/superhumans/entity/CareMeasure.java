package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "care_measures")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CareMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icu_day_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "icuDay"})
    private IcuDay icuDay;

    @Column(nullable = false)
    private Integer hour;

    @Column(nullable = false, length = 200)
    private String procedure;

    @Column(nullable = false)
    private Boolean performed;

    @Column(name = "performed_by", length = 50)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
