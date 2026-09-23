package com.fwn.foodwaste.repository;

import com.fwn.foodwaste.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import com.fwn.foodwaste.entity.enums.RoleName;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRoles_Role(RoleName role);
}
