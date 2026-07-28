package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.VitalSignEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalSignEntryRepository extends JpaRepository<VitalSignEntry, UUID> {
    List<VitalSignEntry> findByDayId(UUID dayId);
    Optional<VitalSignEntry> findByDayIdAndPeriod(UUID dayId, String period);
}
