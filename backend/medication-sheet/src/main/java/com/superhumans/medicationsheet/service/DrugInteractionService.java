package com.superhumans.medicationsheet.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.medicationsheet.entity.DrugInteractionRule;
import com.superhumans.medicationsheet.repository.DrugInteractionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrugInteractionService {

    DrugInteractionRuleRepository ruleRepository;

    public List<String> checkConflicts(List<String> ptgCodes) {
        if (ptgCodes == null || ptgCodes.size() < 2) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        Set<String> codeSet = new HashSet<>(ptgCodes);
        for (String code : codeSet) {
            if (code == null || code.isBlank()) continue;
            List<DrugInteractionRule> rules = ruleRepository.findConflictsForPtgCode(code);
            for (DrugInteractionRule rule : rules) {
                String other = rule.getPtgCodeA().equals(code) ? rule.getPtgCodeB() : rule.getPtgCodeA();
                if (codeSet.contains(other)) {
                    warnings.add(rule.getSeverity() + ": " + rule.getPtgCodeA() + "+" + rule.getPtgCodeB());
                }
            }
        }
        return warnings;
    }

    public boolean isHighRisk(Integer categoryRef) {
        return categoryRef != null && (categoryRef == 13 || categoryRef == 14);
    }
}
