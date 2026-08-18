package com.educore.platform.store.model;

/**
 * Estado del ciclo de vida de un Pedido de compra.
 */
public enum EstadoPedido {
    /** El pago ha sido iniciado pero aún no confirmado por Stripe. */
    PENDIENTE,
    /** El pago ha sido confirmado y el acceso concedido. */
    COMPLETADO,
    /** El pago ha sido reembolsado y el acceso revocado. */
    REEMBOLSADO
}
