package com.tacticaldistrict.command.auth.service;

import com.tacticaldistrict.command.auth.dto.AccessSimulationRequest;
import com.tacticaldistrict.command.auth.dto.CurrentUserResponse;
import com.tacticaldistrict.command.auth.dto.LoginRequest;
import com.tacticaldistrict.command.auth.dto.LogoutRequest;
import com.tacticaldistrict.command.auth.dto.RefreshRequest;
import com.tacticaldistrict.command.auth.dto.TokenResponse;
import com.tacticaldistrict.command.security.config.JwtProperties;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.UserPrincipal;
import com.tacticaldistrict.command.security.service.JwtService;
import com.tacticaldistrict.command.user.dto.CommandAssignmentResponse;
import com.tacticaldistrict.command.user.entity.CommandAssignmentEntity;
import com.tacticaldistrict.command.user.repository.CommandAssignmentRepository;
import com.tacticaldistrict.command.user.repository.PermissionRepository;
import com.tacticaldistrict.command.user.service.TacticalUserDetailsService;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TacticalUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final UserContextProvider userContextProvider;
    private final CommandAssignmentRepository commandAssignmentRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.username());
        String accessToken = jwtService.createAccessToken(principal);
        String refreshToken = refreshTokenService.create(
                principal.userId(),
                userAgent(httpRequest),
                ipAddress(httpRequest)
        );

        return new TokenResponse(
                accessToken,
                refreshToken,
                jwtProperties.accessTtlMinutes() * 60,
                currentUser(principal, null)
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request, HttpServletRequest httpRequest) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                request.refreshToken(),
                userAgent(httpRequest),
                ipAddress(httpRequest)
        );
        String accessToken = jwtService.createAccessToken(rotation.principal());

        return new TokenResponse(
                accessToken,
                rotation.refreshToken(),
                jwtProperties.accessTtlMinutes() * 60,
                currentUser(rotation.principal(), null)
        );
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(AccessSimulationRequest simulationRequest) {
        UserContext user = userContextProvider.currentWithoutAccessSimulation();
        if (simulationRequest.role() != null && !user.hasRole(RoleCode.ADMIN_DISTRICT)) {
            throw new AccessDeniedException("Access simulation is available only for ADMIN_DISTRICT");
        }

        Set<RoleCode> effectiveRoles = simulationRequest.role() == null
                ? user.roles()
                : Set.of(simulationRequest.role());
        Set<String> permissions = effectiveRoles.isEmpty()
                ? Set.of()
                : permissionRepository.findPermissionCodesByRoles(effectiveRoles);

        List<CommandAssignmentResponse> assignments = simulationRequest.role() == null
                ? assignments(user.soldierId())
                : simulationAssignments(user.soldierId(), simulationRequest);

        return new CurrentUserResponse(
                user.userId(),
                user.soldierId(),
                user.username(),
                user.displayName(),
                toNames(user.roles()),
                toNames(effectiveRoles),
                assignments,
                permissions,
                simulationRequest.role() != null
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse simulationPreview(AccessSimulationRequest request) {
        return currentUser(request);
    }

    private CurrentUserResponse currentUser(UserPrincipal principal, RoleCode simulationRole) {
        Set<RoleCode> effectiveRoles = simulationRole == null
                ? principal.roles()
                : Set.of(simulationRole);
        Set<String> permissions = effectiveRoles.isEmpty()
                ? Set.of()
                : permissionRepository.findPermissionCodesByRoles(effectiveRoles);

        return new CurrentUserResponse(
                principal.userId(),
                principal.soldierId(),
                principal.username(),
                principal.displayName(),
                toNames(principal.roles()),
                toNames(effectiveRoles),
                assignments(principal.soldierId()),
                permissions,
                simulationRole != null
        );
    }

    private List<CommandAssignmentResponse> assignments(Long soldierId) {
        if (soldierId == null) {
            return List.of();
        }

        return commandAssignmentRepository.findActiveBySoldierId(soldierId, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private List<CommandAssignmentResponse> simulationAssignments(Long soldierId, AccessSimulationRequest request) {
        if (request.objectType() == null || request.objectId() == null) {
            return List.of();
        }

        return List.of(new CommandAssignmentResponse(
                null,
                request.objectType().name(),
                request.objectId(),
                LocalDate.now(),
                null,
                true
        ));
    }

    private CommandAssignmentResponse toResponse(CommandAssignmentEntity entity) {
        return new CommandAssignmentResponse(
                entity.getId(),
                entity.getObjectType(),
                entity.getObjectId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.isPrimary()
        );
    }

    private Set<String> toNames(Collection<RoleCode> roles) {
        return roles.stream()
                .map(RoleCode::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String ipAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
