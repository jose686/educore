package com.educore.platform.store.repository;

import com.educore.platform.store.model.ProductoCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad ProductoCurso.
 */
@Repository
public interface ProductoCursoRepository extends JpaRepository<ProductoCurso, UUID> {

    /**
     * Recupera todos los productos de curso que tengan un estado específico.
     *
     * @param estado Estado a filtrar (ej: "PUBLISHED", "DRAFT").
     * @return Lista de productos correspondientes.
     */
    List<ProductoCurso> findByEstado(String estado);
}
