package com.educore.platform.store.repository;

import com.educore.platform.store.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Pedido}.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Historial de compras de un usuario, ordenado del más reciente al más antiguo.
     */
    List<Pedido> findByEmailUsuarioOrderByFechaCompraDesc(String emailUsuario);

    /**
     * Búsqueda por Stripe Session ID para evitar procesamiento duplicado en el webhook.
     */
    Optional<Pedido> findByStripeSessionId(String stripeSessionId);

    /**
     * Búsqueda por correo del usuario (adaptado al modelo interno de base de datos).
     */
    @Query("SELECT p FROM Pedido p WHERE p.emailUsuario = ?1 ORDER BY p.fechaCompra DESC")
    List<Pedido> findByUsuarioEmail(String usuarioEmail);

    /**
     * Todos los pedidos ordenados del más reciente al más antiguo (para admin).
     */
    @Query("SELECT p FROM Pedido p ORDER BY p.fechaCompra DESC")
    List<Pedido> findAllOrderByFechaCompraDesc();
}
