package com.educore.platform.lms.service;

import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.model.Modulo;
import com.educore.platform.lms.dto.LeccionDTO;
import com.educore.platform.lms.dto.ModuloDTO;
import java.util.List;

/**
 * Servicio para gestionar las operaciones del aula virtual (LMS).
 */
public interface LmsService {

    /**
     * Recupera todos los cursos en los que está matriculado un estudiante.
     */
    List<Curso> obtenerCursosEstudiante(String emailUsuario);

    /**
     * Recupera una lección específica asegurando primero que el estudiante
     * tiene matrícula activa en dicho curso.
     */
    Leccion obtenerLeccionAsegurada(Long cursoId, Long leccionId, String emailUsuario);

    /**
     * Obtiene los detalles completos de un curso.
     */
    Curso obtenerCursoPorId(Long cursoId);

    /**
     * Obtiene un módulo específico por su ID.
     */
    Modulo obtenerModuloPorId(Long moduloId);

    /**
     * Crea y guarda un nuevo módulo en un curso específico.
     *
     * @param cursoId ID del curso al que se asocia el módulo.
     * @param datos   Datos de creación del módulo.
     * @return El Modulo persistido.
     */
    Modulo crearModulo(Long cursoId, ModuloDTO datos);

    /**
     * Crea y guarda una nueva lección en un módulo específico.
     *
     * @param moduloId ID del módulo al que se asocia la lección.
     * @param datos    Datos de creación de la lección.
     * @return La Leccion persistida.
     */
    Leccion crearLeccion(Long moduloId, LeccionDTO datos);

    /**
     * Obtiene una lección por su ID.
     */
    Leccion obtenerLeccionPorId(Long leccionId);

    /**
     * Actualiza un módulo existente.
     */
    Modulo actualizarModulo(Long moduloId, ModuloDTO datos);

    /**
     * Elimina un módulo y todas sus lecciones asociadas.
     */
    void eliminarModulo(Long moduloId);

    /**
     * Actualiza una lección existente.
     */
    Leccion actualizarLeccion(Long leccionId, LeccionDTO datos);

    /**
     * Elimina una lección.
     */
    void eliminarLeccion(Long leccionId);

    /**
     * Calcula los días restantes de acceso temporal para un usuario en un curso.
     */
    java.util.Optional<Long> obtenerDiasRestantesAcceso(String emailUsuario, Long cursoId);

    /**
     * Recupera todos los cursos registrados del sistema.
     */
    List<Curso> obtenerTodosLosCursos();

    /**
     * Crea y guarda un nuevo curso en el LMS.
     */
    Curso crearCurso(Curso curso);
}
