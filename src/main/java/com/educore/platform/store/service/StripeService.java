package com.educore.platform.store.service;

import com.stripe.Stripe;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Servicio para interactuar con la API de Stripe y crear sesiones de pago de Checkout.
 */
@Service
public class StripeService {

    @Value("${stripe.api.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.trim().isEmpty()) {
            Stripe.apiKey = secretKey;
        } else {
            Stripe.apiKey = "sk_test_dummy";
        }
    }

    /**
     * Crea una sesión de pago en Stripe para redireccionar al usuario al Checkout seguro de Stripe.
     *
     * @param titulo        Nombre del producto (curso o paquete).
     * @param precio        Monto final del producto.
     * @param emailUsuario  Correo electrónico del usuario.
     * @param tipoProducto  Tipo del producto ("curso" o "paquete").
     * @param productoId    Identificador del producto en formato cadena.
     * @param successUrl    URL de éxito a la que se redirige tras el pago.
     * @param cancelUrl     URL de cancelación si el usuario interrumpe el pago.
     * @return El objeto Session de Stripe creado.
     * @throws Exception Si ocurre un error al invocar la API de Stripe.
     */
    public Session crearSesionPago(
            String titulo,
            BigDecimal precio,
            String emailUsuario,
            String tipoProducto,
            String productoId,
            Long userId,
            Long cursoId,
            String successUrl,
            String cancelUrl) throws Exception {

        // Stripe requiere la cantidad expresada en céntimos (ej: 19.99 EUR -> 1999 céntimos)
        long cantidadCentimos = precio.multiply(new BigDecimal("100")).longValue();

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(emailUsuario)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("eur")
                                                .setUnitAmount(cantidadCentimos)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(titulo)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("email", emailUsuario)
                .putMetadata("tipo_producto", tipoProducto)
                .putMetadata("producto_id", productoId);

        if (userId != null) {
            builder.putMetadata("userId", String.valueOf(userId));
            builder.putMetadata("usuarioId", String.valueOf(userId));
        }
        if (cursoId != null) {
            builder.putMetadata("cursoId", String.valueOf(cursoId));
            builder.putMetadata("cursoIds", String.valueOf(cursoId));
        }

        return Session.create(builder.build());
    }

    /**
     * Crea una sesión de pago en Stripe para múltiples ítems en el carrito.
     */
    public Session crearSesionPagoCarrito(
            java.util.List<com.educore.platform.store.dto.CartItem> items,
            int descuentoPorcentaje,
            String emailUsuario,
            Long userId,
            Long cursoId,
            String cursoIds,
            String successUrl,
            String cancelUrl) throws Exception {

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(emailUsuario);

        java.util.StringJoiner idsJoiner = new java.util.StringJoiner(",");

        for (com.educore.platform.store.dto.CartItem item : items) {
            BigDecimal precioFinalItem = item.getPrecio();
            if (descuentoPorcentaje > 0) {
                BigDecimal factor = BigDecimal.valueOf(100 - descuentoPorcentaje);
                precioFinalItem = precioFinalItem.multiply(factor).divide(BigDecimal.valueOf(100), java.math.RoundingMode.HALF_UP);
            }
            long cantidadCentimos = precioFinalItem.multiply(new BigDecimal("100")).longValue();

            builder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("eur")
                                            .setUnitAmount(cantidadCentimos)
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.getTitulo() + " (" + item.getTipo().toUpperCase() + ")")
                                                            .build()
                                            )
                                            .build()
                                    )
                            .build()
            );

            // Guardar ID en formato: tipo_id original (ej: curso_12, paquete_5, servicio_retos_pro)
            idsJoiner.add(item.getTipo() + "_" + item.getId());
        }

        builder.putMetadata("email", emailUsuario);
        builder.putMetadata("tipo_producto", "carrito");
        builder.putMetadata("producto_id", idsJoiner.toString());
        builder.putMetadata("descuento_porcentaje", String.valueOf(descuentoPorcentaje));
        if (userId != null) {
            builder.putMetadata("userId", String.valueOf(userId));
            builder.putMetadata("usuarioId", String.valueOf(userId));
        }
        if (cursoId != null) {
            builder.putMetadata("cursoId", String.valueOf(cursoId));
        }
        if (cursoIds != null && !cursoIds.isBlank()) {
            builder.putMetadata("cursoIds", cursoIds);
        }

        return Session.create(builder.build());
    }

    /**
     * Crea un reembolso completo en Stripe para el PaymentIntent indicado.
     *
     * @param paymentIntentId El ID del PaymentIntent de Stripe a reembolsar.
     * @return El objeto Refund creado por Stripe.
     * @throws Exception Si ocurre un error en la API de Stripe.
     */
    public Refund crearReembolso(String paymentIntentId) throws Exception {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();
        return Refund.create(params);
    }
}
