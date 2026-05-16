package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.security.model.RoleCode;
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

    private static final String ACCESS_SIMULATION_HEADER = "X-Access-Simulation-Role";

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

        RoleCode simulationRole = applyAccessSimulation ? simulationRole() : null;
        Set<RoleCode> effectiveRoles = principal.roles();
        Set<String> effectivePermissions = principal.permissions();

        if (simulationRole != null) {
            if (!principal.roles().contains(RoleCode.ADMIN_DISTRICT)) {
                throw new AccessDeniedException("Access simulation is available only for ADMIN_DISTRICT");
            }
            effectiveRoles = Set.of(simulationRole);
            effectivePermissions = permissionRepository.findPermissionCodesByRoles(effectiveRoles);
        }

        return new UserContext(
                principal.userId(),
                principal.soldierId(),
                principal.username(),
                principal.displayName(),
                effectiveRoles,
                effectivePermissions
        );
    }

    private RoleCode simulationRole() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String value = request.getHeader(ACCESS_SIMULATION_HEADER);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return RoleCode.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid access simulation role");
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
