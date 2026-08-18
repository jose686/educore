package com.educore.platform.users.service;

import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Implementación del servicio público UserPublicService.
 */
@Service
@Transactional(readOnly = true)
public class UserPublicServiceImpl implements UserPublicService {

    private final UsuarioRepository usuarioRepository;

    public UserPublicServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Optional<Long> getUserIdByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(Usuario::getId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void promoverAEstudianteSiEsVisitante(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            if (usuario.getRole() == com.educore.platform.users.model.Role.VISITOR) {
                usuario.setRole(com.educore.platform.users.model.Role.STUDENT);
                usuarioRepository.save(usuario);
            }
        });
    }

    @Override
    public Optional<String> getUserRoleByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> usuario.getRole().name());
    }
}
