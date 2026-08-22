package com.iftikar.outlier.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Get Authorization header
        String header = request.getHeader("Authorization");

        // 2. No Bearer token → continue normally
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Remove "Bearer " from the beginning
        String token = header.substring(7);

        Claims claims;

        try {
            // 4. Parse + verify JWT
            claims = jwtService.getClaims(token);

        } catch (JwtException | IllegalArgumentException ex) {

            // Invalid / expired / malformed token
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Make sure this is an ACCESS token
        if (!jwtService.isAccessToken(claims)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 6. Extract information from JWT
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 7. Don't overwrite an existing authentication
        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            // 8. Convert role into Spring Security authority
            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            // 9. Create authenticated principal
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            // 10. Attach request details
            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // 11. Tell Spring Security who is authenticated
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);
        }

        // 12. Continue to the next filter/controller
        filterChain.doFilter(request, response);
    }
}