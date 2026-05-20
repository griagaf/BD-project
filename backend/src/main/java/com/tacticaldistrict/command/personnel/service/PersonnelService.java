package com.tacticaldistrict.command.personnel.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.common.attribute.DynamicAttributeValueRequest;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.personnel.dto.ChainOfCommandNodeResponse;
import com.tacticaldistrict.command.personnel.dto.CreatePersonnelRequest;
import com.tacticaldistrict.command.personnel.dto.PersonnelFilter;
import com.tacticaldistrict.command.personnel.dto.PersonnelProfileResponse;
import com.tacticaldistrict.command.personnel.dto.PersonnelResponse;
import com.tacticaldistrict.command.personnel.dto.RankResponse;
import com.tacticaldistrict.command.personnel.dto.SpecialtyResponse;
import com.tacticaldistrict.command.personnel.dto.UpdatePersonnelRequest;
import com.tacticaldistrict.command.personnel.entity.MilitaryRankEntity;
import com.tacticaldistrict.command.personnel.entity.PersonnelEntity;
import com.tacticaldistrict.command.personnel.entity.PersonnelRankEntity;
import com.tacticaldistrict.command.personnel.entity.PersonnelSpecialtyEntity;
import com.tacticaldistrict.command.personnel.entity.SpecialtyEntity;
import com.tacticaldistrict.command.personnel.mapper.PersonnelMapper;
import com.tacticaldistrict.command.personnel.repository.MilitaryRankRepository;
import com.tacticaldistrict.command.personnel.repository.PersonnelQueryRepository;
import com.tacticaldistrict.command.personnel.repository.PersonnelRankRepository;
import com.tacticaldistrict.command.personnel.repository.PersonnelRepository;
import com.tacticaldistrict.command.personnel.repository.PersonnelSpecialtyRepository;
import com.tacticaldistrict.command.personnel.repository.SpecialtyRepository;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final PersonnelRankRepository personnelRankRepository;
    private final PersonnelSpecialtyRepository personnelSpecialtyRepository;
    private final MilitaryRankRepository militaryRankRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PersonnelQueryRepository personnelQueryRepository;
    private final PersonnelMapper personnelMapper;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<PersonnelResponse> search(PersonnelFilter filter, Pageable pageable) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("personnel:read")) {
            throw new AccessDeniedException("Access denied");
        }
        return personnelQueryRepository.search(user, filter, pageable);
    }

    @Transactional(readOnly = true)
    public PersonnelResponse findById(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.PERSONNEL, id);
        return personnelQueryRepository.findById(user, id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
    }

    @Transactional(readOnly = true)
    public PersonnelProfileResponse profile(Long id) {
        PersonnelResponse personnel = findById(id);
        return personnelQueryRepository.profile(personnel);
    }

    @Transactional(readOnly = true)
    public List<ChainOfCommandNodeResponse> chainOfCommand(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.PERSONNEL, id);
        return personnelQueryRepository.chainOfCommand(id);
    }

    @Transactional
    public PersonnelResponse create(CreatePersonnelRequest request) {
        UserContext user = userContextProvider.current();
        validateBirthDate(request.birthDate());
        validatePersonalNumberAvailable(request.personalNumber(), null);
        ObjectType parentType = subdivisionObjectType(request.subdivisionId());
        permissionService.checkCreate(user, parentType, request.subdivisionId(), ObjectType.PERSONNEL);

        PersonnelEntity entity = personnelMapper.toEntity(request);
        PersonnelEntity saved = personnelRepository.save(entity);
        updateRank(saved.getId(), request.rankId(), request.rankAssignmentDate());
        syncRankAttributes(saved.getId(), request.rankAttributes());
        replaceSpecialties(saved.getId(), request.specialtyIds());
        auditService.created(user, ObjectType.PERSONNEL, saved.getId(), "Personnel created");

        return findById(saved.getId());
    }

    @Transactional
    public PersonnelResponse update(Long id, UpdatePersonnelRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.PERSONNEL, id);
        validateBirthDate(request.birthDate());
        validatePersonalNumberAvailable(request.personalNumber(), id);
        ObjectType parentType = subdivisionObjectType(request.subdivisionId());
        if (!permissionService.canCreate(user, parentType, request.subdivisionId(), ObjectType.PERSONNEL)) {
            throw new AccessDeniedException("Access denied");
        }

        PersonnelEntity entity = personnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Personnel not found: " + id));
        personnelMapper.updateEntity(request, entity);
        PersonnelEntity saved = personnelRepository.save(entity);
        updateRank(saved.getId(), request.rankId(), request.rankAssignmentDate());
        syncRankAttributes(saved.getId(), request.rankAttributes());
        replaceSpecialties(saved.getId(), request.specialtyIds());
        auditService.updated(user, ObjectType.PERSONNEL, saved.getId(), "Personnel updated");

        return findById(saved.getId());
    }

    @Transactional
    public void delete(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkDelete(user, ObjectType.PERSONNEL, id);
        if (!personnelRepository.existsById(id)) {
            throw new EntityNotFoundException("Personnel not found: " + id);
        }

        personnelRepository.deleteById(id);
        auditService.deleted(user, ObjectType.PERSONNEL, id, "Personnel deleted");
    }

    @Transactional(readOnly = true)
    public List<RankResponse> ranks() {
        return militaryRankRepository.findAll()
                .stream()
                .map(this::toRankResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> specialties() {
        return specialtyRepository.findAll()
                .stream()
                .map(this::toSpecialtyResponse)
                .toList();
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("Personnel must be at least 18 years old");
        }
    }

    private void validatePersonalNumberAvailable(String personalNumber, Long currentId) {
        boolean exists = currentId == null
                ? personnelRepository.existsByPersonalNumber(personalNumber)
                : personnelRepository.existsByPersonalNumberAndIdNot(personalNumber, currentId);
        if (exists) {
            throw new IllegalArgumentException("Personal number already exists");
        }
    }

    private ObjectType subdivisionObjectType(Long subdivisionId) {
        String type = personnelQueryRepository.subdivisionType(subdivisionId);
        if (type == null) {
            throw new EntityNotFoundException("Subdivision not found: " + subdivisionId);
        }
        return switch (type) {
            case "Батальон" -> ObjectType.BATTALION;
            case "Рота" -> ObjectType.COMPANY;
            case "Взвод" -> ObjectType.PLATOON;
            case "Отделение" -> ObjectType.SQUAD;
            default -> ObjectType.PLATOON;
        };
    }

    private void updateRank(Long personnelId, Long rankId, LocalDate assignmentDate) {
        if (rankId == null) {
            personnelRankRepository.deleteById(personnelId);
            return;
        }
        if (!militaryRankRepository.existsById(rankId)) {
            throw new EntityNotFoundException("Rank not found: " + rankId);
        }

        PersonnelRankEntity rank = personnelRankRepository.findById(personnelId)
                .orElseGet(PersonnelRankEntity::new);
        rank.setPersonnelId(personnelId);
        rank.setRankId(rankId);
        rank.setAssignmentDate(assignmentDate == null ? LocalDate.now() : assignmentDate);
        personnelRankRepository.save(rank);
    }

    private void replaceSpecialties(Long personnelId, Set<Long> specialtyIds) {
        personnelSpecialtyRepository.deleteByPersonnelId(personnelId);
        if (specialtyIds == null || specialtyIds.isEmpty()) {
            return;
        }

        for (Long specialtyId : specialtyIds) {
            if (!specialtyRepository.existsById(specialtyId)) {
                throw new EntityNotFoundException("Specialty not found: " + specialtyId);
            }
            PersonnelSpecialtyEntity entity = new PersonnelSpecialtyEntity();
            entity.setPersonnelId(personnelId);
            entity.setSpecialtyId(specialtyId);
            personnelSpecialtyRepository.save(entity);
        }
    }

    private void syncRankAttributes(Long personnelId, List<DynamicAttributeValueRequest> attributes) {
        if (attributes == null) {
            return;
        }
        for (DynamicAttributeValueRequest attribute : attributes) {
            String dataType = jdbcTemplate.queryForObject("""
                    SELECT data_type
                    FROM rank_attribute_types
                    WHERE attribute_id = :attributeId
                    """, java.util.Map.of("attributeId", attribute.attributeId()), String.class);
            if (attribute.value() == null || attribute.value().isBlank()) {
                jdbcTemplate.update("""
                        DELETE FROM rank_attribute_values
                        WHERE personnel_id = :personnelId AND attribute_id = :attributeId
                        """, java.util.Map.of("personnelId", personnelId, "attributeId", attribute.attributeId()));
                continue;
            }
            java.util.Map<String, Object> params = attributeParams(personnelId, attribute.attributeId(), dataType, attribute.value().trim());
            jdbcTemplate.update("""
                    INSERT INTO rank_attribute_values (personnel_id, attribute_id, value_text, value_number, value_date, value_boolean)
                    VALUES (:personnelId, :attributeId, :valueText, :valueNumber, :valueDate, :valueBoolean)
                    ON CONFLICT (personnel_id, attribute_id) DO UPDATE SET
                        value_text = EXCLUDED.value_text,
                        value_number = EXCLUDED.value_number,
                        value_date = EXCLUDED.value_date,
                        value_boolean = EXCLUDED.value_boolean
                    """, params);
        }
    }

    private java.util.Map<String, Object> attributeParams(Long personnelId, Long attributeId, String dataType, String value) {
        java.util.Map<String, Object> params = new HashMap<>();
        params.put("personnelId", personnelId);
        params.put("attributeId", attributeId);
        params.put("valueText", "text".equals(dataType) ? value : null);
        params.put("valueNumber", "number".equals(dataType) ? new BigDecimal(value.replace(',', '.')) : null);
        params.put("valueDate", "date".equals(dataType) ? LocalDate.parse(value) : null);
        params.put("valueBoolean", "boolean".equals(dataType) ? Boolean.valueOf(value) : null);
        return params;
    }

    private RankResponse toRankResponse(MilitaryRankEntity entity) {
        return new RankResponse(entity.getId(), entity.getName(), entity.getCategory());
    }

    private SpecialtyResponse toSpecialtyResponse(SpecialtyEntity entity) {
        return new SpecialtyResponse(entity.getId(), entity.getName());
    }
}
