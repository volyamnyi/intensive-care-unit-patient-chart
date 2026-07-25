package com.superhumans.service;

import com.superhumans.entity.DrugInteractionRule;
import com.superhumans.repository.DrugInteractionRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRuleRepository ruleRepository;

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
