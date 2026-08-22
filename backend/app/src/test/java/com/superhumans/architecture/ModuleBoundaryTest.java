package com.superhumans.architecture;

import com.superhumans.mis.MisApiClient;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Module-independence contract for the four concerns (platform, ICU, medication sheet,
 * prosthetics manufacturing).
 *
 * <p>Feature packages ({@code medicationsheet}, {@code prosthesismanufacturing}) may only
 * depend on the shared platform allowlist — never on ICU domain packages
 * ({@code controller}, {@code service}, {@code dto}, {@code entity}, {@code mapper},
 * {@code repository} root packages) and never on each other. Platform packages must not
 * depend on feature packages.
 */
@AnalyzeClasses(packages = "com.superhumans", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    private static final String MEDICATION_SHEET = "com.superhumans.medicationsheet..";

    /**
     * Prosthetics feature packages.
     */
    private static final String[] PROSTHESIS_PACKAGES = {
        "com.superhumans.prosthesismanufacturing.."
    };

    /**
     * Shared platform packages a feature module is allowed to use. Everything else under
     * {@code com.superhumans} (ICU domain root packages) is off-limits to features.
     *
     * <p>Exact classes (AuditService, PermissionService, SpringContext) cannot be expressed
     * as package patterns — {@code resideInAnyPackage} matches package names only — so they
     * are allowed via {@code name(...)} predicates in {@link #platformTargets(String[])}.
     */
    private static final String[] SHARED_PLATFORM_ALLOWLIST = {
        "com.superhumans.entity.base..",
        "com.superhumans.entity.core..",
        "com.superhumans.repository.core..",
        "com.superhumans.exception..",
        "com.superhumans.mis..",
        "com.superhumans.util..",
        "java..",
        "jakarta..",
        "lombok..",
        "org.springframework..",
        "org.mapstruct..",
        "org.hibernate..",
        "org.slf4j..",
        "com.fasterxml.jackson..",
        "com.itextpdf..",
        "io.swagger.v3.oas.annotations.."
    };

    /**
     * ICU (com.superhumans.controller) + feature-module controller packages targeted by
     * the mutating-mapping security rule (#2).
     */
    private static final String[] CONTROLLER_PACKAGES = {
        "com.superhumans.controller..",
        "com.superhumans.medicationsheet.controller..",
        "com.superhumans.prosthesismanufacturing.controller.."
    };

    /**
     * Platform controllers that are intentionally ceiling-gated or public (auth login /
     * admin console / mock-MIS control) and therefore excluded from rule #2 — they are
     * not clinical feature endpoints.
     */
    private static final String[] PLATFORM_CONTROLLER_EXCLUSIONS = {
        "AuthController",
        "AdminController",
        "MockMedicalInformationSystemController"
    };

    @ArchTest
    static final ArchRule medicationSheetDependsOnlyOnPlatformAllowlist = classes()
        .that().resideInAPackage(MEDICATION_SHEET)
        .should().onlyDependOnClassesThat(platformTargets(new String[] {MEDICATION_SHEET}));

    @ArchTest
    static final ArchRule prosthesisDependsOnlyOnPlatformAllowlist = classes()
        .that().resideInAnyPackage(PROSTHESIS_PACKAGES)
        .should().onlyDependOnClassesThat(platformTargets(PROSTHESIS_PACKAGES));

    @ArchTest
    static final ArchRule featuresDoNotDependOnEachOther = noClasses()
        .that().resideInAPackage(MEDICATION_SHEET)
        .should().dependOnClassesThat().resideInAnyPackage(PROSTHESIS_PACKAGES);

    @ArchTest
    static final ArchRule platformDoesNotDependOnFeatures = noClasses()
        .that(notResidingInFeaturePackages())
        .should().dependOnClassesThat().resideInAnyPackage(allFeaturePackages());

    /** Rule #1 (MIS write-path guardian): only the MIS facade may call the client. */
    @ArchTest
    static final ArchRule misCallsOnlyFromFacade = noClasses()
        .that().resideOutsideOfPackage("com.superhumans.mis..")
        .should().callMethodWhere(new DescribedPredicate<JavaMethodCall>("targets MisApiClient.callMethod") {
            @Override
            public boolean test(JavaMethodCall call) {
                return call.getTargetOwner().isAssignableTo(MisApiClient.class)
                    && call.getName().equals("callMethod");
            }
        });

    /** Rule #2: every mutating mapping in feature/ICU controllers carries method security. */
    @ArchTest
    static final ArchRule mutatingMappingsCarryMethodSecurity = methods()
        .that(isMutatingMapping())
        .and().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
        .and().areDeclaredInClassesThat(isEnforcedController())
        .should().beAnnotatedWith(PreAuthorize.class);

    private static DescribedPredicate<JavaClass> isEnforcedController() {
        DescribedPredicate<JavaClass> predicate = resideInAnyPackage(CONTROLLER_PACKAGES);
        for (String excluded : PLATFORM_CONTROLLER_EXCLUSIONS) {
            predicate = predicate.and(simpleName(excluded).negate());
        }
        return predicate;
    }

    private static DescribedPredicate<JavaMethod> isMutatingMapping() {
        return new DescribedPredicate<JavaMethod>("is a mutating REST mapping") {
            @Override
            public boolean test(JavaMethod method) {
                return method.isAnnotatedWith(PostMapping.class)
                    || method.isAnnotatedWith(PutMapping.class)
                    || method.isAnnotatedWith(PatchMapping.class)
                    || method.isAnnotatedWith(DeleteMapping.class);
            }
        };
    }

    private static DescribedPredicate<JavaClass> platformTargets(String[] featurePackages) {
        return JavaClass.Predicates.resideInAnyPackage(combine(featurePackages, SHARED_PLATFORM_ALLOWLIST))
            .or(name("com.superhumans.service.AuditService"))
            .or(name("com.superhumans.service.PermissionService"))
            .or(name("com.superhumans.config.SpringContext"));
    }

    private static DescribedPredicate<JavaClass> notResidingInFeaturePackages() {
        return JavaClass.Predicates.resideInAnyPackage(allFeaturePackages()).negate();
    }

    private static String[] allFeaturePackages() {
        return combine(MEDICATION_SHEET, PROSTHESIS_PACKAGES);
    }

    private static String[] combine(String first, String[] rest) {
        String[] result = new String[1 + rest.length];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }

    private static String[] combine(String[] first, String[] second) {
        String[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
