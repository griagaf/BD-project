package com.tacticaldistrict.command.audit;

import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void created(UserContext actor, ObjectType objectType, Long objectId, String details) {
        record(actor, "CREATE", objectType, objectId, details);
    }

    public void updated(UserContext actor, ObjectType objectType, Long objectId, String details) {
        record(actor, "UPDATE", objectType, objectId, details);
    }

    public void deleted(UserContext actor, ObjectType objectType, Long objectId, String details) {
        record(actor, "DELETE", objectType, objectId, details);
    }

    private void record(UserContext actor, String action, ObjectType objectType, Long objectId, String details) {
        jdbcTemplate.update("""
                INSERT INTO audit_events (
                    actor_user_id,
                    actor_username,
                    action,
                    object_type,
                    object_id,
                    details
                )
                VALUES (
                    :actorUserId,
                    :actorUsername,
                    :action,
                    :objectType,
                    :objectId,
                    :details
                )
                """, Map.of(
                "actorUserId", actor.userId(),
                "actorUsername", actor.username(),
                "action", action,
                "objectType", objectType.name(),
                "objectId", objectId,
                "details", details == null ? "" : details
        ));
    }
}
