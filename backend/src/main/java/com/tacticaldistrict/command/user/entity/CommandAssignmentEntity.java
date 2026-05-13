package com.tacticaldistrict.command.user.entity;

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
@Table(name = "command_assignments")
public class CommandAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    @Column(name = "soldier_id", nullable = false)
    private Long soldierId;

    @Column(name = "object_type", nullable = false, length = 32)
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "starts_at", nullable = false)
    private LocalDate startsAt;

    @Column(name = "ends_at")
    private LocalDate endsAt;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;
}
