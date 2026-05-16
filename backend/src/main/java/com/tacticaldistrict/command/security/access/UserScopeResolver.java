package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserScopeResolver {

    private final CommandAssignmentResolver commandAssignmentResolver;

    @Transactional(readOnly = true)
    public UserScope resolve(UserContext user) {
        boolean unrestricted = user.hasAnyRole(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST);
        boolean selfOnly = user.hasRole(RoleCode.SOLDIER) && user.roles().size() == 1;

        return new UserScope(
                user.userId(),
                user.soldierId(),
                unrestricted,
                selfOnly,
                commandAssignmentResolver.resolveActiveAssignments(user.soldierId())
        );
    }
}
