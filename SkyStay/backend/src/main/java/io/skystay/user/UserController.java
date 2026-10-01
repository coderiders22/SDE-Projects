package io.skystay.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public  class UserController {
    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    public record UserDto(String id, String name, String email, String role) {
        static UserDto of(User u) {
            return new UserDto(u.getId().toString(), u.getName(), u.getEmail(), u.getRole().name());
        }
    }

    public record UpdateProfile(@NotBlank @Size(max = 80) String name, @Email String email) {}

    @GetMapping("/me")
    public  UserDto me(@AuthenticationPrincipal UserDetails principal) {
        User u = users.findByEmail(principal.getUsername()).orElseThrow();
        return UserDto.of(u);
    }

    @PutMapping("/me")
    public UserDto update(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody UpdateProfile body) {
        User u = users.findByEmail(principal.getUsername()).orElseThrow();
        u.setName(body.name());
        u.setEmail(body.email());

        return UserDto.of(users.save(u));
    }
}