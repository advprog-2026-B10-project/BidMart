package id.ac.ui.cs.advprog.bidmart.auth.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Permission;
import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import id.ac.ui.cs.advprog.bidmart.auth.repository.PermissionRepository;
import id.ac.ui.cs.advprog.bidmart.auth.repository.RoleGroupRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleGroupRepository roleGroupRepository;

    @Override
    public void run(String... args) {
        if (permissionRepository.count() > 0) {
            return;
        }

        Permission auctionCreate = createPermission("auction:create");
        Permission auctionView = createPermission("auction:view");
        Permission auctionEdit = createPermission("auction:edit");
        Permission auctionDelete = createPermission("auction:delete");
        Permission bidPlace = createPermission("bid:place");
        Permission bidView = createPermission("bid:view");
        Permission orderView = createPermission("order:view");
        Permission orderManage = createPermission("order:manage");
        Permission userManage = createPermission("user:manage");
        Permission userDisable = createPermission("user:disable");
        Permission profileEdit = createPermission("profile:edit");
        Permission profileView = createPermission("profile:view");
        Permission walletView = createPermission("wallet:view");
        Permission walletTopup = createPermission("wallet:topup");
        Permission notificationManage = createPermission("notification:manage");
        Permission roleManage = createPermission("role:manage");

        createRoleGroup("ADMIN", Set.of(
                auctionCreate, auctionView, auctionEdit, auctionDelete,
                bidPlace, bidView, orderView, orderManage,
                userManage, userDisable, profileEdit, profileView,
                walletView, walletTopup, notificationManage, roleManage
        ));

        createRoleGroup("BUYER", Set.of(
                auctionView, bidPlace, bidView, orderView,
                profileEdit, profileView, walletView, walletTopup,
                notificationManage
        ));

        createRoleGroup("SELLER", Set.of(
                auctionCreate, auctionView, auctionEdit, auctionDelete,
                bidView, orderView, profileEdit, profileView,
                walletView, walletTopup, notificationManage
        ));
    }

    private Permission createPermission(String name) {
        if (!permissionRepository.existsByName(name)) {
            return permissionRepository.save(Permission.builder().name(name).build());
        }
        return permissionRepository.findByName(name).orElseThrow();
    }

    private RoleGroup createRoleGroup(String name, Set<Permission> permissions) {
        if (!roleGroupRepository.existsByName(name)) {
            return roleGroupRepository.save(RoleGroup.builder()
                    .name(name)
                    .permissions(permissions)
                    .build());
        }
        return roleGroupRepository.findByName(name).orElseThrow();
    }
}
