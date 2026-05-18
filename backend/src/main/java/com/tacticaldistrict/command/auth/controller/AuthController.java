package com.tacticaldistrict.command.auth.controller;

import com.tacticaldistrict.command.auth.dto.AccessSimulationRequest;
import com.tacticaldistrict.command.auth.dto.CurrentUserResponse;
import com.tacticaldistrict.command.auth.dto.LoginRequest;
import com.tacticaldistrict.command.auth.dto.LogoutRequest;
import com.tacticaldistrict.command.auth.dto.RefreshRequest;
import com.tacticaldistrict.command.auth.dto.TokenResponse;
import com.tacticaldistrict.command.auth.service.AuthService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.refresh(request, httpRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(
            @RequestHeader(name = UserContextProvider.ACCESS_SIMULATION_ROLE_HEADER, required = false) RoleCode simulationRole,
            @RequestHeader(name = UserContextProvider.ACCESS_SIMULATION_OBJECT_TYPE_HEADER, required = false) ObjectType simulationObjectType,
            @RequestHeader(name = UserContextProvider.ACCESS_SIMULATION_OBJECT_ID_HEADER, required = false) Long simulationObjectId
    ) {
        return authService.currentUser(new AccessSimulationRequest(simulationRole, simulationObjectType, simulationObjectId));
    }

    @PostMapping("/simulation/preview")
    public CurrentUserResponse simulationPreview(@Valid @RequestBody AccessSimulationRequest request) {
        return authService.simulationPreview(request);
    }
}
