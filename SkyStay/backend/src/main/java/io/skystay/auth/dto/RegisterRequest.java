package io.skystay.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@Email @NotBlank() String email, @NotBlank @Size(max = 80) String name, @NotBlank @Size(min = 6, max = 72) String password ) {
}