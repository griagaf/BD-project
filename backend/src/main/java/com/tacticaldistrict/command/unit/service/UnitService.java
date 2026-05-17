package com.tacticaldistrict.command.unit.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.hierarchy.dto.CommanderAssignmentRequest;
import com.tacticaldistrict.command.hierarchy.dto.ObjectPassportResponse;
import com.tacticaldistrict.command.hierarchy.repository.HierarchyQueryRepository;
import com.tacticaldistrict.command.personnel.repository.PersonnelRepository;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.unit.dto.UnitRequest;
import com.tacticaldistrict.command.unit.dto.UnitResponse;
import com.tacticaldistrict.command.unit.entity.MilitaryUnitEntity;
import com.tacticaldistrict.command.unit.repository.MilitaryUnitRepository;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final MilitaryUnitRepository unitRepository;
    private final PersonnelRepository personnelRepository;
    private final HierarchyQueryRepository hierarchyQueryRepository;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UnitResponse> units() {
        UserContext user = userContextProvider.current();
        return hierarchyQueryRepository.units()
                .stream()
                .filter(unit -> permissionService.canRead(user, ObjectType.MILITARY_UNIT, unit.id()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UnitResponse unit(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.MILITARY_UNIT, id);
        return hierarchyQueryRepository.unit(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
    }

    @Transactional(readOnly = true)
    public ObjectPassportResponse passport(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.MILITARY_UNIT, id);
        return hierarchyQueryRepository.passport(ObjectType.MILITARY_UNIT, id);
    }

    @Transactional
    public UnitResponse create(UnitRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkCreate(
                user,
                hierarchyQueryRepository.formationObjectType(request.formationId()),
                request.formationId(),
                ObjectType.MILITARY_UNIT
        );
        validateCommander(user, request.commanderId(), request.commanderId() != null);
        MilitaryUnitEntity entity = new MilitaryUnitEntity();
        apply(request, entity);
        MilitaryUnitEntity saved = unitRepository.save(entity);
        auditService.created(user, ObjectType.MILITARY_UNIT, saved.getId(), "Military unit created");
        return unit(saved.getId());
    }

    @Transactional
    public UnitResponse update(Long id, UnitRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.MILITARY_UNIT, id);
        MilitaryUnitEntity entity = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        validateCommander(user, request.commanderId(), request.commanderId() != null && !request.commanderId().equals(entity.getCommanderId()));
        apply(request, entity);
        MilitaryUnitEntity saved = unitRepository.save(entity);
        auditService.updated(user, ObjectType.MILITARY_UNIT, saved.getId(), "Military unit updated");
        return unit(saved.getId());
    }

    @Transactional
    public void delete(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkDelete(user, ObjectType.MILITARY_UNIT, id);
        if (!unitRepository.existsById(id)) {
            throw new EntityNotFoundException("Unit not found: " + id);
        }
        unitRepository.deleteById(id);
        auditService.deleted(user, ObjectType.MILITARY_UNIT, id, "Military unit deleted");
    }

    @Transactional
    public UnitResponse assignCommander(Long id, CommanderAssignmentRequest request) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.MILITARY_UNIT, id);
        validateCommander(user, request.commanderId(), true);
        MilitaryUnitEntity entity = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        entity.setCommanderId(request.commanderId());
        MilitaryUnitEntity saved = unitRepository.save(entity);
        auditService.updated(user, ObjectType.MILITARY_UNIT, id, "Military unit commander assigned: " + request.commanderId());
        return unit(saved.getId());
    }

    @Transactional
    public UnitResponse clearCommander(Long id) {
        UserContext user = userContextProvider.current();
        permissionService.checkUpdate(user, ObjectType.MILITARY_UNIT, id);
        requireCommanderPermission(user);
        MilitaryUnitEntity entity = unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Unit not found: " + id));
        entity.setCommanderId(null);
        MilitaryUnitEntity saved = unitRepository.save(entity);
        auditService.updated(user, ObjectType.MILITARY_UNIT, id, "Military unit commander cleared");
        return unit(saved.getId());
    }

    private void validateCommander(UserContext user, Long commanderId, boolean commanderChange) {
        if (commanderId != null && !personnelRepository.existsById(commanderId)) {
            throw new EntityNotFoundException("Commander not found: " + commanderId);
        }
        if (commanderChange) {
            requireCommanderPermission(user);
        }
    }

    private void requireCommanderPermission(UserContext user) {
        if (!user.hasRole(RoleCode.ADMIN_DISTRICT) && !user.hasPermission("commander:assign")) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void apply(UnitRequest request, MilitaryUnitEntity entity) {
        entity.setName(request.name());
        entity.setFormationId(request.formationId());
        entity.setLocationId(request.locationId());
        entity.setCommanderId(request.commanderId());
    }
}
