package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrugInteractionServiceTest {

    @Mock DrugInteractionRuleRepository ruleRepository;
    @InjectMocks DrugInteractionService service;

    @Test
    void isHighRisk_shouldReturnTrue_forCategory13() {
        assertThat(service.isHighRisk(13)).isTrue();
    }

    @Test
    void isHighRisk_shouldReturnTrue_forCategory14() {
        assertThat(service.isHighRisk(14)).isTrue();
    }

    @Test
    void isHighRisk_shouldReturnFalse_forCategory1() {
        assertThat(service.isHighRisk(1)).isFalse();
    }

    @Test
    void isHighRisk_shouldReturnFalse_forNull() {
        assertThat(service.isHighRisk(null)).isFalse();
    }

    @Test
    void checkConflicts_shouldReturnEmpty_forSingleCode() {
        when(ruleRepository.findConflictsForPtgCode("1")).thenReturn(List.of());
        assertThat(service.checkConflicts(List.of("1"))).isEmpty();
    }

    @Test
    void checkConflicts_shouldReturnEmpty_forNullInput() {
        assertThat(service.checkConflicts(null)).isEmpty();
    }

    @Test
    void checkConflicts_shouldDetectConflict() {
        var rule = DrugInteractionRule.builder()
                .ptgCodeA("1").ptgCodeB("2")
                .severity("WARNING").description("test")
                .build();
        when(ruleRepository.findConflictsForPtgCode("1")).thenReturn(List.of(rule));
        when(ruleRepository.findConflictsForPtgCode("2")).thenReturn(List.of(rule));

        var result = service.checkConflicts(List.of("1", "2"));
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).contains("WARNING");
    }
}
