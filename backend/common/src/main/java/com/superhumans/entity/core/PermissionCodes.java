package com.superhumans.entity.core;

/**
 * Canonical RBAC permission-code constants (#427 item 2). This class lives in
 * an allowlisted package ({@code entity.core}), so feature modules may import
 * codes directly instead of re-declaring them as string literals next to the
 * catalog (the drift the {@code codes_matchCatalog} tests used to guard).
 * {@code PermissionCatalog} aliases these same constants for its definitions
 * and default grants — one source of truth, referenced from both sides.
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    // Clinical
    public static final String EPISODE_CREATE = "EPISODE_CREATE";
    public static final String CLINICAL_DAY_CREATE = "CLINICAL_DAY_CREATE";
    public static final String SIGN_NURSE = "SIGN_NURSE";
    public static final String SIGN_DOCTOR = "SIGN_DOCTOR";
    public static final String REOPEN_DAY = "REOPEN_DAY";
    public static final String PRESCRIPTION_CREATE = "PRESCRIPTION_CREATE";
    public static final String PRESCRIPTION_LIST_CREATE = "PRESCRIPTION_LIST_CREATE";
    public static final String PRESCRIPTION_EXECUTE = "PRESCRIPTION_EXECUTE";
    public static final String VITALS_ENTER = "VITALS_ENTER";
    public static final String PATIENT_VIEW = "PATIENT_VIEW";
    public static final String SCALE_APACHE_SOFA = "SCALE_APACHE_SOFA";
    public static final String SCALE_CAMICU_BRADEN_RASS = "SCALE_CAMICU_BRADEN_RASS";

    // Administration
    public static final String AUDIT_ACCESS = "AUDIT_ACCESS";
    public static final String AUDIT_SECURITY_ACCESS = "AUDIT_SECURITY_ACCESS";
    public static final String AUDITOR_VIEW = "AUDITOR_VIEW";

    // Prosthetics manufacturing
    public static final String PROSTHETICS_DASHBOARD = "PROSTHETICS_DASHBOARD";
    public static final String PROSTHETICS_INSTANCE_CREATE = "PROSTHETICS_INSTANCE_CREATE";
    public static final String PROSTHETICS_STEP_COMPLETE = "PROSTHETICS_STEP_COMPLETE";
    public static final String PROSTHETICS_PAUSE_RESUME = "PROSTHETICS_PAUSE_RESUME";
    public static final String PROSTHETICS_TEMPLATE_MANAGE = "PROSTHETICS_TEMPLATE_MANAGE";
    public static final String PROSTHETICS_ORDER_MANAGE = "PROSTHETICS_ORDER_MANAGE";

    // Production monitoring (read-scoped dashboard codes, epic #271)
    public static final String PROSTHETICS_PRODUCTION_VIEW = "PROSTHETICS_PRODUCTION_VIEW";
    public static final String PROSTHETICS_PRODUCTION_VIEW_ALL = "PROSTHETICS_PRODUCTION_VIEW_ALL";
    public static final String PROSTHETICS_PRODUCTION_PATIENT_VIEW = "PROSTHETICS_PRODUCTION_PATIENT_VIEW";
    public static final String PROSTHETICS_PRODUCTION_QUALITY_VIEW = "PROSTHETICS_PRODUCTION_QUALITY_VIEW";

    // Module navigation (routing between sub-applications)
    public static final String MODULE_ICU_ACCESS = "MODULE_ICU_ACCESS";
    public static final String MODULE_MEDICATION_ACCESS = "MODULE_MEDICATION_ACCESS";
    public static final String MODULE_PROSTHETICS_ACCESS = "MODULE_PROSTHETICS_ACCESS";
    public static final String MODULE_ADMIN_ACCESS = "MODULE_ADMIN_ACCESS";
}
