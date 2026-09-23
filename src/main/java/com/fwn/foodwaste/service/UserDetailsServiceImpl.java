package com.fwn.foodwaste.service;



import com.fwn.foodwaste.entity.User;
import com.fwn.foodwaste.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class UserDetailsServiceImpl implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public UserDetails loadUserByUsername(String username)
                throws UsernameNotFoundException {

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with username: " + username));

            // Convert each Role → SimpleGrantedAuthority ("ROLE_ADMIN" etc.)
            var authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority(role.getRole().name()))
                    .collect(Collectors.toSet());

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    user.isActive(),   // enabled
                    true,              // accountNonExpired
                    true,              // credentialsNonExpired
                    true,              // accountNonLocked
                    authorities
            );
        }

}
