package id.ac.ui.cs.advprog.bidmart.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;

public interface RoleGroupRepository extends JpaRepository<RoleGroup, Long> {
    Optional<RoleGroup> findByName(String name);
    boolean existsByName(String name);
}
