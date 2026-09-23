package com.fwn.foodwaste.security;


import com.fwn.foodwaste.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // enables @PreAuthorize on methods
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final UserDetailsServiceImpl userDetailsService;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("ADMIN","OPERATOR","DONOR")
                            .requestMatchers(HttpMethod.POST, "/api/food-donors").hasAnyRole("ADMIN","OPERATOR","DONOR")
                            .requestMatchers(HttpMethod.PUT, "/api/food-donors/**").hasAnyRole("ADMIN","OPERATOR","DONOR")
                            .requestMatchers(HttpMethod.POST, "/api/food-waste-items").hasAnyRole("ADMIN","OPERATOR","DONOR")
                            .requestMatchers(HttpMethod.PUT, "/api/food-waste-items/**").hasAnyRole("ADMIN","OPERATOR","DONOR")
                            .requestMatchers(HttpMethod.PATCH, "/api/food-waste-items/**").hasAnyRole("ADMIN","OPERATOR")
                            .requestMatchers(HttpMethod.DELETE, "/api/food-waste-items/**").hasRole("ADMIN")
                            .requestMatchers("/api/reports/**").hasAnyRole("ADMIN","OPERATOR")
                            .requestMatchers("/api/**").hasAnyRole("ADMIN","OPERATOR")
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();
        }
    }

