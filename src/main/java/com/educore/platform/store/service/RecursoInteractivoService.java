package com.educore.platform.store.service;

import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.media.model.CategoriaMedia;
import java.util.List;

/**
 * Servicio para gestionar la lógica de negocio de los recursos interactivos HTML5.
 */
public interface RecursoInteractivoService {

    /**
     * Obtiene todos los recursos registrados.
     */
    List<RecursoInteractivo> obtenerTodos();

    /**
     * Obtiene todos los recursos activos filtrados por la categoría del MediaFile asociado.
     */
    List<RecursoInteractivo> obtenerActivosPorCategoriaMedia(CategoriaMedia categoria);

    /**
     * Obtiene recursos usando los filtros de búsqueda, tags y categoría de MediaFile.
     */
    List<RecursoInteractivo> obtenerBibliotecaFiltrada(CategoriaMedia categoria, String etiqueta, String search);

    /**
     * Obtiene todas las etiquetas únicas registradas.
     */
    List<String> obtenerTodasEtiquetas();

    /**
     * Obtiene un recurso por su ID de base de datos.
     */
    RecursoInteractivo obtenerPorId(Long id);

    /**
     * Obtiene un recurso por su identificador único de búsqueda.
     */
    RecursoInteractivo obtenerPorIdentificador(String identificador);

    /**
     * Guarda o actualiza un recurso interactivo.
     */
    RecursoInteractivo guardar(RecursoInteractivo recurso);

    /**
     * Elimina un recurso interactivo.
     */
    void eliminar(Long id);
}
