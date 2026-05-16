package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.PermissionAction;
import com.tacticaldistrict.command.user.service.UserContext;

public interface PermissionService {

    boolean canRead(UserContext user, ObjectType objectType, Long objectId);

    boolean canCreate(UserContext user, ObjectType parentType, Long parentId, ObjectType targetType);

    boolean canUpdate(UserContext user, ObjectType objectType, Long objectId);

    boolean canDelete(UserContext user, ObjectType objectType, Long objectId);

    boolean hasPermission(UserContext user, PermissionAction action, ObjectType objectType, Long objectId);

    void checkRead(UserContext user, ObjectType objectType, Long objectId);

    void checkCreate(UserContext user, ObjectType parentType, Long parentId, ObjectType targetType);

    void checkUpdate(UserContext user, ObjectType objectType, Long objectId);

    void checkDelete(UserContext user, ObjectType objectType, Long objectId);
}
