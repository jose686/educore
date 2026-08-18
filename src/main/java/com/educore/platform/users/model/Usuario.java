package com.educore.platform.users.model;

import jakarta.persistence.*;
import lombok.*;
import com.educore.platform.store.model.RecursoInteractivo;
import java.util.Set;
import java.util.HashSet;

/**
 * Entidad de dominio que representa a un Usuario en la plataforma.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "saldo_creditos", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default
    private Integer saldoCreditos = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuarios_recursos",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "recurso_id")
    )
    @Builder.Default
    private Set<RecursoInteractivo> minijuegosDesbloqueados = new HashSet<>();
}
