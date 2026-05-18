package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.audit.AuditService;
import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.dto.RoleResponse;
import com.tacticaldistrict.command.user.dto.UserAdminResponse;
import com.tacticaldistrict.command.user.dto.UserCreateRequest;
import com.tacticaldistrict.command.user.dto.UserUpdateRequest;
import com.tacticaldistrict.command.user.entity.RoleEntity;
import com.tacticaldistrict.command.user.entity.UserEntity;
import com.tacticaldistrict.command.user.entity.UserRoleEntity;
import com.tacticaldistrict.command.user.repository.RoleRepository;
import com.tacticaldistrict.command.user.repository.UserRepository;
import com.tacticaldistrict.command.user.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final UserContextProvider userContextProvider;

    @Transactional(readOnly = true)
    public PageResponse<UserAdminResponse> search(String search, Boolean active, int page, int size) {
        Page<UserEntity> users = userRepository.search(
                normalize(search),
                active,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("username").ascending())
        );
        return new PageResponse<>(
                users.getContent().stream().map(this::toResponse).toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isFirst(),
                users.isLast()
        );
    }

    @Transactional(readOnly = true)
    public UserAdminResponse get(Long id) {
        return toResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(role -> role.getCode().name()))
                .map(role -> new RoleResponse(role.getId(), role.getCode().name(), role.getName(), role.getDescription()))
                .toList();
    }

    @Transactional
    public UserAdminResponse create(UserCreateRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new DataIntegrityViolationException("Username already exists");
        }
        Instant now = Instant.now();
        UserEntity user = new UserEntity();
        user.setUsername(request.username().trim());
        user.setDisplayName(request.displayName().trim());
        user.setPersonnelId(request.personnelId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(request.active() == null || request.active());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        UserEntity saved = userRepository.save(user);
        replaceRoles(saved.getId(), request.roles());
        auditService.created(userContextProvider.current(), ObjectType.USER, saved.getId(), "User created");
        return toResponse(saved);
    }

    @Transactional
    public UserAdminResponse update(Long id, UserUpdateRequest request) {
        UserEntity user = findUser(id);
        user.setDisplayName(request.displayName().trim());
        user.setPersonnelId(request.personnelId());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.active() != null) {
            ensureNotLastActiveAdmin(id, request.active(), request.roles());
            user.setActive(request.active());
        }
        user.setUpdatedAt(Instant.now());
        replaceRoles(id, request.roles());
        auditService.updated(userContextProvider.current(), ObjectType.USER, id, "User updated");
        return toResponse(user);
    }

    @Transactional
    public UserAdminResponse activate(Long id) {
        UserEntity user = findUser(id);
        user.setActive(true);
        user.setUpdatedAt(Instant.now());
        auditService.updated(userContextProvider.current(), ObjectType.USER, id, "User activated");
        return toResponse(user);
    }

    @Transactional
    public UserAdminResponse deactivate(Long id) {
        UserEntity user = findUser(id);
        ensureNotLastActiveAdmin(id, false, null);
        user.setActive(false);
        user.setUpdatedAt(Instant.now());
        auditService.updated(userContextProvider.current(), ObjectType.USER, id, "User deactivated");
        return toResponse(user);
    }

    private void replaceRoles(Long userId, Set<String> requestedRoles) {
        if (requestedRoles == null) {
            return;
        }
        ensureNotLastActiveAdmin(userId, null, requestedRoles);
        userRoleRepository.deleteByUserId(userId);
        for (RoleCode roleCode : parseRoles(requestedRoles)) {
            RoleEntity role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleCode));
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setAssignedAt(Instant.now());
            userRoleRepository.save(userRole);
        }
    }

    private Set<RoleCode> parseRoles(Set<String> roles) {
        Set<RoleCode> parsed = new LinkedHashSet<>();
        for (String role : roles) {
            parsed.add(RoleCode.valueOf(role));
        }
        return parsed;
    }

    private void ensureNotLastActiveAdmin(Long userId, Boolean active, Set<String> roles) {
        UserEntity user = findUser(userId);
        boolean currentlyAdmin = userRoleRepository.findRoleCodesByUserId(userId).contains(RoleCode.ADMIN_DISTRICT);
        boolean remainsAdmin = roles == null ? currentlyAdmin : roles.contains(RoleCode.ADMIN_DISTRICT.name());
        boolean remainsActive = active == null ? user.isActive() : active;
        if (!currentlyAdmin || (remainsAdmin && remainsActive)) {
            return;
        }
        long activeAdmins = userRepository.findAll().stream()
                .filter(UserEntity::isActive)
                .filter(candidate -> userRoleRepository.findRoleCodesByUserId(candidate.getId()).contains(RoleCode.ADMIN_DISTRICT))
                .count();
        if (activeAdmins <= 1) {
            throw new DataIntegrityViolationException("At least one active district administrator is required");
        }
    }

    private UserAdminResponse toResponse(UserEntity user) {
        Set<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId()).stream()
                .map(RoleCode::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPersonnelId(),
                user.isActive(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserEntity findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
