package com.tacticaldistrict.command.security.access;

import java.util.Map;

public record ScopedQuery(
        String sql,
        Map<String, ?> parameters
) {
}
