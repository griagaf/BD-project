package com.tacticaldistrict.command.personnel.repository;

import com.tacticaldistrict.command.personnel.entity.MilitaryRankEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilitaryRankRepository extends JpaRepository<MilitaryRankEntity, Long> {
}
