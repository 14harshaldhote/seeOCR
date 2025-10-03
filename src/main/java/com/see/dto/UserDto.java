package com.see.dto;

import com.see.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Data
public class UserDto {

    private UUID id;

    @NotBlank(message = "Username must not be blank")
    @Size(min=3, max=50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotNull(message = "Email must not be null")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    private boolean isActive= true;

    private Date createdAt;
    private Date updatedAt;

    private Set<String> roleNames;

    public UserDto(){

    }
    private UserDto(String username, String email)
    {
        this.username = username;
        this.email = email;
    }
}
