package com.educore.platform.users.service;

import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.dto.UsuarioRegistroDTO;

/**
 * Servicio para gestionar la lógica de negocio de los usuarios.
 */
public interface UsuarioService {

    /**
     * Registra un nuevo usuario en la plataforma.
     *
     * @param dto Datos del registro del usuario.
     * @return El usuario guardado.
     * @throws com.educore.platform.users.exception.EmailAlreadyExistsException si el email ya existe.
     */
    Usuario registrar(UsuarioRegistroDTO dto);

    /**
     * Recupera todos los usuarios de la base de datos.
     */
    java.util.List<Usuario> obtenerTodosLosUsuarios();

    /**
     * Modifica el rol de un usuario existente.
     */
    void actualizarRol(Long id, com.educore.platform.users.model.Role rol);

    /**
     * Recupera un usuario por su email.
     */
    Usuario obtenerPorEmail(String email);

    /**
     * Recupera un usuario por su ID.
     */
    Usuario obtenerPorId(Long id);

    /**
     * Guarda o actualiza un usuario en la base de datos.
     */
    Usuario guardar(Usuario usuario);
}
