package io.skystay.auth;

import io.skystay.auth.dto.AuthResponse;
import io.skystay.auth.dto.LoginRequest;
import io.skystay.auth.dto.RegisterRequest;
import io.skystay.user.Role;
import io.skystay.user.User;
import io.skystay.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private  final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;


    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User u = User.builder()
                .id(UUID.randomUUID())
                .name(req.name())
                .email(req.email())
                .passwordHash(encoder.encode(req.password()))
                .role(Role.CUSTOMER)
                .createdAt(OffsetDateTime.now())
                .build();
        users.save(u);
        return new AuthResponse(jwt.issue(u.getEmail(), u.getRole().name()), u.getName(), u.getEmail(), u.getRole().name());

    }

    public AuthResponse login(LoginRequest req) {
        User u = users.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Email or password."));
        if(!encoder.matches(req.password(), u.getPasswordHash())){
            throw new IllegalArgumentException("Invalid email or password");
        }
        return new AuthResponse(jwt.issue(u.getEmail(), u.getRole().name()), u.getName(), u.getEmail(), u.getRole().name());

    }

}