package com.see.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "username must not be blank")
    @Size(min=3, max=50, message = "username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Size(max=100, message = "Email must not exceed 100 characters")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min=6, max=100, message = "Password must be between 6 and 100 characters")
    private String password;

    public CreateUserRequest() {
    }
}
