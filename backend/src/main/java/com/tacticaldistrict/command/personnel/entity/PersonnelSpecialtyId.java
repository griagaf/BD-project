package com.tacticaldistrict.command.personnel.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PersonnelSpecialtyId implements Serializable {

    private Long personnelId;
    private Long specialtyId;
}
