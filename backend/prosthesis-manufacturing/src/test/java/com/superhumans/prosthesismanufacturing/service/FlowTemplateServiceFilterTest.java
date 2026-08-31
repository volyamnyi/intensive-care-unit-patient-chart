package com.superhumans.prosthesismanufacturing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.mapper.FlowTemplateMapperImpl;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.QualityGateRepository;
import com.superhumans.prosthesismanufacturing.repository.ReworkLoopRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateElementRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStageRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStepRepository;
import com.superhumans.service.AuditService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 * Фаза 3 — Setup Flow & Template Selection: BOTH wildcard та generic_lower_limb.
 */
@ExtendWith(MockitoExtension.class)
class FlowTemplateServiceFilterTest {

    @Mock FlowTemplateRepository templateRepository;
    @Mock TemplateStageRepository stageRepository;
    @Mock TemplateStepRepository stepRepository;
    @Mock TemplateElementRepository elementRepository;
    @Mock QualityGateRepository gateRepository;
    @Mock ReworkLoopRepository reworkLoopRepository;
    @Mock AuditService auditService;

    FlowTemplateService service;

    @BeforeEach
    void setUp() {
        service = new FlowTemplateService(templateRepository, stageRepository, stepRepository,
                elementRepository, gateRepository, reworkLoopRepository,
                new FlowTemplateMapperImpl(), auditService,
                new TemplateSnapshotParser(new ObjectMapper()), new ObjectMapper());
    }

    private FlowTemplate template(String name, ProductType productType, String amputationLevel, LimbSide limbSide, TemplateStatus status) {
        FlowTemplate t = new FlowTemplate();
        t.setName(name);
        t.setTemplateVersion(1);
        t.setProductType(productType);
        t.setAmputationLevel(amputationLevel);
        t.setLimbSide(limbSide);
        t.setStatus(status);
        t.setEstimatedDurationMin(100);
        return t;
    }

    @Test
    void list_withBothWildcard_returnsGenericTemplate() {
        var generic = template("TP-LL-02", ProductType.LOWER_LIMB, "generic_lower_limb", null, TemplateStatus.ACTIVE);
        var specific = template("TP-LL-01", ProductType.LOWER_LIMB, "below_knee", LimbSide.RIGHT, TemplateStatus.ACTIVE);
        var upper = template("TP-UL-01", ProductType.UPPER_LIMB, "upper_third_forearm", LimbSide.LEFT, TemplateStatus.ACTIVE);
        when(templateRepository.findAll()).thenReturn(List.of(generic, specific, upper));

        // Query with BOTH should return both LOWER templates (generic matches any, BOTH matches any)
        var resultBoth = service.list("LOWER_LIMB", "BOTH", "BOTH", TemplateStatus.ACTIVE);
        assertThat(resultBoth).extracting("name").contains("TP-LL-02", "TP-LL-01");
        assertThat(resultBoth).extracting("name").doesNotContain("TP-UL-01");

        // Query with specific amputationLevel "Гомілка, с/3" should still return generic (wildcard)
        var resultSpecific = service.list("LOWER_LIMB", "Гомілка, с/3", "LEFT", TemplateStatus.ACTIVE);
        assertThat(resultSpecific).extracting("name").contains("TP-LL-02");

        // Query with generic_lower_limb should return generic
        var resultGeneric = service.list("LOWER_LIMB", "generic_lower_limb", "BOTH", TemplateStatus.ACTIVE);
        assertThat(resultGeneric).extracting("name").contains("TP-LL-02");

        // Query with null amputationLevel should return all LOWER
        var resultNull = service.list("LOWER_LIMB", null, null, TemplateStatus.ACTIVE);
        assertThat(resultNull).extracting("name").contains("TP-LL-02", "TP-LL-01");
    }

    @Test
    void list_withLimbSideNullTemplateMatchesAnyQuery() {
        var generic = template("TP-LL-02", ProductType.LOWER_LIMB, "generic_lower_limb", null, TemplateStatus.ACTIVE);
        when(templateRepository.findAll()).thenReturn(List.of(generic));

        var left = service.list("LOWER_LIMB", "generic_lower_limb", "LEFT", TemplateStatus.ACTIVE);
        assertThat(left).hasSize(1);

        var right = service.list("LOWER_LIMB", "generic_lower_limb", "RIGHT", TemplateStatus.ACTIVE);
        assertThat(right).hasSize(1);

        var both = service.list("LOWER_LIMB", "generic_lower_limb", "BOTH", TemplateStatus.ACTIVE);
        assertThat(both).hasSize(1);
    }

    @Test
    void list_caseInsensitiveAmputationLevel() {
        var generic = template("TP-LL-02", ProductType.LOWER_LIMB, "generic_lower_limb", null, TemplateStatus.ACTIVE);
        when(templateRepository.findAll()).thenReturn(List.of(generic));

        var upper = service.list("LOWER_LIMB", "GENERIC_LOWER_LIMB", "LEFT", TemplateStatus.ACTIVE);
        assertThat(upper).hasSize(1);

        var mixed = service.list("LOWER_LIMB", "Generic_Lower_Limb", "LEFT", TemplateStatus.ACTIVE);
        assertThat(mixed).hasSize(1);
    }
}
