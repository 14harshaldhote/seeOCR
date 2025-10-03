package com.see.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class RoleDto {

    private UUID id;

    @NotBlank(message = "Role name must not be blank")
    @Size(min=2, max=50, message = "Role name must be between 2 and 50 characters")
    private String name;

    @Size(max=200, message = "Description must not exceed 200 characters")
    private String description;

    public RoleDto(){}

    public RoleDto(String name, String description){
        this.name = name;
        this.description = description;
    }

}
