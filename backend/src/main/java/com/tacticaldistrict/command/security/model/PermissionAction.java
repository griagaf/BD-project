package com.tacticaldistrict.command.security.model;

import java.util.Locale;

public enum PermissionAction {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    EXECUTE,
    GENERATE,
    ASSIGN;

    public static PermissionAction from(String value) {
        return PermissionAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
