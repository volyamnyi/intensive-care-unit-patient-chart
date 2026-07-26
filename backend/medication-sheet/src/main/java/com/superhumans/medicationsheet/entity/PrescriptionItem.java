package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "prescription_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    PrescriptionList list;

    @Column(name = "medicine_name", nullable = false, length = 500)
    String medicineName;

    @Column(name = "medicine_method", length = 255)
    String medicineMethod;

    @Column(length = 255)
    String regime;

    @Column(length = 32)
    String status;

    @Column(name = "sort_order")
    Integer sortOrder;
}
