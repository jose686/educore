package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que registra una transacción de compra completada o reembolsada.
 * Se persiste cuando Stripe confirma el pago mediante el evento webhook
 * {@code checkout.session.completed}.
 */
@Entity
@Table(
    name = "pedidos",
    indexes = {
        @Index(name = "idx_pedidos_email", columnList = "email_usuario"),
        @Index(name = "idx_pedidos_session", columnList = "stripe_session_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID de la Checkout Session de Stripe. Clave única para evitar procesamiento duplicado.
     */
    @Column(name = "stripe_session_id", nullable = false, unique = true, length = 200)
    private String stripeSessionId;

    /**
     * ID del PaymentIntent de Stripe. Necesario para ejecutar reembolsos.
     * Puede ser nulo en sesiones de pago no completadas o en modo test sin PI.
     */
    @Column(name = "stripe_payment_intent_id", length = 200)
    private String stripePaymentIntentId;

    /**
     * Email del usuario que realizó la compra (copia desnormalizada para simplificar consultas).
     */
    @Column(name = "email_usuario", nullable = false, length = 320)
    private String emailUsuario;

    /**
     * Importe total pagado en EUR.
     */
    @Column(name = "total_euros", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalEuros;

    /**
     * Fecha y hora de confirmación del pago.
     */
    @Column(name = "fecha_compra", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCompra = LocalDateTime.now();

    /**
     * Estado del ciclo de vida del pedido.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.COMPLETADO;

    /**
     * Ítems comprados en este pedido. Se cargan en cascada.
     */
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetallePedido> detalles = new ArrayList<>();

    /**
     * Añade un detalle al pedido manteniendo la relación bidireccional.
     */
    public void addDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        this.detalles.add(detalle);
    }
}
