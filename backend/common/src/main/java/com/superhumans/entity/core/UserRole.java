package com.superhumans.entity.core;

public enum UserRole {
    DOCTOR,
    NURSE,
    HEAD_OF_DEPARTMENT,
    ADMINISTRATOR,
    AUDITOR,
    ADJACENT_SPECIALIST,
    PROSTHETIST,
    PROSTHETICS_ADMINISTRATOR,
    /**
     * First-login directory role: authenticated with zero permissions.
     * Holds no grants in the default matrix and is excluded from every
     * URL-ceiling role set, so only {@code /api/users/me/**} is reachable.
     * Elevated solely by an administrator via the role-update endpoint.
     */
    GUEST
}
