package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.PermissionAction;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultPermissionService implements PermissionService {

    private final AccessControlService accessControlService;
    private final PermissionCodeResolver permissionCodeResolver;

    @Override
    public boolean canRead(UserContext user, ObjectType objectType, Long objectId) {
        return hasPermission(user, PermissionAction.READ, objectType, objectId);
    }

    @Override
    public boolean canCreate(UserContext user, ObjectType parentType, Long parentId, ObjectType targetType) {
        if (!hasRolePermission(user, PermissionAction.CREATE, targetType)) {
            return false;
        }
        return accessControlService.isInScope(user, parentType, parentId);
    }

    @Override
    public boolean canUpdate(UserContext user, ObjectType objectType, Long objectId) {
        return hasPermission(user, PermissionAction.UPDATE, objectType, objectId);
    }

    @Override
    public boolean canDelete(UserContext user, ObjectType objectType, Long objectId) {
        return hasPermission(user, PermissionAction.DELETE, objectType, objectId);
    }

    @Override
    public boolean hasPermission(UserContext user, PermissionAction action, ObjectType objectType, Long objectId) {
        return hasRolePermission(user, action, objectType)
                && accessControlService.isInScope(user, objectType, objectId);
    }

    @Override
    public void checkRead(UserContext user, ObjectType objectType, Long objectId) {
        check(canRead(user, objectType, objectId));
    }

    @Override
    public void checkCreate(UserContext user, ObjectType parentType, Long parentId, ObjectType targetType) {
        check(canCreate(user, parentType, parentId, targetType));
    }

    @Override
    public void checkUpdate(UserContext user, ObjectType objectType, Long objectId) {
        check(canUpdate(user, objectType, objectId));
    }

    @Override
    public void checkDelete(UserContext user, ObjectType objectType, Long objectId) {
        check(canDelete(user, objectType, objectId));
    }

    private boolean hasRolePermission(UserContext user, PermissionAction action, ObjectType objectType) {
        if (user.hasRole(RoleCode.ADMIN_DISTRICT)) {
            return true;
        }

        return permissionCodeResolver.resolve(action, objectType)
                .map(user::hasPermission)
                .orElse(false);
    }

    private void check(boolean allowed) {
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
