package com.tacticaldistrict.command.unit.repository;

import com.tacticaldistrict.command.unit.entity.MilitaryUnitEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilitaryUnitRepository extends JpaRepository<MilitaryUnitEntity, Long> {

    List<MilitaryUnitEntity> findByFormationId(Long formationId);
}
