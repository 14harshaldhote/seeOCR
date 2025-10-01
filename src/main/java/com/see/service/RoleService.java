package com.see.service;

import com.see.domain.Role;
import com.see.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public Optional<Role> findByName(String name) {
        log.debug("Finding role by name: {}", name);
        return roleRepository.findByName(name);
    }

    public Role save(Role role) {
        log.info("Saving role: {}", role.getName());
        return roleRepository.save(role);
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }
}
