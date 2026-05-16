package com.tacticaldistrict.command.security.web;

import com.tacticaldistrict.command.security.dto.AccessDecisionResponse;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.PermissionAction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
public class AccessControlController {

    @GetMapping("/check")
    @PreAuthorize("hasPermission(#objectId, #objectType, #action)")
    public AccessDecisionResponse check(
            @RequestParam ObjectType objectType,
            @RequestParam Long objectId,
            @RequestParam PermissionAction action
    ) {
        return new AccessDecisionResponse(objectType, objectId, action, true);
    }
}
