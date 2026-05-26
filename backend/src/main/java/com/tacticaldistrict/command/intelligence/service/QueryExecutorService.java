package com.tacticaldistrict.command.intelligence.service;

import com.tacticaldistrict.command.intelligence.dto.ExecuteQueryRequest;
import com.tacticaldistrict.command.intelligence.dto.QueryResultDto;
import com.tacticaldistrict.command.intelligence.dto.QueryScopeDto;
import com.tacticaldistrict.command.intelligence.application.port.QueryDefinitionPort;
import com.tacticaldistrict.command.intelligence.application.port.QueryExecutionPort;
import com.tacticaldistrict.command.intelligence.mapper.QueryResultMapper;
import com.tacticaldistrict.command.intelligence.model.QueryTemplate;
import com.tacticaldistrict.command.intelligence.repository.QueryDefinition;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueryExecutorService {

    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final QueryParameterResolver parameterResolver;
    private final QueryDefinitionPort queryDefinitionPort;
    private final QueryExecutionPort queryExecutionPort;
    private final QueryResultMapper queryResultMapper;

    @Transactional(readOnly = true)
    public QueryResultDto execute(QueryTemplate template, ExecuteQueryRequest request) {
        UserContext user = userContextProvider.current();
        checkRolePermissions(user, template);
        parameterResolver.validate(template, request);
        checkScope(user, request.scope());

        QueryDefinition query = queryDefinitionPort.build(template, request);
        List<Map<String, Object>> rows = queryExecutionPort.execute(query)
                .stream()
                .filter(row -> rowInScope(user, row))
                .toList();
        return queryResultMapper.toResult(template, preview(template, request), rows);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportCsv(QueryTemplate template, ExecuteQueryRequest request) {
        QueryResultDto result = execute(template, request);
        byte[] bytes = queryResultMapper.toCsv(result);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(template.name().toLowerCase() + ".csv")
                        .build()
                        .toString())
                .body(bytes);
    }

    private void checkRolePermissions(UserContext user, QueryTemplate template) {
        boolean allowed = template.requiredPermissions().stream().allMatch(user::hasPermission);
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void checkScope(UserContext user, QueryScopeDto scope) {
        if (scope == null || scope.type() == null || scope.id() == null) {
            if (!user.hasAnyRole(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST)) {
                throw new AccessDeniedException("Explicit query scope is required");
            }
            return;
        }
        if ("GLOBAL".equalsIgnoreCase(scope.type())) {
            if (!user.hasAnyRole(RoleCode.ADMIN_DISTRICT, RoleCode.STAFF_ANALYST)) {
                throw new AccessDeniedException("Global query scope is not allowed");
            }
            return;
        }
        ObjectType objectType = objectType(scope.type());
        permissionService.checkRead(user, objectType, scope.id());
    }

    private boolean rowInScope(UserContext user, Map<String, Object> row) {
        Map<String, Object> normalized = new java.util.LinkedHashMap<>();
        row.forEach((key, value) -> normalized.put(key.toLowerCase(java.util.Locale.ROOT), value));
        if (normalized.containsKey("personnel_id") && normalized.get("personnel_id") != null) {
            return permissionService.canRead(user, ObjectType.PERSONNEL, longValue(normalized.get("personnel_id")));
        }
        if (normalized.containsKey("building_id") && normalized.get("building_id") != null) {
            return permissionService.canRead(user, ObjectType.BUILDING, longValue(normalized.get("building_id")));
        }
        if (normalized.containsKey("unit_id") && normalized.get("unit_id") != null) {
            return permissionService.canRead(user, ObjectType.MILITARY_UNIT, longValue(normalized.get("unit_id")));
        }
        if (normalized.containsKey("formation_id") && normalized.get("formation_id") != null) {
            return permissionService.canRead(user, ObjectType.FORMATION, longValue(normalized.get("formation_id")));
        }
        return true;
    }

    private ObjectType objectType(String type) {
        String normalized = type.trim().toUpperCase(java.util.Locale.ROOT);
        return "UNIT".equals(normalized) ? ObjectType.MILITARY_UNIT : ObjectType.from(normalized);
    }

    private String preview(QueryTemplate template, ExecuteQueryRequest request) {
        if (request != null && request.previewCommand() != null && !request.previewCommand().isBlank()) {
            return request.previewCommand();
        }
        return template.metadata().exampleCommand();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
