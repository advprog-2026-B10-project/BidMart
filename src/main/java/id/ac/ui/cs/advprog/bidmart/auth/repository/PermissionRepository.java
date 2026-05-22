package id.ac.ui.cs.advprog.bidmart.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    boolean existsByName(String name);
}
