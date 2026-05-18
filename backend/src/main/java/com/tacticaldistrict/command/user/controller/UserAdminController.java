package com.tacticaldistrict.command.user.controller;

import com.tacticaldistrict.command.common.dto.PageResponse;
import com.tacticaldistrict.command.user.dto.RoleResponse;
import com.tacticaldistrict.command.user.dto.UserAdminResponse;
import com.tacticaldistrict.command.user.dto.UserCreateRequest;
import com.tacticaldistrict.command.user.dto.UserUpdateRequest;
import com.tacticaldistrict.command.user.service.UserAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_DISTRICT')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public PageResponse<UserAdminResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userAdminService.search(search, active, page, size);
    }

    @GetMapping("/{id}")
    public UserAdminResponse get(@PathVariable Long id) {
        return userAdminService.get(id);
    }

    @GetMapping("/roles")
    public List<RoleResponse> roles() {
        return userAdminService.roles();
    }

    @PostMapping
    public UserAdminResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userAdminService.create(request);
    }

    @PutMapping("/{id}")
    public UserAdminResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userAdminService.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public UserAdminResponse activate(@PathVariable Long id) {
        return userAdminService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public UserAdminResponse deactivate(@PathVariable Long id) {
        return userAdminService.deactivate(id);
    }

    @DeleteMapping("/{id}")
    public UserAdminResponse delete(@PathVariable Long id) {
        return userAdminService.deactivate(id);
    }
}
