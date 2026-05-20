package com.neong.vixie.model;

/**
 * Status of a marketplace item in the creator publishing workflow.
 * Replaces the previous boolean is_active column.
 */
public enum ContentStatus {
    DRAFT,
    PUBLISHED,
    UNPUBLISHED,
    REJECTED
}
