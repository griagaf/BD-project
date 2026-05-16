package com.tacticaldistrict.command.security.access;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.PermissionAction;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PermissionCodeResolver {

    public Optional<String> resolve(PermissionAction action, ObjectType objectType) {
        return switch (objectType) {
            case DISTRICT, FORMATION, ARMY, CORPS, DIVISION, BRIGADE, BATTALION, COMPANY, PLATOON, SQUAD ->
                    structurePermission(action);
            case MILITARY_UNIT -> crudPermission("unit", action);
            case PERSONNEL, SELF -> crudPermission("personnel", action);
            case EQUIPMENT -> resourcePermission("equipment", action);
            case WEAPON -> resourcePermission("weapon", action);
            case BUILDING -> resourcePermission("building", action);
            case SPECIALTY -> readOnlyPermission("specialty", action);
            case QUERY -> action == PermissionAction.EXECUTE
                    ? Optional.of("query:execute")
                    : Optional.empty();
            case ALERT -> alertPermission(action);
            case REPORT -> reportPermission(action);
            case USER -> action == PermissionAction.CREATE
                    || action == PermissionAction.UPDATE
                    || action == PermissionAction.DELETE
                    || action == PermissionAction.READ
                    ? Optional.of("user:manage")
                    : Optional.empty();
            case COMMAND_ASSIGNMENT -> action == PermissionAction.ASSIGN
                    || action == PermissionAction.CREATE
                    || action == PermissionAction.UPDATE
                    || action == PermissionAction.DELETE
                    ? Optional.of("commander:assign")
                    : Optional.empty();
        };
    }

    private Optional<String> crudPermission(String prefix, PermissionAction action) {
        return switch (action) {
            case READ -> Optional.of(prefix + ":read");
            case CREATE -> Optional.of(prefix + ":create");
            case UPDATE -> Optional.of(prefix + ":update");
            case DELETE -> Optional.of(prefix + ":delete");
            default -> Optional.empty();
        };
    }

    private Optional<String> resourcePermission(String prefix, PermissionAction action) {
        return switch (action) {
            case READ -> Optional.of(prefix + ":read");
            case UPDATE -> Optional.of(prefix + ":update");
            default -> Optional.empty();
        };
    }

    private Optional<String> readOnlyPermission(String prefix, PermissionAction action) {
        return action == PermissionAction.READ
                ? Optional.of(prefix + ":read")
                : Optional.empty();
    }

    private Optional<String> structurePermission(PermissionAction action) {
        return switch (action) {
            case READ -> Optional.of("structure:read");
            case UPDATE -> Optional.of("unit:update");
            case CREATE -> Optional.of("unit:create");
            case DELETE -> Optional.of("unit:delete");
            default -> Optional.empty();
        };
    }

    private Optional<String> alertPermission(PermissionAction action) {
        return switch (action) {
            case READ -> Optional.of("alert:read");
            case UPDATE -> Optional.of("alert:update");
            default -> Optional.empty();
        };
    }

    private Optional<String> reportPermission(PermissionAction action) {
        return switch (action) {
            case READ -> Optional.of("report:read");
            case GENERATE, CREATE -> Optional.of("report:generate");
            default -> Optional.empty();
        };
    }
}
