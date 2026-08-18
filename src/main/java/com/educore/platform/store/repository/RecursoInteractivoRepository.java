package com.educore.platform.store.repository;

import com.educore.platform.store.model.RecursoInteractivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad RecursoInteractivo.
 */
@Repository
public interface RecursoInteractivoRepository extends JpaRepository<RecursoInteractivo, Long> {

    /**
     * Busca un recurso por su identificador único de búsqueda.
     */
    Optional<RecursoInteractivo> findByIdentificador(String identificador);

    /**
     * Recupera recursos activos.
     */
    List<RecursoInteractivo> findByActivo(boolean activo);

    /**
     * Filtrado dinámico avanzado para la administración de la biblioteca.
     */
    @Query("SELECT DISTINCT r FROM RecursoInteractivo r LEFT JOIN r.etiquetas e WHERE " +
           "(:etiqueta IS NULL OR :etiqueta = '' OR e = :etiqueta) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(r.identificador) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.titulo) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<RecursoInteractivo> filterBiblioteca(
            @Param("etiqueta") String etiqueta,
            @Param("search") String search);

    /**
     * Obtiene una lista de todas las etiquetas únicas cargadas en el sistema.
     */
    @Query("SELECT DISTINCT e FROM RecursoInteractivo r JOIN r.etiquetas e")
    List<String> findAllUniqueTags();
}
