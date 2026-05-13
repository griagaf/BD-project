package com.tacticaldistrict.command.user.service;

import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.UserPrincipal;
import com.tacticaldistrict.command.user.entity.UserEntity;
import com.tacticaldistrict.command.user.repository.PermissionRepository;
import com.tacticaldistrict.command.user.repository.UserRepository;
import com.tacticaldistrict.command.user.repository.UserRoleRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TacticalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadPrincipalByUserId(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User id " + userId));
        return toPrincipal(user);
    }

    private UserPrincipal toPrincipal(UserEntity user) {
        Set<RoleCode> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        Set<String> permissions = roles.isEmpty()
                ? Set.of()
                : permissionRepository.findPermissionCodesByRoles(roles);

        return new UserPrincipal(
                user.getId(),
                user.getPersonnelId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPasswordHash(),
                user.isActive(),
                roles,
                permissions
        );
    }
}
