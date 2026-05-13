package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.security.model.UserPrincipal;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContextProvider {

    public UserContext current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }

        return new UserContext(
                principal.userId(),
                principal.soldierId(),
                principal.username(),
                principal.displayName(),
                principal.roles(),
                principal.permissions()
        );
    }
}

