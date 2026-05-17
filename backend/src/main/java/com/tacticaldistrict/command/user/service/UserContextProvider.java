package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.UserPrincipal;
import com.tacticaldistrict.command.user.repository.PermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class UserContextProvider {

    public static final String ACCESS_SIMULATION_ROLE_HEADER = "X-Access-Simulation-Role";
    public static final String ACCESS_SIMULATION_OBJECT_TYPE_HEADER = "X-Access-Simulation-Object-Type";
    public static final String ACCESS_SIMULATION_OBJECT_ID_HEADER = "X-Access-Simulation-Object-Id";

    private final PermissionRepository permissionRepository;

    public UserContext current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return from(authentication, true);
    }

    public UserContext from(Authentication authentication) {
        return from(authentication, true);
    }

    public UserContext currentWithoutAccessSimulation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return from(authentication, false);
    }

    public UserContext from(Authentication authentication, boolean applyAccessSimulation) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }

        SimulationContext simulationContext = applyAccessSimulation ? simulationContext() : SimulationContext.disabled();
        Set<RoleCode> effectiveRoles = principal.roles();
        Set<String> effectivePermissions = principal.permissions();

        if (simulationContext.active()) {
            if (!principal.roles().contains(RoleCode.ADMIN_DISTRICT)) {
                throw new AccessDeniedException("Access simulation is available only for ADMIN_DISTRICT");
            }
            effectiveRoles = Set.of(simulationContext.role());
            effectivePermissions = permissionRepository.findPermissionCodesByRoles(effectiveRoles);
        }

        return new UserContext(
                principal.userId(),
                principal.soldierId(),
                principal.username(),
                principal.displayName(),
                effectiveRoles,
                effectivePermissions,
                simulationContext.active(),
                simulationContext.objectType(),
                simulationContext.objectId()
        );
    }

    private SimulationContext simulationContext() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return SimulationContext.disabled();
        }

        String roleValue = request.getHeader(ACCESS_SIMULATION_ROLE_HEADER);
        if (roleValue == null || roleValue.isBlank()) {
            return SimulationContext.disabled();
        }

        try {
            RoleCode role = RoleCode.valueOf(roleValue.trim());
            return new SimulationContext(
                    true,
                    role,
                    simulationObjectType(request),
                    simulationObjectId(request)
            );
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid access simulation context");
        }
    }

    private ObjectType simulationObjectType(HttpServletRequest request) {
        String value = request.getHeader(ACCESS_SIMULATION_OBJECT_TYPE_HEADER);
        if (value == null || value.isBlank()) {
            return null;
        }
        return ObjectType.from(value);
    }

    private Long simulationObjectId(HttpServletRequest request) {
        String value = request.getHeader(ACCESS_SIMULATION_OBJECT_ID_HEADER);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private record SimulationContext(
            boolean active,
            RoleCode role,
            ObjectType objectType,
            Long objectId
    ) {

        private static SimulationContext disabled() {
            return new SimulationContext(false, null, null, null);
        }
    }
}
