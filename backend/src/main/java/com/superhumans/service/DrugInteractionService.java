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
                    warnings.add(rule.getSeverity() + ": PTG-" + code + " + PTG-" + other + " - " +
                            (rule.getDescription() != null ? rule.getDescription() : "potential interaction"));
                }
            }
        }
        return warnings.stream().distinct().toList();
    }

    public boolean isHighRisk(Integer categoryRef) {
        return categoryRef != null && (categoryRef == 13 || categoryRef == 14);
    }

    public void seedDefaultRules(Long userId) {
        if (ruleRepository.count() > 0) return;

        Map<String, String> rules = Map.of(
                "1,2", "PTG group 1 + 2 - potential additive effect",
                "1,3", "PTG group 1 + 3 - potential pharmacokinetic interaction",
                "2,3", "PTG group 2 + 3 - potential pharmacodynamic interaction",
                "2,4", "PTG group 2 + 4 - potential CNS depression",
                "1,4", "PTG group 1 + 4 - potential respiratory depression"
        );

        for (var entry : rules.entrySet()) {
            String[] codes = entry.getKey().split(",");
            DrugInteractionRule rule = DrugInteractionRule.builder()
                    .ptgCodeA(codes[0].trim())
                    .ptgCodeB(codes[1].trim())
                    .severity("WARNING")
                    .description(entry.getValue())
                    .build();
            rule.setCreatedBy(userId);
            rule.setUpdatedBy(userId);
            ruleRepository.save(rule);
        }
        log.info("Seeded {} default drug interaction rules", rules.size());
    }
}
