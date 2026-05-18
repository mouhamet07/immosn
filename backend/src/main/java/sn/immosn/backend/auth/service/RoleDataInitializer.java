package sn.immosn.backend.auth.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.repository.RoleRepository;

@Component
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleDataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (RoleType type : RoleType.values()) {
            roleRepository.findByRole(type).orElseGet(() -> {
                Role role = new Role();
                role.setRole(type);
                return roleRepository.save(role);
            });
        }
    }
}
