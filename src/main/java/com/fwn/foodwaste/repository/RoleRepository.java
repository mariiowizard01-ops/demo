package com.fwn.foodwaste.repository;

import com.fwn.foodwaste.entity.Role;
import com.fwn.foodwaste.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Used by DataSeeder on startup and by AuthService during registration

    Optional<Role> findByRole(RoleName role);
}
