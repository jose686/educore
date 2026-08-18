package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa una línea de detalle dentro de un {@link Pedido}.
 * Almacena una copia inmutable del ítem comprado para preservar el histórico
 * aunque el producto original sea modificado o eliminado del catálogo.
 */
@Entity
@Table(name = "detalles_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo del producto: "curso", "paquete" o "servicio".
     */
    @Column(nullable = false, length = 20)
    private String tipo;

    /**
     * ID original del producto en formato String (UUID para cursos, Long para paquetes).
     */
    @Column(name = "referencia_id", nullable = false, length = 100)
    private String referenciaId;

    /**
     * Título del producto en el momento de la compra (copia inmutable).
     */
    @Column(nullable = false)
    private String titulo;

    /**
     * Precio unitario final pagado (ya con descuentos aplicados).
     */
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Referencia al pedido padre.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
}
