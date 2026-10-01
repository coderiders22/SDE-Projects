package io.skystay.auth;


import io.jsonwebtoken.Claims;
import io.skystay.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public  class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private  final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")){
            String token = header.substring(7);
            try {
                Claims c = jwt.parse(token);
                String email = c.getSubject();
                String role = String.valueOf(c.get("role"));
                users.findByEmail(email).ifPresent(u -> {
                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    var principal = org.springframework.security.core.userdetails.User
                            .withUsername(u.getEmail())
                            .password(u.getPasswordHash())
                            .authorities(List.of(authority))
                            .build();
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (Exception ignored) {
                // invalid token - leave context empty
            }
        }
        chain.doFilter(req, res);
    }

}