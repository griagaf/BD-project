package com.tacticaldistrict.command.personnel.repository;

import com.tacticaldistrict.command.personnel.entity.PersonnelRankEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelRankRepository extends JpaRepository<PersonnelRankEntity, Long> {
}
