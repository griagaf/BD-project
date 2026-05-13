package com.tacticaldistrict.command.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(UserRoleId.class)
@Table(name = "user_roles")
public class UserRoleEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
