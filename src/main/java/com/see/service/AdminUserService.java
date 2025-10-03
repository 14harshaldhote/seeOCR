package com.see.service;

import com.see.domain.Role;
import com.see.domain.User;
import com.see.dto.AssignRoleRequest;
import com.see.dto.CreateUserRequest;
import com.see.dto.UserDto;
import com.see.repository.RoleRepository;
import com.see.repository.UserRepository;
import com.see.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDto createUser(CreateUserRequest request){
        log.info("Creating new user: {}", request.getUsername());

        if(userRepository.existsByUsername(request.getUsername())){
            throw new RuntimeException("Username "+ request.getUsername() + " already exists.");
        }
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email "+ request.getEmail() + " already exists.");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(){
        log.info("Fetching all users");

        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(UUID id){
        log.info("Getting user by ID: {}", id);

        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findByUsername(String username){
        log.info("Finding user by username: {}", username);

        return userRepository.findByUsername(username)
                .map(userMapper::toDto);
    }

    public UserDto updateUser(UUID id, UserDto udto){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        if(!user.getUsername().equals(udto.getUsername()) &&
                userRepository.existsByUsername(udto.getUsername())){
            throw new RuntimeException("Username " + udto.getUsername() + " already exists.");
        }

        if(!user.getEmail().equals(udto.getEmail()) &&
                userRepository.existsByEmail(udto.getEmail())){
            throw new RuntimeException("Email " + udto.getEmail() + " already exists.");
        }

        User updatedUser = userMapper.updateEntity(user, udto); // Fixed method name
        User savedUser = userRepository.save(updatedUser);

        log.info("User updated with ID: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    public UserDto assignRoleToUser(AssignRoleRequest request) {
        log.info("Assigning role {} to user {}", request.getRoleId(), request.getUserId());

        // Convert String IDs to UUIDs
        UUID userId = UUID.fromString(request.getUserId());
        UUID roleId = UUID.fromString(request.getRoleId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        Role role = (Role) roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + request.getRoleId()));

        if (user.getRoles().contains(role)) {
            throw new RuntimeException("User already has this role assigned");
        }

        user.getRoles().add(role);
        user.setUpdatedAt(new Date());

        User savedUser = userRepository.save(user);
        log.info("Role assigned successfully");

        return userMapper.toDto(savedUser);
    }

    public UserDto removeRoleFromUser(AssignRoleRequest request){
        log.info("Removing role {} from user {}", request.getRoleId(), request.getUserId());

        // Convert String IDs to UUIDs
        UUID userId = UUID.fromString(request.getUserId());
        UUID roleId = UUID.fromString(request.getRoleId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + request.getUserId()));

        Role role = (Role) roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + request.getRoleId()));

        if(!user.getRoles().contains(role)){
            throw new RuntimeException("User does not have this role assigned");
        }

        user.getRoles().remove(role);
        user.setUpdatedAt(new Date());
        User savedUser = userRepository.save(user);

        log.info("Role {} removed from user {}", request.getRoleId(), request.getUserId());
        return userMapper.toDto(savedUser);
    }

    public void deleteUser(UUID id){
        log.info("Deleting user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        userRepository.delete(user);
        log.info("User deleted with ID: {}", id);
    }


}
