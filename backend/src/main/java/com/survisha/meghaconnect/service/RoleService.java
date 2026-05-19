package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    public List<String> getAllRoleNames() {
        log.debug("Fetching all role names");
        return roleRepository.findAll().stream()
                .map(role -> role.getRoleName())
                .sorted()
                .collect(Collectors.toList());
    }

    public boolean existsByRoleName(String roleName) {
        return roleName != null && roleRepository.existsByRoleNameIgnoreCase(roleName.trim());
    }
}
