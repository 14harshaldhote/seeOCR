package com.see.service;

import com.see.domain.Role;
import com.see.dto.RoleDto;
import com.see.repository.RoleRepository;
import com.see.util.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleDto createRole(RoleDto roleDto){
        log.info("Creating new role: {}", roleDto.getName());
        if(roleRepository.existsByName(roleDto.getName())){
            throw new IllegalArgumentException("Role with name " + roleDto.getName() + " already exists.");
        }
        Role role = roleMapper.toEntity(roleDto);
        Role savedRole = roleRepository.save(role);

        log.info("Role created with ID: {}", savedRole.getId());
        return roleMapper.toDto(savedRole);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles(){
        log.info("Fetching all roles");
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .map(roleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getRoleById(UUID id) {
        log.info("Fetching role by ID: {}", id);
        return roleRepository.findById(id)
                .map(roleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getRoleByName(String name){
        log.info("Fetching role by name: {}", name);
        return roleRepository.findByName(name)
                .map(roleMapper::toDto);
    }

    public RoleDto updateRole(UUID id, RoleDto roleDto){
        log.info("Updating role with ID: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + id));

        if(!role.getName().equals(roleDto.getName()) && roleRepository.existsByName(roleDto.getName())){
            throw new RuntimeException("Role with name " + roleDto.getName() + " already exists.");
        }
        Role updatedRole = roleMapper.updateEntity(role, roleDto);
        Role savedRole = roleRepository.save(updatedRole);

        log.info("Role updated with ID: {}", savedRole.getId());
        return roleMapper.toDto(savedRole);
    }

    public void deleteRole(UUID id){
        log.info("Deleting role with ID : {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + id));

        if(!role.getUsers().isEmpty()){
            throw new RuntimeException("Cannot delete role assigned to users.");
        }
        roleRepository.delete(role);
        log.info("Role deleted successfully with ID: {}", id);
    }
}
