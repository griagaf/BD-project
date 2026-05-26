package com.tacticaldistrict.command.intelligence.repository;

import java.util.Map;

public record QueryDefinition(String sql, Map<String, Object> parameters) {
}
