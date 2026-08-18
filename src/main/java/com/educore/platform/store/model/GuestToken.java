package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un token de invitación temporal generado por el administrador.
 */
@Entity
@Table(name = "guest_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    /**
     * IDs de los cursos asignados a este token.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "guest_token_cursos",
        joinColumns = @JoinColumn(name = "guest_token_id")
    )
    @Column(name = "curso_id")
    @Builder.Default
    private Set<Long> cursoIds = new HashSet<>();

    @Column(name = "dias_acceso", nullable = false)
    private Integer diasAcceso;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "usuario_id")
    private Long usuarioId; // ID del estudiante que lo canjeó

    @Column(name = "fecha_canje")
    private LocalDateTime fechaCanje;
}
