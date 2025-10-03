package com.see.util;

import com.see.domain.Role;
import com.see.domain.User;
import com.see.dto.CreateUserRequest;
import com.see.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user){
        if(user == null){
            return null;
        }

        UserDto unto = new UserDto();
        unto.setId(user.getId());
        unto.setUsername(user.getUsername());
        unto.setEmail(user.getEmail());
        unto.setActive(user.isActive());
        unto.setCreatedAt(user.getCreatedAt());
        unto.setUpdatedAt(user.getUpdatedAt());

        if(user.getRoles()!=null){
            unto.setRoleNames((
                    user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toSet())
                    ));
        }

        return unto;
    }

    public User toEntity(CreateUserRequest request){
        if(request == null){
            return null;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setActive(true); // New users are active by default
        user.setCreatedAt(new java.util.Date());
        user.setUpdatedAt(new java.util.Date());

        return user;
    }

    public User updateEntity(User user, UserDto dto){
        if(user == null || dto == null){
            return null;
        }

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setActive(dto.isActive());
        user.setUpdatedAt(new java.util.Date());

        return user;
    }
}
