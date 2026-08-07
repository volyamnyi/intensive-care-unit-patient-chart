package com.superhumans.service;

import com.superhumans.entity.UserRole;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catalog of application permissions (operations) and the default role-permission
 * matrix. The catalog is the single source of truth for permission codes: the
 * Liquibase seed SQL mirrors these definitions, and {@link PermissionService}
 * re-seeds the database from this catalog whenever the {@code permissions} table
 * is empty (fresh install). Runtime grants are managed by the administrator
 * through the admin UI and persisted in {@code role_permissions}.
 */
public final class PermissionCatalog {

    private PermissionCatalog() {
    }

    // Clinical operations
    public static final String EPISODE_CREATE = "EPISODE_CREATE";
    public static final String CLINICAL_DAY_CREATE = "CLINICAL_DAY_CREATE";
    public static final String SIGN_NURSE = "SIGN_NURSE";
    public static final String SIGN_DOCTOR = "SIGN_DOCTOR";
    public static final String REOPEN_DAY = "REOPEN_DAY";
    public static final String PRESCRIPTION_CREATE = "PRESCRIPTION_CREATE";
    public static final String PRESCRIPTION_EXECUTE = "PRESCRIPTION_EXECUTE";
    public static final String VITALS_ENTER = "VITALS_ENTER";
    public static final String PATIENT_VIEW = "PATIENT_VIEW";
    public static final String SCALE_APACHE_SOFA = "SCALE_APACHE_SOFA";
    public static final String SCALE_CAMICU_BRADEN_RASS = "SCALE_CAMICU_BRADEN_RASS";

    // Administration
    public static final String AUDIT_ACCESS = "AUDIT_ACCESS";
    public static final String AUDITOR_VIEW = "AUDITOR_VIEW";

    // Prosthetics manufacturing
    public static final String PROSTHETICS_DASHBOARD = "PROSTHETICS_DASHBOARD";
    public static final String PROSTHETICS_INSTANCE_CREATE = "PROSTHETICS_INSTANCE_CREATE";
    public static final String PROSTHETICS_STEP_COMPLETE = "PROSTHETICS_STEP_COMPLETE";
    public static final String PROSTHETICS_PAUSE_RESUME = "PROSTHETICS_PAUSE_RESUME";
    public static final String PROSTHETICS_GATE_DECISION = "PROSTHETICS_GATE_DECISION";
    public static final String PROSTHETICS_TEMPLATE_MANAGE = "PROSTHETICS_TEMPLATE_MANAGE";
    public static final String PROSTHETICS_ORDER_MANAGE = "PROSTHETICS_ORDER_MANAGE";

    /** A single permission definition shown in the admin matrix UI. */
    public record Def(String code, String label, String description, String category) {
    }

    private static final String CLINICAL = "Клінічні операції";
    private static final String ADMINISTRATION = "Адміністрування";
    private static final String PROSTHETICS = "Протезування";

    private static final List<Def> DEFINITIONS = List.of(
            new Def(EPISODE_CREATE, "Створення епізоду",
                    "Створення нового епізоду (карти інтенсивної терапії)", CLINICAL),
            new Def(CLINICAL_DAY_CREATE, "Створення клінічного дня",
                    "Створення нового клінічного дня для епізоду", CLINICAL),
            new Def(SIGN_NURSE, "Підпис медсестрою",
                    "Підписання клінічного дня на етапі медсестри", CLINICAL),
            new Def(SIGN_DOCTOR, "Підпис лікарем",
                    "Підписання клінічного дня на етапі лікаря", CLINICAL),
            new Def(REOPEN_DAY, "Перевідкриття дня",
                    "Перевідкриття підписаного клінічного дня", CLINICAL),
            new Def(PRESCRIPTION_CREATE, "Створення призначень",
                    "Створення та планування лікарських призначень і медичних замовлень", CLINICAL),
            new Def(PRESCRIPTION_EXECUTE, "Виконання призначень",
                    "Виконання та завершення призначень медсестрою", CLINICAL),
            new Def(VITALS_ENTER, "Введення показників",
                    "Введення та редагування показників пацієнта (vital signs)", CLINICAL),
            new Def(PATIENT_VIEW, "Перегляд даних пацієнта",
                    "Перегляд даних пацієнта та клінічної документації (read-only)", CLINICAL),
            new Def(SCALE_APACHE_SOFA, "Шкали APACHE II / SOFA",
                    "Створення результатів клінічних шкал APACHE II та SOFA", CLINICAL),
            new Def(SCALE_CAMICU_BRADEN_RASS, "Шкали CAM-ICU / Браден / RASS",
                    "Створення результатів клінічних шкал CAM-ICU, Браден, RASS", CLINICAL),
            new Def(AUDIT_ACCESS, "Журнал аудиту",
                    "Перегляд журналу аудиту", ADMINISTRATION),
            new Def(AUDITOR_VIEW, "Read-only доступ аудитора",
                    "Службовий read-only доступ ролі AUDITOR", ADMINISTRATION),
            new Def(PROSTHETICS_DASHBOARD, "Дашборд протезування",
                    "Перегляд власних процесів протезування та довідників", PROSTHETICS),
            new Def(PROSTHETICS_INSTANCE_CREATE, "Створення процесу",
                    "Створення процесу виготовлення протеза (Wizard)", PROSTHETICS),
            new Def(PROSTHETICS_STEP_COMPLETE, "Виконання кроків",
                    "Заповнення та завершення кроків процесу, завантаження файлів", PROSTHETICS),
            new Def(PROSTHETICS_PAUSE_RESUME, "Пауза / відновлення",
                    "Призупинення та відновлення процесу", PROSTHETICS),
            new Def(PROSTHETICS_GATE_DECISION, "Рішення quality gate",
                    "Рішення контролю якості (PASS / REWORK / FAIL)", PROSTHETICS),
            new Def(PROSTHETICS_TEMPLATE_MANAGE, "Керування шаблонами",
                    "Створення та редагування шаблонів технологічних процесів", PROSTHETICS),
            new Def(PROSTHETICS_ORDER_MANAGE, "Пацієнти та замовлення",
                    "Створення пацієнтів і замовлень протезування", PROSTHETICS));

    /**
     * Default role-permission matrix, aligned with the approved access table.
     * Grant rows are seeded on fresh installs; administrators may change them
     * at runtime through the admin interface.
     */
    private static final Map<UserRole, Set<String>> DEFAULT_MATRIX = Map.ofEntries(
            Map.entry(UserRole.DOCTOR, Set.of(
                    EPISODE_CREATE, CLINICAL_DAY_CREATE, SIGN_DOCTOR,
                    PRESCRIPTION_CREATE, PATIENT_VIEW,
                    SCALE_APACHE_SOFA, SCALE_CAMICU_BRADEN_RASS)),
            Map.entry(UserRole.NURSE, Set.of(
                    SIGN_NURSE, PRESCRIPTION_EXECUTE, VITALS_ENTER,
                    PATIENT_VIEW, SCALE_CAMICU_BRADEN_RASS)),
            Map.entry(UserRole.HEAD_OF_DEPARTMENT, Set.of(
                    EPISODE_CREATE, CLINICAL_DAY_CREATE, SIGN_DOCTOR, REOPEN_DAY,
                    PRESCRIPTION_CREATE, PATIENT_VIEW,
                    SCALE_APACHE_SOFA, SCALE_CAMICU_BRADEN_RASS)),
            Map.entry(UserRole.ADMINISTRATOR, Set.of(
                    PATIENT_VIEW, AUDIT_ACCESS)),
            Map.entry(UserRole.AUDITOR, Set.of(
                    AUDITOR_VIEW)),
            Map.entry(UserRole.ADJACENT_SPECIALIST, Set.of(
                    PATIENT_VIEW)),
            Map.entry(UserRole.PROSTHETIST, Set.of(
                    PROSTHETICS_DASHBOARD, PROSTHETICS_INSTANCE_CREATE,
                    PROSTHETICS_STEP_COMPLETE, PROSTHETICS_PAUSE_RESUME)),
            Map.entry(UserRole.PROSTHETICS_ADMINISTRATOR, Set.of(
                    PROSTHETICS_DASHBOARD, PROSTHETICS_INSTANCE_CREATE,
                    PROSTHETICS_STEP_COMPLETE, PROSTHETICS_PAUSE_RESUME,
                    PROSTHETICS_GATE_DECISION, PROSTHETICS_TEMPLATE_MANAGE,
                    PROSTHETICS_ORDER_MANAGE)));

    public static List<Def> definitions() {
        return DEFINITIONS;
    }

    public static Map<UserRole, Set<String>> defaultMatrix() {
        return DEFAULT_MATRIX;
    }

    /** All permission codes, used by the admin matrix UI and tests. */
    public static Set<String> allCodes() {
        return DEFINITIONS.stream().map(Def::code).collect(Collectors.toUnmodifiableSet());
    }
}
