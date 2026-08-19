package com.educore.platform.blog.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio que representa un artículo o entrada de blog.
 * Utiliza UUID como identificador primario.
 */
@Entity
@Table(
    name = "articulos",
    indexes = {
        @Index(name = "idx_articulo_slug", columnList = "slug", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Articulo {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "resumen_corto", length = 300)
    private String resumenCorto;

    /**
     * El contenido HTML completo del artículo.
     * Mapeado como TEXT para permitir textos largos.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    /**
     * URL amigable única para SEO (ej: "introduccion-tactica-ajedrez").
     */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    /**
     * ID del usuario (autor) en el módulo 'users'.
     */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "featured_image_url")
    private String featuredImageUrl;
}
