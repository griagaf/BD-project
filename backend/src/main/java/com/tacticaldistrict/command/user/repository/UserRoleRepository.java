package com.tacticaldistrict.command.user.repository;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.entity.UserRoleEntity;
import com.tacticaldistrict.command.user.entity.UserRoleId;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    @Query("""
            select r.code
            from UserRoleEntity ur
            join RoleEntity r on r.id = ur.roleId
            where ur.userId = :userId
            """)
    Set<RoleCode> findRoleCodesByUserId(@Param("userId") Long userId);

    List<UserRoleEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    long countByRoleId(Long roleId);
}
