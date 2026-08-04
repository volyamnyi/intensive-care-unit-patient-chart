package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prosthetics_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsOrder extends BaseEntity {

    @Column(name = "order_number", nullable = false, length = 32)
    String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    ProstheticsPatient patient;

    @Column(name = "prosthesis_type", length = 64)
    String prosthesisType;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", length = 32)
    ProductType productType;

    @Column(name = "amputation_level", length = 64)
    String amputationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "limb_side", length = 16)
    LimbSide limbSide;

    @Column(name = "doctor_name")
    String doctorName;

    @Column(name = "prescription_date")
    LocalDate prescriptionDate;

    @Column(name = "materials", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String materials;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    OrderStatus status = OrderStatus.NEW;

    @Column(name = "recipe_pdf_data")
    byte[] recipePdfData;

    @Column(name = "recipe_pdf_generated_at")
    LocalDateTime recipePdfGeneratedAt;

    public boolean hasRecipePdf() {
        return recipePdfData != null;
    }
}
