package com.educore.platform.users.service;

import java.util.Optional;

/**
 * Contrato de servicio público expuesto por el módulo 'users' para ser consumido
 * por otros módulos (ej. lms o store), asegurando el aislamiento modular.
 */
public interface UserPublicService {

    /**
     * Obtiene el identificador numérico de un usuario por su correo electrónico.
     *
     * @param email Correo electrónico del usuario.
     * @return Un Optional conteniendo el ID del usuario si se encuentra.
     */
    Optional<Long> getUserIdByEmail(String email);

    /**
     * Promueve al usuario con el rol de visitante (VISITOR) a estudiante (STUDENT)
     * para otorgarle acceso al Aula Virtual tras realizar una compra.
     *
     * @param email Correo electrónico del usuario.
     */
    void promoverAEstudianteSiEsVisitante(String email);

    /**
     * Obtiene el rol de un usuario por su correo electrónico.
     */
    java.util.Optional<String> getUserRoleByEmail(String email);
}
