package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.PermissionAction;
import com.tacticaldistrict.command.security.model.UserPrincipal;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TacticalPermissionEvaluator implements PermissionEvaluator {

    private final PermissionService permissionService;
    private final UserContextProvider userContextProvider;

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetDomainObject,
            Object permission
    ) {
        if (targetDomainObject instanceof AccessTarget target) {
            return evaluate(authentication, target.objectId(), target.objectType(), permission);
        }
        return false;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission
    ) {
        return evaluate(authentication, targetId, targetType, permission);
    }

    private boolean evaluate(
            Authentication authentication,
            Object targetId,
            Object targetType,
            Object permission
    ) {
        if (!isAuthenticated(authentication) || targetId == null || targetType == null || permission == null) {
            return false;
        }

        try {
            UserContext user = userContextProvider.from(authentication);
            ObjectType objectType = ObjectType.from(targetType.toString());
            PermissionAction action = PermissionAction.from(permission.toString());
            Long objectId = Long.valueOf(targetId.toString());
            return permissionService.hasPermission(user, action, objectType, objectId);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal;
    }

    public record AccessTarget(
            Long objectId,
            String objectType
    ) {
    }
}
