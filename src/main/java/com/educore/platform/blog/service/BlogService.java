package com.educore.platform.blog.service;

import com.educore.platform.blog.model.Articulo;
import java.util.List;

/**
 * Servicio para gestionar la lógica del blog de la plataforma.
 */
public interface BlogService {

    /**
     * Recupera todos los artículos del blog ordenados cronológicamente por fecha descendente.
     */
    List<Articulo> obtenerTodosLosArticulos();

    /**
     * Recupera un artículo por su slug único.
     *
     * @param slug El slug amigable del artículo.
     * @return El artículo correspondiente.
     * @throws IllegalArgumentException si el artículo no existe.
     */
    Articulo obtenerPorSlug(String slug);

    /**
     * Guarda o actualiza un artículo de blog en la base de datos.
     */
    Articulo guardarArticulo(Articulo articulo);

    /**
     * Obtiene un artículo de blog por su ID.
     */
    Articulo obtenerPorId(java.util.UUID id);

    /**
     * Elimina un artículo de blog por su ID.
     */
    void eliminarArticulo(java.util.UUID id);
}
