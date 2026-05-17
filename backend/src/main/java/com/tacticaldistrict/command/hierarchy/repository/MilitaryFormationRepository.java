package com.tacticaldistrict.command.hierarchy.repository;

import com.tacticaldistrict.command.hierarchy.entity.MilitaryFormationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilitaryFormationRepository extends JpaRepository<MilitaryFormationEntity, Long> {

    List<MilitaryFormationEntity> findByParentId(Long parentId);
}
