//package com.fwn.foodwaste.config;
//
//
//
//
//import com.fwn.foodwaste.entity.Role;
//import com.fwn.foodwaste.entity.enums.RoleName;
//import com.fwn.foodwaste.repository.RoleRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.Arrays;
//
//@Component
//@RequiredArgsConstructor
//public class DataSeeder implements CommandLineRunner {
//
//    private final RoleRepository roleRepo;
//
//    @Override
//    public void run(String... args) {
//        Arrays.stream(RoleName.values()).forEach(role -> {
//            if (roleRepo.findByRole(role).isEmpty()) {
//                Role roles = new Role();
//                roles.setRole(role);
//                roleRepo.save(roles);
//                System.out.println("Seeded role: " + role);
//            }
//        });
//    }
//}
