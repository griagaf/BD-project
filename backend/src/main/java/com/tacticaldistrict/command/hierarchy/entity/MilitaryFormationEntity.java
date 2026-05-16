package com.tacticaldistrict.command.hierarchy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "military_formations")
public class MilitaryFormationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "formation_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "formation_type", nullable = false, length = 50)
    private String formationType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "formation_date")
    private LocalDate formationDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "commander_id")
    private Long commanderId;
}
