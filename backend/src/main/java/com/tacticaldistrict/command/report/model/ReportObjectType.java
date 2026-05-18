package com.tacticaldistrict.command.report.model;

import com.tacticaldistrict.command.security.model.ObjectType;

public enum ReportObjectType {
    DISTRICT,
    ARMY,
    FORMATION,
    BRIGADE,
    MILITARY_UNIT,
    COMPANY,
    PLATOON,
    SQUAD;

    public ObjectType toObjectType() {
        return ObjectType.from(name());
    }
}
