package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import java.util.Collection;
import java.util.Set;

public interface AccessControlService {

    boolean isInScope(UserContext user, ObjectType objectType, Long objectId);

    Set<Long> filterIdsInScope(UserContext user, ObjectType objectType, Collection<Long> objectIds);

    ScopedQuery applyDirectScopeFilter(UserContext user, String sql, ObjectType objectType, String idColumn);
}
