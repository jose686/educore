package com.educore.platform.store.service;

import com.educore.platform.store.model.Pedido;
import com.stripe.model.checkout.Session;

import java.util.List;

/**
 * Contrato del servicio de gestión de Pedidos de compra.
 */
public interface PedidoService {

    /**
     * Crea y persiste un pedido a partir de los datos de una sesión de Stripe completada.
     * Si ya existe un pedido con ese stripeSessionId, retorna el existente (idempotencia).
     *
     * @param session      La sesión de Stripe con metadata y payment intent.
     * @param itemsMetadata El valor del campo metadata "producto_id" codificado como String.
     * @param totalEuros   El importe total en euros (amount_total de Stripe / 100).
     * @return El pedido creado o el existente.
     */
    Pedido crearPedidoDesdeWebhook(Session session, String itemsMetadata, java.math.BigDecimal totalEuros);

    /**
     * Ejecuta un reembolso completo en Stripe, cambia el estado del pedido a REEMBOLSADO
     * y revoca la matrícula/suscripción del alumno en el Aula Virtual.
     *
     * @param pedidoId ID del pedido a reembolsar.
     * @throws IllegalStateException Si el pedido no está en estado COMPLETADO.
     * @throws Exception             Si la API de Stripe devuelve un error.
     */
    void reembolsarPedido(Long pedidoId) throws Exception;

    /**
     * Obtiene todos los pedidos de un usuario ordenados por fecha descendente.
     */
    List<Pedido> obtenerPedidosPorEmail(String email);

    /**
     * Obtiene todos los pedidos del sistema (para el panel de administración).
     */
    List<Pedido> obtenerTodosLosPedidos();

    /**
     * Obtiene los pedidos del panel administrativo con sus detalles cargados.
     */
    List<Pedido> obtenerTodosLosPedidosConDetalles();

    /**
     * Procesa la sesión completada de Stripe (checkout.session.completed) de forma transactional.
     * Resuelve los accesos e inscribe/suscribe al usuario, y crea el pedido correspondiente.
     */
    void procesarCheckoutCompleted(Session session);

    /**
     * Obtiene un pedido por su ID.
     */
    Pedido obtenerPedidoPorId(Long id);

    /**
     * Fuerza la matriculación manual de un alumno en todos los cursos o paquetes incluidos en un pedido.
     *
     * @param pedidoId ID del pedido a procesar.
     */
    void forzarMatriculacionManual(Long pedidoId);
}
