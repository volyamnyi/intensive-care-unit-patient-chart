package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fluid_output")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FluidOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icu_day_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "icuDay"})
    private IcuDay icuDay;

    @Column(nullable = false)
    private Integer hour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutputType type;

    private Integer volume;

    @Column(name = "is_present")
    private Boolean isPresent;
}
