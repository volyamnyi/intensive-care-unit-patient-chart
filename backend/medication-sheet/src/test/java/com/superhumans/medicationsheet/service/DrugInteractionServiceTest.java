package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.DrugInteractionRule;
import com.superhumans.medicationsheet.repository.DrugInteractionRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

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
        assertThat(service.checkConflicts(List.of("1"))).isEmpty();
    }

    @Test
    void checkConflicts_shouldReturnEmpty_forNull() {
        assertThat(service.checkConflicts(null)).isEmpty();
    }

    @Test
    void checkConflicts_shouldDetectConflict() {
        var rule = DrugInteractionRule.builder()
                .ptgCodeA("1").ptgCodeB("2").severity("WARNING").build();
        rule.setCreatedBy(0L);
        rule.setUpdatedBy(0L);
        when(ruleRepository.findConflictsForPtgCode("1")).thenReturn(List.of(rule));
        when(ruleRepository.findConflictsForPtgCode("2")).thenReturn(List.of());

        var result = service.checkConflicts(List.of("1", "2"));
        assertThat(result).isNotEmpty();
    }
}
