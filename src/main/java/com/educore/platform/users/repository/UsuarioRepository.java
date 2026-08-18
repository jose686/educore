package com.educore.platform.users.repository;

import com.educore.platform.users.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su dirección de correo electrónico.
     *
     * @param email Correo electrónico a buscar.
     * @return Un Optional que contiene el usuario si se encuentra, o vacío en caso contrario.
     */
    Optional<Usuario> findByEmail(String email);
}
