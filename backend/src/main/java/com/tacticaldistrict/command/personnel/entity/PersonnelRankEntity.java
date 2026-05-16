package com.tacticaldistrict.command.personnel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "personnel_ranks")
public class PersonnelRankEntity {

    @Id
    @Column(name = "personnel_id")
    private Long personnelId;

    @Column(name = "rank_id", nullable = false)
    private Long rankId;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;
}
