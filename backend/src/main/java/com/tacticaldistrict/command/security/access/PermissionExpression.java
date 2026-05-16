package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("permissionExpression")
@RequiredArgsConstructor
public class PermissionExpression {

    private final PermissionService permissionService;
    private final UserContextProvider userContextProvider;

    public boolean canCreate(Long parentId, String parentType, String targetType) {
        UserContext user = userContextProvider.current();
        return permissionService.canCreate(
                user,
                ObjectType.from(parentType),
                parentId,
                ObjectType.from(targetType)
        );
    }
}
