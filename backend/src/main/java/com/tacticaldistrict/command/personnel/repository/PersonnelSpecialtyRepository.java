package com.tacticaldistrict.command.personnel.repository;

import com.tacticaldistrict.command.personnel.entity.PersonnelSpecialtyEntity;
import com.tacticaldistrict.command.personnel.entity.PersonnelSpecialtyId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelSpecialtyRepository extends JpaRepository<PersonnelSpecialtyEntity, PersonnelSpecialtyId> {

    List<PersonnelSpecialtyEntity> findByPersonnelId(Long personnelId);

    void deleteByPersonnelId(Long personnelId);
}
