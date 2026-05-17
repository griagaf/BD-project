package com.tacticaldistrict.command.personnel.repository;

import com.tacticaldistrict.command.personnel.entity.PersonnelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelRepository extends JpaRepository<PersonnelEntity, Long> {

    boolean existsByPersonalNumber(String personalNumber);

    boolean existsByPersonalNumberAndIdNot(String personalNumber, Long id);
}
