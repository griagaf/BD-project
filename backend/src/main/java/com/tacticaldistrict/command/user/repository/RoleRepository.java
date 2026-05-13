package com.tacticaldistrict.command.user.repository;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.entity.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(RoleCode code);
}
