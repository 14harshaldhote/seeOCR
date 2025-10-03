package com.see.util;

import com.see.domain.Role;
import com.see.dto.RoleDto;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleDto toDto(Role role){  // FIXED: Changed from rdto() to toDto()
        if(role == null){
            return null;
        }

        RoleDto dto = new RoleDto();  // FIXED: Changed variable name from rdto to dto for clarity
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());

        return dto;
    }

    public Role toEntity(RoleDto dto){  // FIXED: Changed parameter name from rdto to dto
        if(dto == null){
            return null;
        }

        Role role = new Role();
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        return role;
    }

    public Role updateEntity(Role role, RoleDto dto){  // FIXED: Changed method name from updatedEntity to updateEntity
        if (role == null || dto == null){
            return role;  // FIXED: Return role instead of null to maintain the object reference
        }
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());

        return role;
    }
}
