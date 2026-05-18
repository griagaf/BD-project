package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserScopeResolver {

    private final CommandAssignmentResolver commandAssignmentResolver;

    @Transactional(readOnly = true)
    public UserScope resolve(UserContext user) {
        boolean unrestricted = !user.hasSimulationScope()
                && user.hasAnyRole(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST);
        boolean selfOnly = user.hasRole(RoleCode.SOLDIER) && user.roles().size() == 1;
        Long scopeSoldierId = user.hasSimulationScope()
                ? simulatedSoldierId(user)
                : user.soldierId();
        List<CommandAssignmentScope> assignments = user.hasSimulationScope()
                ? List.of(new CommandAssignmentScope(
                null,
                scopeSoldierId,
                user.simulationScopeType(),
                user.simulationScopeId(),
                LocalDate.now(),
                null,
                true
        ))
                : commandAssignmentResolver.resolveActiveAssignments(user.soldierId());

        return new UserScope(
                user.userId(),
                scopeSoldierId,
                unrestricted,
                selfOnly,
                assignments
        );
    }

    private Long simulatedSoldierId(UserContext user) {
        return user.simulationScopeType() == ObjectType.SELF
                ? user.simulationScopeId()
                : null;
    }
}
