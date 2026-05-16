package com.tacticaldistrict.command.personnel.mapper;

import com.tacticaldistrict.command.personnel.dto.CreatePersonnelRequest;
import com.tacticaldistrict.command.personnel.dto.UpdatePersonnelRequest;
import com.tacticaldistrict.command.personnel.entity.PersonnelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PersonnelMapper {

    @Mapping(target = "id", ignore = true)
    PersonnelEntity toEntity(CreatePersonnelRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UpdatePersonnelRequest request, @MappingTarget PersonnelEntity entity);
}
