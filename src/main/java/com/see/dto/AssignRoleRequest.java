package com.see.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleRequest {
    @NotNull(message = "User ID must not be null")
    private String userId;

    @NotNull(message = "Role ID must not be null")
    private String roleId;

    public AssignRoleRequest(){

    }

    public AssignRoleRequest(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
}
