package com.superhumans.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Arrays;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
