package com.tacticaldistrict.command.personnel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(PersonnelSpecialtyId.class)
@Table(name = "personnel_specialties")
public class PersonnelSpecialtyEntity {

    @Id
    @Column(name = "personnel_id")
    private Long personnelId;

    @Id
    @Column(name = "specialty_id")
    private Long specialtyId;
}
