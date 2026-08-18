package com.educore.platform.blog.repository;

import com.educore.platform.blog.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad Articulo.
 */
@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, UUID> {

    /**
     * Busca un artículo a través de su URL amigable (slug).
     *
     * @param slug El slug único del artículo.
     * @return El artículo correspondiente envuelto en un Optional.
     */
    Optional<Articulo> findBySlug(String slug);

    /**
     * Obtiene todos los artículos ordenados por fecha de publicación descendente.
     */
    List<Articulo> findAllByOrderByFechaPublicacionDesc();
}
