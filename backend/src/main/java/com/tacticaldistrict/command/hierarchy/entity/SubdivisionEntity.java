package com.tacticaldistrict.command.hierarchy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subdivisions")
public class SubdivisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subdivision_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "commander_id")
    private Long commanderId;
}
