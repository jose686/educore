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

    /**
     * Comprueba si existe algún usuario con el rol especificado.
     *
     * @param role Rol a verificar.
     * @return true si existe al menos un usuario con ese rol, false en caso contrario.
     */
    boolean existsByRole(com.educore.platform.users.model.Role role);
}
