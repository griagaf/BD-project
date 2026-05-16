package com.tacticaldistrict.command.hierarchy.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.hierarchy.dto.ActionItemResponse;
import com.tacticaldistrict.command.hierarchy.dto.CommanderAssignmentRequest;
import com.tacticaldistrict.command.hierarchy.dto.FocusTreeResponse;
import com.tacticaldistrict.command.hierarchy.dto.FormationRequest;
import com.tacticaldistrict.command.hierarchy.dto.FormationResponse;
import com.tacticaldistrict.command.hierarchy.dto.HierarchyContextResponse;
import com.tacticaldistrict.command.hierarchy.dto.ObjectPassportResponse;
import com.tacticaldistrict.command.hierarchy.dto.SubdivisionRequest;
import com.tacticaldistrict.command.hierarchy.dto.SubdivisionResponse;
import com.tacticaldistrict.command.hierarchy.dto.TreeMode;
import com.tacticaldistrict.command.hierarchy.dto.TreeNodeDto;
import com.tacticaldistrict.command.hierarchy.entity.MilitaryFormationEntity;
import com.tacticaldistrict.command.hierarchy.entity.SubdivisionEntity;
import com.tacticaldistrict.command.hierarchy.repository.HierarchyQueryRepository;
import com.tacticaldistrict.command.hierarchy.repository.MilitaryFormationRepository;
import com.tacticaldistrict.command.hierarchy.repository.SubdivisionRepository;
import com.tacticaldistrict.command.personnel.repository.PersonnelRepository;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
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
public class HierarchyService {

    private static final List<String> FORMATION_TYPES = List.of("Округ", "Армия", "Корпус", "Дивизия", "Бригада");
    private static final List<String> SUBDIVISION_TYPES = List.of("Батальон", "Рота", "Взвод", "Отделение");

    private final MilitaryFormationRepository formationRepository;
    private final SubdivisionRepository subdivisionRepository;
    private final PersonnelRepository personnelRepository;
    private final HierarchyQueryRepository hierarchyQueryRepository;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TreeNodeDto> roots(TreeMode mode) {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("structure:read") && !user.hasPermission("personnel:read")) {
            throw new AccessDeniedException("Access denied");
        }
        return hierarchyQueryRepository.rootNodes(user)
                .stream()
                .filter(node -> canRead(user, node))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TreeNodeDto> children(String nodeType, Long nodeId) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = ObjectType.from(nodeType);
        permissionService.checkRead(user, objectType, nodeId);
        return hierarchyQueryRepository.children(objectType, nodeId)
                .stream()
                .filter(node -> canRead(user, node))
                .toList();
    }

    @Transactional(readOnly = true)
    public FocusTreeResponse focus(String nodeType, Long nodeId) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = ObjectType.from(nodeType);
        permissionService.checkRead(user, objectType, nodeId);
        TreeNodeDto selectedNode = hierarchyQueryRepository.findNode(objectType, nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchy object not found: " + nodeType + ":" + nodeId));
        return new FocusTreeResponse(
                TreeMode.FOCUS,
                selectedNode,
                hierarchyQueryRepository.breadcrumbPath(objectType, nodeId),
                filterVisible(user, hierarchyQueryRepository.siblings(objectType, nodeId)),
                filterVisible(user, hierarchyQueryRepository.children(objectType, nodeId))
        );
    }

    @Transactional(readOnly = true)
    public FocusTreeResponse personnelChain(Long personnelId) {
        UserContext user = userContextProvider.current();
        permissionService.checkRead(user, ObjectType.PERSONNEL, personnelId);
        List<TreeNodeDto> path = hierarchyQueryRepository.breadcrumbPath(ObjectType.PERSONNEL, personnelId);
        TreeNodeDto selected = path.isEmpty() ? null : path.get(path.size() - 1);
        return new FocusTreeResponse(TreeMode.CHAIN, selected, path, List.of(), List.of());
    }

    @Transactional(readOnly = true)
    public ObjectPassportResponse passport(String nodeType, Long nodeId) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = ObjectType.from(nodeType);
        permissionService.checkRead(user, objectType, nodeId);
        return hierarchyQueryRepository.passport(objectType, nodeId);
    }

    @Transactional(readOnly = true)
    public HierarchyContextResponse context(String nodeType, Long nodeId) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = ObjectType.from(nodeType);
        permissionService.checkRead(user, objectType, nodeId);
        ObjectPassportResponse passport = hierarchyQueryRepository.passport(objectType, nodeId);
        boolean canUpdate = permissionService.canUpdate(user, objectType, nodeId);
        boolean canCreateChild = user.hasRole(RoleCode.ADMIN_DISTRICT) || user.hasPermission("unit:create");
        boolean canAssignCommander = canAssignCommander(user) && canUpdate;
        return new HierarchyContextResponse(
                List.of(
                        new ActionItemResponse("OPEN_PASSPORT", "Open passport", passport.objectType() + ":" + passport.objectId(), true),
                        new ActionItemResponse("ASSIGN_COMMANDER", "Assign commander", passport.objectType() + ":" + passport.objectId(), canAssignCommander),
                        new ActionItemResponse("CREATE_CHILD", "Create child object", passport.objectType() + ":" + passport.objectId(), canCreateChild),
                        new ActionItemResponse("UNIT_PASSPORT", "Open unit passport", "/units/" + passport.objectId(), objectType == ObjectType.MILITARY_UNIT)
                ),
                List.of(),
                passport.metrics()
        );
    }

    @Transactional(readOnly = true)
    public List<FormationResponse> formations() {
        UserContext user = userContextProvider.current();
        return hierarchyQueryRepository.formations()
                .stream()
                .filter(formation -> permissionService.canRead(
                        user,
                        HierarchyQueryRepository.formationType(formation.formationType()),
                        formation.id()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FormationResponse formation(Long id) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.formationObjectType(id);
        permissionService.checkRead(user, objectType, id);
        return hierarchyQueryRepository.formation(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found: " + id));
    }

    @Transactional
    public FormationResponse createFormation(FormationRequest request) {
        validateFormationType(request.formationType());
        UserContext user = userContextProvider.current();
        ObjectType targetType = HierarchyQueryRepository.formationType(request.formationType());
        if (request.parentId() == null) {
            if (!user.hasRole(RoleCode.ADMIN_DISTRICT)) {
                throw new AccessDeniedException("Access denied");
            }
        } else {
            permissionService.checkCreate(user, hierarchyQueryRepository.formationObjectType(request.parentId()), request.parentId(), targetType);
        }
        validateCommander(user, request.commanderId(), request.commanderId() != null);

        MilitaryFormationEntity entity = new MilitaryFormationEntity();
        apply(request, entity);
        MilitaryFormationEntity saved = formationRepository.save(entity);
        auditService.created(user, targetType, saved.getId(), "Formation created");
        return formation(saved.getId());
    }

    @Transactional
    public FormationResponse updateFormation(Long id, FormationRequest request) {
        validateFormationType(request.formationType());
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.formationObjectType(id);
        permissionService.checkUpdate(user, objectType, id);
        MilitaryFormationEntity entity = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found: " + id));
        validateCommander(user, request.commanderId(), request.commanderId() != null && !request.commanderId().equals(entity.getCommanderId()));
        apply(request, entity);
        MilitaryFormationEntity saved = formationRepository.save(entity);
        auditService.updated(user, objectType, saved.getId(), "Formation updated");
        return formation(saved.getId());
    }

    @Transactional
    public void deleteFormation(Long id) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.formationObjectType(id);
        permissionService.checkDelete(user, objectType, id);
        if (!formationRepository.existsById(id)) {
            throw new EntityNotFoundException("Formation not found: " + id);
        }
        formationRepository.deleteById(id);
        auditService.deleted(user, objectType, id, "Formation deleted");
    }

    @Transactional
    public FormationResponse assignFormationCommander(Long id, CommanderAssignmentRequest request) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.formationObjectType(id);
        permissionService.checkUpdate(user, objectType, id);
        validateCommander(user, request.commanderId(), true);
        MilitaryFormationEntity entity = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found: " + id));
        entity.setCommanderId(request.commanderId());
        MilitaryFormationEntity saved = formationRepository.save(entity);
        auditService.updated(user, objectType, id, "Formation commander assigned: " + request.commanderId());
        return formation(saved.getId());
    }

    @Transactional
    public FormationResponse clearFormationCommander(Long id) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.formationObjectType(id);
        permissionService.checkUpdate(user, objectType, id);
        requireCommanderPermission(user);
        MilitaryFormationEntity entity = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation not found: " + id));
        entity.setCommanderId(null);
        MilitaryFormationEntity saved = formationRepository.save(entity);
        auditService.updated(user, objectType, id, "Formation commander cleared");
        return formation(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<SubdivisionResponse> subdivisions() {
        UserContext user = userContextProvider.current();
        return hierarchyQueryRepository.subdivisions()
                .stream()
                .filter(subdivision -> permissionService.canRead(
                        user,
                        HierarchyQueryRepository.subdivisionType(subdivision.type()),
                        subdivision.id()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubdivisionResponse subdivision(Long id) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.subdivisionObjectType(id);
        permissionService.checkRead(user, objectType, id);
        return hierarchyQueryRepository.subdivision(id)
                .orElseThrow(() -> new EntityNotFoundException("Subdivision not found: " + id));
    }

    @Transactional
    public SubdivisionResponse createSubdivision(SubdivisionRequest request) {
        validateSubdivisionType(request.type());
        UserContext user = userContextProvider.current();
        ObjectType targetType = HierarchyQueryRepository.subdivisionType(request.type());
        if (request.parentId() == null) {
            permissionService.checkCreate(user, ObjectType.MILITARY_UNIT, request.unitId(), targetType);
        } else {
            permissionService.checkCreate(user, hierarchyQueryRepository.subdivisionObjectType(request.parentId()), request.parentId(), targetType);
        }
        validateCommander(user, request.commanderId(), request.commanderId() != null);

        SubdivisionEntity entity = new SubdivisionEntity();
        apply(request, entity);
        SubdivisionEntity saved = subdivisionRepository.save(entity);
        auditService.created(user, targetType, saved.getId(), "Subdivision created");
        return subdivision(saved.getId());
    }

    @Transactional
    public SubdivisionResponse updateSubdivision(Long id, SubdivisionRequest request) {
        validateSubdivisionType(request.type());
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.subdivisionObjectType(id);
        permissionService.checkUpdate(user, objectType, id);
        SubdivisionEntity entity = subdivisionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subdivision not found: " + id));
        validateCommander(user, request.commanderId(), request.commanderId() != null && !request.commanderId().equals(entity.getCommanderId()));
        apply(request, entity);
        SubdivisionEntity saved = subdivisionRepository.save(entity);
        auditService.updated(user, objectType, saved.getId(), "Subdivision updated");
        return subdivision(saved.getId());
    }

    @Transactional
    public void deleteSubdivision(Long id) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.subdivisionObjectType(id);
        permissionService.checkDelete(user, objectType, id);
        if (!subdivisionRepository.existsById(id)) {
            throw new EntityNotFoundException("Subdivision not found: " + id);
        }
        subdivisionRepository.deleteById(id);
        auditService.deleted(user, objectType, id, "Subdivision deleted");
    }

    @Transactional
    public SubdivisionResponse assignSubdivisionCommander(Long id, CommanderAssignmentRequest request) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.subdivisionObjectType(id);
        permissionService.checkUpdate(user, objectType, id);
        validateCommander(user, request.commanderId(), true);
        SubdivisionEntity entity = subdivisionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subdivision not found: " + id));
        entity.setCommanderId(request.commanderId());
        SubdivisionEntity saved = subdivisionRepository.save(entity);
        auditService.updated(user, objectType, id, "Subdivision commander assigned: " + request.commanderId());
        return subdivision(saved.getId());
    }

    @Transactional
    public SubdivisionResponse clearSubdivisionCommander(Long id) {
        UserContext user = userContextProvider.current();
        ObjectType objectType = hierarchyQueryRepository.subdivisionObjectType(id);
        permissionService.checkUpdate(user, objectType, id);
        requireCommanderPermission(user);
        SubdivisionEntity entity = subdivisionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subdivision not found: " + id));
        entity.setCommanderId(null);
        SubdivisionEntity saved = subdivisionRepository.save(entity);
        auditService.updated(user, objectType, id, "Subdivision commander cleared");
        return subdivision(saved.getId());
    }

    private List<TreeNodeDto> filterVisible(UserContext user, List<TreeNodeDto> nodes) {
        return nodes.stream().filter(node -> canRead(user, node)).toList();
    }

    private boolean canRead(UserContext user, TreeNodeDto node) {
        return permissionService.canRead(user, ObjectType.from(node.type()), node.objectId());
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
        if (!canAssignCommander(user)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private boolean canAssignCommander(UserContext user) {
        return user.hasRole(RoleCode.ADMIN_DISTRICT) || user.hasPermission("commander:assign");
    }

    private void validateFormationType(String formationType) {
        if (!FORMATION_TYPES.contains(formationType)) {
            throw new IllegalArgumentException("Unsupported formation type: " + formationType);
        }
    }

    private void validateSubdivisionType(String subdivisionType) {
        if (!SUBDIVISION_TYPES.contains(subdivisionType)) {
            throw new IllegalArgumentException("Unsupported subdivision type: " + subdivisionType);
        }
    }

    private void apply(FormationRequest request, MilitaryFormationEntity entity) {
        entity.setName(request.name());
        entity.setFormationType(request.formationType());
        entity.setParentId(request.parentId());
        entity.setFormationDate(request.formationDate());
        entity.setStatus(request.status() == null || request.status().isBlank() ? "Активна" : request.status());
        entity.setCommanderId(request.commanderId());
    }

    private void apply(SubdivisionRequest request, SubdivisionEntity entity) {
        entity.setName(request.name());
        entity.setType(request.type());
        entity.setUnitId(request.unitId());
        entity.setParentId(request.parentId());
        entity.setCommanderId(request.commanderId());
    }
}
