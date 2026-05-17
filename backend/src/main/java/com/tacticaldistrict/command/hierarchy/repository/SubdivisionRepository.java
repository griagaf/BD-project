package com.tacticaldistrict.command.hierarchy.repository;

import com.tacticaldistrict.command.hierarchy.entity.SubdivisionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubdivisionRepository extends JpaRepository<SubdivisionEntity, Long> {

    List<SubdivisionEntity> findByUnitId(Long unitId);

    List<SubdivisionEntity> findByParentId(Long parentId);
}
