package id.ac.ui.cs.advprog.bidmart.auth.service;

import id.ac.ui.cs.advprog.bidmart.auth.dto.*;
import id.ac.ui.cs.advprog.bidmart.auth.entity.Permission;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.exception.AuthException;
import id.ac.ui.cs.advprog.bidmart.auth.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RoleGroupRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPermissionServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private RoleGroupRepository roleGroupRepository;
    @Mock private UserRepository userRepository;

    private AdminPermissionService service;

    @BeforeEach
    void setUp() {
        service = new AdminPermissionService(permissionRepository, roleGroupRepository, userRepository);
    }

    @Test
    void getAllPermissions_ReturnsAll() {
        when(permissionRepository.findAll()).thenReturn(List.of(
                Permission.builder().id(1L).name("role:manage").build()
        ));

        List<PermissionResponse> result = service.getAllPermissions();
        assertEquals(1, result.size());
        assertEquals("role:manage", result.getFirst().getName());
    }

    @Test
    void createPermission_Saves() {
        when(permissionRepository.existsByName("test:perm")).thenReturn(false);
        when(permissionRepository.save(any())).thenReturn(Permission.builder().id(1L).name("test:perm").build());

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("test:perm");

        PermissionResponse result = service.createPermission(request);
        assertEquals("test:perm", result.getName());
    }

    @Test
    void deletePermission_Removes() {
        Permission perm = Permission.builder().id(1L).name("test:perm").build();
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(perm));
        when(roleGroupRepository.findAll()).thenReturn(List.of());

        service.deletePermission(1L);

        verify(permissionRepository).delete(perm);
    }

    @Test
    void getAllRoleGroups_ReturnsAll() {
        when(roleGroupRepository.findAll()).thenReturn(List.of(
                RoleGroup.builder().id(1L).name("ADMIN").permissions(Set.of()).build()
        ));

        List<RoleGroupResponse> result = service.getAllRoleGroups();
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.getFirst().getName());
    }

    @Test
    void createRoleGroup_Saves() {
        when(roleGroupRepository.existsByName("TEST_ROLE")).thenReturn(false);
        when(roleGroupRepository.save(any())).thenReturn(RoleGroup.builder().id(1L).name("TEST_ROLE").permissions(Set.of()).build());

        CreateRoleGroupRequest request = new CreateRoleGroupRequest();
        request.setName("TEST_ROLE");

        RoleGroupResponse result = service.createRoleGroup(request);
        assertEquals("TEST_ROLE", result.getName());
    }

    @Test
    void deleteRoleGroup_Removes() {
        RoleGroup rg = RoleGroup.builder().id(1L).name("TEST").permissions(Set.of()).build();
        when(roleGroupRepository.findById(1L)).thenReturn(Optional.of(rg));
        when(userRepository.findAll()).thenReturn(List.of());

        service.deleteRoleGroup(1L);

        verify(roleGroupRepository).delete(rg);
    }
}
