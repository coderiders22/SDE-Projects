package io.skystay.auth;


import io.skystay.auth.dto.AuthResponse;
import io.skystay.auth.dto.LoginRequest;
import io.skystay.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public  class AuthController {

    private final AuthService auth;
    public AuthController(AuthService auth) {
        this.auth = auth;
    }
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest body) { return auth.register(body); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest body) { return auth.login(body); }
}