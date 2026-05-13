package com.tacticaldistrict.command.user.repository;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.entity.PermissionEntity;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    @Query("""
            select distinct p.code
            from RolePermissionEntity rp
            join PermissionEntity p on p.id = rp.permissionId
            join RoleEntity r on r.id = rp.roleId
            where r.code in :roles
            """)
    Set<String> findPermissionCodesByRoles(@Param("roles") Collection<RoleCode> roles);
}
