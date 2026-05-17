package sn.immosn.backend.client.web.auth.mapper;

import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.client.web.auth.dto.AuthRegisterRequestDto;
import sn.immosn.backend.client.web.auth.dto.AuthResponseDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AuthMapper {

    public User toUser(AuthRegisterRequestDto dto) {
        User user = new User();
        user.setNomComplet(dto.getNomComplet());
        user.setEmail(dto.getEmail());
        user.setTelephone(dto.getTelephone());
        user.setPassword(dto.getMotDePasse());
        user.setArchived(false);
        return user;
    }

    public AuthResponseDto toAuthResponseDto(User user, String token) {
        return new AuthResponseDto(
                user.getId(),
                user.getNomComplet(),
                user.getEmail(),
                user.getTelephone(),
                user.getCreationDate(),
                user.isArchived(),
                token,
                "Bearer",
                user.getRoles().stream()
                        .map(Role::getRole)
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        );
    }
}
