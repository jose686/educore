package com.educore.platform.media.repository;

import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.model.MediaType;
import com.educore.platform.media.model.CategoriaMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad MediaFile.
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

    Optional<MediaFile> findByFilename(String filename);

    Optional<MediaFile> findByUrl(String url);

    Optional<MediaFile> findByAlias(String alias);

    List<MediaFile> findAllByOrderByUploadedAtDesc();

    List<MediaFile> findByTipoOrderByUploadedAtDesc(MediaType tipo);

    List<MediaFile> findByCategoriaMediaOrderByUploadedAtDesc(CategoriaMedia categoriaMedia);

    @Query("SELECT m FROM MediaFile m WHERE " +
           "(:tipo IS NULL OR m.tipo = :tipo) AND " +
           "(:categoria IS NULL OR m.categoriaMedia = :categoria) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(m.alias) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(m.nombreOriginal) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<MediaFile> searchFiles(@Param("tipo") MediaType tipo, @Param("categoria") CategoriaMedia categoria, @Param("search") String search);
}
