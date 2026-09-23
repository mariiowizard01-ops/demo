package com.fwn.foodwaste.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

        @Value("${app.jwt.secret}")
        private String jwtSecret;

        @Value("${app.jwt.expiration-ms}")
        private long jwtExpirationMs;

        public String generateToken(Authentication auth) {
            UserDetails user = (UserDetails) auth.getPrincipal();
            return Jwts.builder()
                    .setSubject(user.getUsername())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                    .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()), SignatureAlgorithm.HS256)
                    .compact();
        }

        public String getUsernameFromToken(String token) {
            return Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        }

        public boolean validateToken(String token) {
            try {
                Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                        .build()
                        .parseClaimsJws(token)
                        .getPayload();

                return true;
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        }

}
