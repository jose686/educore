package com.educore.platform.lms.service;

/**
 * Servicio para gestionar la matriculación de alumnos en el Aula Virtual.
 */
public interface AulaVirtualService {

    /**
     * Matricula a un alumno en un curso específico, promoviendo su rol de ser necesario.
     *
     * @param email    Correo electrónico del alumno.
     * @param cursoId  ID numérico del curso en el Aula Virtual.
     */
    void matricularAlumno(String email, Long cursoId);
}
