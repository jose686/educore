package com.educore.platform.lms.service;

/**
 * Servicio para verificar el acceso y gestionar las inscripciones de un usuario a un curso.
 */
public interface AccesoService {
    /**
     * Comprueba si un usuario tiene acceso activo (inscripción permanente o temporal vigente) a un curso.
     */
    boolean tieneAcceso(Long studentId, Long cursoId);

    /**
     * Inscribe temporalmente al usuario en todos los cursos incluidos en un paquete.
     */
    void crearInscripcionesDePaquete(Long paqueteId, Long studentId);
}
