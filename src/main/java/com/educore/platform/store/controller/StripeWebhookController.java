package com.educore.platform.store.controller;

import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.store.service.PedidoService;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.store.model.ProductoCurso;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Controlador REST para recibir y procesar los Webhooks de Stripe de forma asíncrona.
 */
@RestController
@Slf4j
public class StripeWebhookController {

    private final ApplicationEventPublisher eventPublisher;
    private final PromocionService promocionService;
    private final CatalogoService catalogoService;
    private final PedidoService pedidoService;

    @Value("${stripe.api.webhook-secret:}")
    private String webhookSecret;

    public StripeWebhookController(ApplicationEventPublisher eventPublisher,
                                   PromocionService promocionService,
                                   CatalogoService catalogoService,
                                   PedidoService pedidoService) {
        this.eventPublisher = eventPublisher;
        this.promocionService = promocionService;
        this.catalogoService = catalogoService;
        this.pedidoService = pedidoService;
    }

    /**
     * Endpoint del webhook que recibe eventos de Stripe. Escucha 'checkout.session.completed'
     * y concede el acceso correspondiente de manera automática.
     */
    @PostMapping("/api/v1/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        Event event;
        try {
            if (webhookSecret != null && !webhookSecret.trim().isEmpty() && sigHeader != null) {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } else {
                // Fallback de desarrollo para cuando no hay webhook secret configurado
                log.warn("[WEBHOOK] Sin webhook-secret configurado. Usando deserialización sin firma (solo desarrollo).");
                event = ApiResource.GSON.fromJson(payload, Event.class);
            }
        } catch (SignatureVerificationException e) {
            log.error("[WEBHOOK] Firma de webhook de Stripe no válida", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma no válida");
        } catch (Exception e) {
            log.error("[WEBHOOK] Error al procesar la firma del webhook de Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error de procesamiento");
        }

        log.info("[WEBHOOK] Evento Stripe recibido: type={}, id={}", event.getType(), event.getId());

        if ("checkout.session.completed".equals(event.getType())) {
            procesarSesionCompletada(event);
        }

        return ResponseEntity.ok("Evento recibido y procesado correctamente");
    }

    /**
    /**
     * Procesa el evento checkout.session.completed extrayendo el objeto de sesión
     * y delegando todo el procesamiento transaccional al servicio de pedidos.
     */
    private void procesarSesionCompletada(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        Session session = null;

        if (dataObjectDeserializer.getObject().isPresent()) {
            session = (Session) dataObjectDeserializer.getObject().get();
        } else {
            try {
                session = (Session) dataObjectDeserializer.deserializeUnsafe();
                log.warn("[WEBHOOK] Se usó deserialización insegura para el evento {} " +
                        "(posible diferencia de versión de API Stripe)", event.getId());
            } catch (Exception e) {
                log.error("[WEBHOOK] Error crítico: no se pudo deserializar el objeto de sesión " +
                        "del evento {}. La inscripción NO se procesará.", event.getId(), e);
                return;
            }
        }

        if (session == null) {
            log.warn("[WEBHOOK] Objeto de sesión nulo para evento {}. Se omite el procesamiento.", event.getId());
            return;
        }

        try {
            pedidoService.procesarCheckoutCompleted(session);
            log.info("[WEBHOOK] ✅ Sesión completada procesada correctamente: {}", session.getId());
        } catch (Exception e) {
            log.error("[WEBHOOK] Error al procesar sesión completada: {}", session.getId(), e);
        }
    }
}
