package com.tacticaldistrict.command.security.model;

import java.util.Locale;

public enum ObjectType {
    DISTRICT,
    FORMATION,
    ARMY,
    CORPS,
    DIVISION,
    BRIGADE,
    MILITARY_UNIT,
    BATTALION,
    COMPANY,
    PLATOON,
    SQUAD,
    PERSONNEL,
    SELF,
    EQUIPMENT,
    WEAPON,
    BUILDING,
    SPECIALTY,
    QUERY,
    ALERT,
    REPORT,
    USER,
    COMMAND_ASSIGNMENT;

    public static ObjectType from(String value) {
        return ObjectType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isFormationLevel() {
        return switch (this) {
            case FORMATION, ARMY, CORPS, DIVISION, BRIGADE -> true;
            default -> false;
        };
    }

    public boolean isSubdivisionLevel() {
        return switch (this) {
            case BATTALION, COMPANY, PLATOON, SQUAD -> true;
            default -> false;
        };
    }
}
