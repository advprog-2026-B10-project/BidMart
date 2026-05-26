package id.ac.ui.cs.advprog.bidmart.auth.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Permission;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.auth.exception.AuthException;
import id.ac.ui.cs.advprog.bidmart.auth.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RoleGroupRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleGroupRepository roleGroupRepository;
    private final UserRepository userRepository;

    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new AuthException(HttpStatus.CONFLICT, "Permission already exists");
        }
        Permission permission = Permission.builder().name(request.getName()).build();
        return PermissionResponse.fromEntity(permissionRepository.save(permission));
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Permission not found"));

        for (RoleGroup rg : roleGroupRepository.findAll()) {
            if (rg.getPermissions().removeIf(p -> p.getId().equals(id))) {
                roleGroupRepository.save(rg);
            }
        }

        permissionRepository.delete(permission);
    }

    public List<RoleGroupResponse> getAllRoleGroups() {
        return roleGroupRepository.findAll().stream()
                .map(RoleGroupResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleGroupResponse createRoleGroup(CreateRoleGroupRequest request) {
        if (roleGroupRepository.existsByName(request.getName())) {
            throw new AuthException(HttpStatus.CONFLICT, "Role group already exists");
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionIds());
        RoleGroup roleGroup = RoleGroup.builder()
                .name(request.getName())
                .permissions(permissions)
                .build();
        return RoleGroupResponse.fromEntity(roleGroupRepository.save(roleGroup));
    }

    @Transactional
    public RoleGroupResponse updateRoleGroup(Long id, UpdateRoleGroupRequest request) {
        RoleGroup roleGroup = roleGroupRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Role group not found"));

        if (!roleGroup.getName().equals(request.getName()) && roleGroupRepository.existsByName(request.getName())) {
            throw new AuthException(HttpStatus.CONFLICT, "Role group name already taken");
        }

        roleGroup.setName(request.getName());
        roleGroup.setPermissions(resolvePermissions(request.getPermissionIds()));
        return RoleGroupResponse.fromEntity(roleGroupRepository.save(roleGroup));
    }

    @Transactional
    public void deleteRoleGroup(Long id) {
        RoleGroup roleGroup = roleGroupRepository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Role group not found"));

        for (User user : userRepository.findAll()) {
            if (user.getRoleGroups().removeIf(rg -> rg.getId().equals(id))) {
                userRepository.save(user);
            }
        }

        roleGroupRepository.delete(roleGroup);
    }

    @Transactional
    public void updateUserRoleGroups(Long userId, Set<Long> roleGroupIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

        Set<RoleGroup> roleGroups = new HashSet<>(roleGroupRepository.findAllById(roleGroupIds));
        if (roleGroups.size() != roleGroupIds.size()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "One or more role groups not found");
        }

        user.setRoleGroups(roleGroups);
        userRepository.save(user);
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
        if (permissions.size() != permissionIds.size()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "One or more permissions not found");
        }
        return permissions;
    }
}
