package com.educore.platform.media.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio que representa un archivo multimedia subido al servidor con soporte para alias únicos.
 */
@Entity
@Table(
    name = "media_files",
    indexes = {
        @Index(name = "idx_media_file_filename", columnList = "filename", unique = true),
        @Index(name = "idx_media_file_alias", columnList = "alias", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String filename;

    @Column(nullable = false)
    private String url;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private MediaType tipo;

    @Column(name = "nombre_original", nullable = true)
    private String nombreOriginal;

    @Column(name = "alias", unique = true, nullable = true)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_media", nullable = false)
    @Builder.Default
    private CategoriaMedia categoriaMedia = CategoriaMedia.GENERAL;

    public MediaType getTipo() {
        return tipo != null ? tipo : MediaType.IMAGEN;
    }
}
