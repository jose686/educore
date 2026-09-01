package com.educore.platform.store.service;

import com.educore.platform.store.model.ProductoCurso;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestionar la lógica del catálogo de ventas de cursos.
 */
public interface CatalogoService {

    /**
     * Obtiene todos los productos de curso activos y publicados.
     *
     * @return Lista de productos publicados.
     */
    List<ProductoCurso> obtenerCatalogoPublico();

    /**
     * Obtiene un producto de curso por su ID.
     *
     * @param id Identificador único del producto.
     * @return ProductoCurso correspondiente.
     * @throws IllegalArgumentException si no se encuentra.
     */
    ProductoCurso obtenerPorId(UUID id);

    /**
     * Guarda o actualiza un producto de curso en el catálogo.
     */
    ProductoCurso guardarProducto(ProductoCurso producto);

    /**
     * Elimina un producto de curso del catálogo por su ID.
     */
    void eliminarProducto(java.util.UUID id);

    /**
     * Obtiene todos los productos de curso del catálogo, sin importar su estado.
     */
    List<ProductoCurso> obtenerTodos();

    /**
     * Obtiene un producto de curso por el ID de su curso correspondiente en el LMS.
     */
    java.util.Optional<ProductoCurso> obtenerPorLmsCursoId(Long lmsCursoId);
}
