package com.educore.platform.store.service;

import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.store.model.DetallePedido;
import com.educore.platform.store.model.EstadoPedido;
import com.educore.platform.store.model.Pedido;
import com.educore.platform.store.repository.PedidoRepository;
import com.educore.platform.store.repository.PaqueteRepository;
import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.lms.service.AulaVirtualService;
import com.educore.platform.users.service.UserPublicService;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio de Pedidos.
 * Gestiona la creación de pedidos desde webhooks de Stripe y el procesamiento
 * de reembolsos con revocación automática de acceso.
 */
@Service
@Transactional
@Slf4j
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final StripeService stripeService;
    private final UserPublicService userPublicService;
    private final InscripcionRepository inscripcionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CatalogoService catalogoService;
    private final PromocionService promocionService;
    private final PaqueteRepository paqueteRepository;
    private final AulaVirtualService aulaVirtualService;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             StripeService stripeService,
                             UserPublicService userPublicService,
                             InscripcionRepository inscripcionRepository,
                             ApplicationEventPublisher eventPublisher,
                             CatalogoService catalogoService,
                             PromocionService promocionService,
                             PaqueteRepository paqueteRepository,
                             AulaVirtualService aulaVirtualService) {
        this.pedidoRepository = pedidoRepository;
        this.stripeService = stripeService;
        this.userPublicService = userPublicService;
        this.inscripcionRepository = inscripcionRepository;
        this.eventPublisher = eventPublisher;
        this.catalogoService = catalogoService;
        this.promocionService = promocionService;
        this.paqueteRepository = paqueteRepository;
        this.aulaVirtualService = aulaVirtualService;
    }

    @Override
    @Transactional
    public Pedido crearPedidoDesdeWebhook(Session session, String itemsMetadata, BigDecimal totalEuros) {
        String sessionId = session.getId();

        // Idempotencia: si ya existe un pedido para esta sesión, lo devolvemos sin crear otro
        Optional<Pedido> existente = pedidoRepository.findByStripeSessionId(sessionId);
        if (existente.isPresent()) {
            log.warn("[PEDIDO] Ya existe un pedido para la sesión Stripe {}. Se omite la creación duplicada.", sessionId);
            return existente.get();
        }

        String email = session.getMetadata() != null ? session.getMetadata().get("email") : null;
        String paymentIntentId = session.getPaymentIntent();

        Pedido pedido = Pedido.builder()
                .stripeSessionId(sessionId)
                .stripePaymentIntentId(paymentIntentId)
                .emailUsuario(email)
                .totalEuros(totalEuros)
                .fechaCompra(LocalDateTime.now())
                .estado(EstadoPedido.COMPLETADO)
                .build();

        String descuentoStr = session.getMetadata() != null ? session.getMetadata().get("descuento_porcentaje") : null;
        int descuentoPorcentaje = 0;
        if (descuentoStr != null) {
            try {
                descuentoPorcentaje = Integer.parseInt(descuentoStr);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Parsear la metadata "producto_id" para construir los DetallePedido
        if (itemsMetadata != null && !itemsMetadata.isBlank()) {
            for (String item : itemsMetadata.split(",")) {
                item = item.trim();
                if (item.isEmpty()) continue;

                int underscoreIdx = item.indexOf('_');
                String tipo = underscoreIdx > 0 ? item.substring(0, underscoreIdx) : "desconocido";
                String referenciaId = underscoreIdx > 0 ? item.substring(underscoreIdx + 1) : item;

                String titulo = tipo.toUpperCase() + " — " + referenciaId;
                BigDecimal precioUnitario = BigDecimal.ZERO;

                try {
                    if ("curso".equalsIgnoreCase(tipo)) {
                        com.educore.platform.store.model.ProductoCurso prod = catalogoService.obtenerPorId(java.util.UUID.fromString(referenciaId));
                        if (prod != null) {
                            titulo = prod.getTitulo();
                            precioUnitario = prod.getPrecio();
                        }
                    } else if ("paquete".equalsIgnoreCase(tipo)) {
                        com.educore.platform.store.model.Paquete paq = paqueteRepository.findById(Long.parseLong(referenciaId)).orElse(null);
                        if (paq != null) {
                            titulo = paq.getTitulo();
                            precioUnitario = paq.getPrecio();
                        }
                    } else if ("servicio".equalsIgnoreCase(tipo)) {
                        if ("servicio_retos_pro".equals(referenciaId)) {
                            titulo = "Acceso a Minijuegos: Retos de Táctica Pro";
                            precioUnitario = new BigDecimal("4.99");
                        } else if ("servicio_analisis_partida".equals(referenciaId)) {
                            titulo = "Análisis de Partida por Maestro FIDE";
                            precioUnitario = new BigDecimal("14.99");
                        }
                    }
                } catch (Exception e) {
                    log.error("[PEDIDO] Error al obtener detalles del producto tipo={}, id={}", tipo, referenciaId, e);
                }

                if (descuentoPorcentaje > 0 && precioUnitario.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal factor = BigDecimal.valueOf(100 - descuentoPorcentaje);
                    precioUnitario = precioUnitario.multiply(factor).divide(BigDecimal.valueOf(100), java.math.RoundingMode.HALF_UP);
                }

                DetallePedido detalle = DetallePedido.builder()
                        .tipo(tipo)
                        .referenciaId(referenciaId)
                        .titulo(titulo)
                        .precioUnitario(precioUnitario)
                        .build();

                pedido.addDetalle(detalle);
            }
        }

        Pedido savedPedido = pedidoRepository.save(pedido);
        log.info("[PEDIDO] ✅ Pedido creado - ID: {}, email: {}, total: {}€, ítems: {}",
                savedPedido.getId(), email, totalEuros, savedPedido.getDetalles().size());

        return savedPedido;
    }

    @Override
    @Transactional
    public void procesarCheckoutCompleted(Session session) {
        if (session == null) {
            log.error("[PEDIDO-WEBHOOK] La sesión recibida es nula.");
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        if (metadata == null) {
            log.error("[PEDIDO-WEBHOOK] La sesión {} no contiene metadata. No se puede procesar.", session.getId());
            return;
        }

        String email = metadata.get("email");
        String tipoProducto = metadata.get("tipo_producto");
        String idProductoStr = metadata.get("producto_id");

        log.info("[PEDIDO-WEBHOOK] Procesando checkout completed - email: {}, tipo_producto: {}, producto_id: {}",
                email, tipoProducto, idProductoStr);

        if (email == null || email.isBlank()) {
            log.error("[PEDIDO-WEBHOOK] Email ausente en la metadata de la sesión: {}", session.getId());
            return;
        }

        // 1. Otorgar suscripción/matrícula
        if ("carrito".equalsIgnoreCase(tipoProducto)) {
            procesarItemsCarrito(email, idProductoStr);
        } else {
            procesarProductoIndividual(email, tipoProducto, idProductoStr);
        }

        // 2. Crear y persistir el Pedido en la BD
        BigDecimal totalEuros = BigDecimal.ZERO;
        if (session.getAmountTotal() != null) {
            totalEuros = BigDecimal.valueOf(session.getAmountTotal()).divide(BigDecimal.valueOf(100));
        }

        crearPedidoDesdeWebhook(session, idProductoStr, totalEuros);
    }

    private void procesarItemsCarrito(String email, String idProductoStr) {
        log.info("[PEDIDO-WEBHOOK] Procesando carrito para usuario '{}', ítems: {}", email, idProductoStr);
        String[] items = idProductoStr.split(",");

        for (String item : items) {
            item = item.trim();
            if (item.isEmpty()) continue;

            try {
                int underscoreIdx = item.indexOf('_');
                if (underscoreIdx <= 0) {
                    log.warn("[PEDIDO-WEBHOOK] Formato de ítem de carrito inválido (sin '_'): '{}'. Se omite.", item);
                    continue;
                }

                String itemTipo = item.substring(0, underscoreIdx);
                String itemIdStr = item.substring(underscoreIdx + 1);

                log.debug("[PEDIDO-WEBHOOK] Procesando ítem carrito - tipo: {}, id: {}", itemTipo, itemIdStr);

                if ("curso".equalsIgnoreCase(itemTipo)) {
                    Long lmsCursoId = resolveLmsCursoId(itemIdStr);
                    eventPublisher.publishEvent(new CursoCompradoEvent(email, lmsCursoId));
                    log.info("[PEDIDO-WEBHOOK] ✅ Evento CursoComprado publicado - usuario: {}, lmsCursoId: {}",
                            email, lmsCursoId);
                } else if ("paquete".equalsIgnoreCase(itemTipo)) {
                    Long paqueteId = Long.parseLong(itemIdStr);
                    promocionService.comprarPaquete(paqueteId, email);
                    log.info("[PEDIDO-WEBHOOK] ✅ Paquete {} procesado para usuario: {}", paqueteId, email);

                } else if ("servicio".equalsIgnoreCase(itemTipo)) {
                    log.info("[PEDIDO-WEBHOOK] Ítem de tipo 'servicio' (id: {}) para usuario {}. Sin acción automática.", itemIdStr, email);

                } else {
                    log.warn("[PEDIDO-WEBHOOK] Tipo de ítem desconocido: '{}'.", itemTipo);
                }

            } catch (Exception e) {
                log.error("[PEDIDO-WEBHOOK] Error al procesar el ítem '{}' para usuario '{}'.", item, email, e);
            }
        }
    }

    private void procesarProductoIndividual(String email, String tipoProducto, String idProductoStr) {
        try {
            if ("curso".equalsIgnoreCase(tipoProducto)) {
                Long lmsCursoId = resolveLmsCursoId(idProductoStr);
                eventPublisher.publishEvent(new CursoCompradoEvent(email, lmsCursoId));
                log.info("[PEDIDO-WEBHOOK] ✅ Evento CursoComprado publicado (individual) - usuario: {}, lmsCursoId: {}",
                        email, lmsCursoId);
            } else if ("paquete".equalsIgnoreCase(tipoProducto)) {
                Long paqueteId = Long.parseLong(idProductoStr);
                promocionService.comprarPaquete(paqueteId, email);
                log.info("[PEDIDO-WEBHOOK] ✅ Paquete {} procesado (individual) para usuario: {}", paqueteId, email);

            } else {
                log.warn("[PEDIDO-WEBHOOK] Tipo de producto no reconocido: '{}'.", tipoProducto);
            }
        } catch (Exception e) {
            log.error("[PEDIDO-WEBHOOK] Error al procesar producto individual tipo='{}', id='{}' para usuario '{}'.",
                    tipoProducto, idProductoStr, email, e);
        }
    }

    private Long resolveLmsCursoId(String itemIdStr) {
        try {
            return Long.parseLong(itemIdStr);
        } catch (NumberFormatException e) {
            log.debug("[PEDIDO-WEBHOOK] '{}' no es un Long, intentando resolver como UUID del catálogo.", itemIdStr);
        }

        try {
            java.util.UUID uuid = java.util.UUID.fromString(itemIdStr);
            com.educore.platform.store.model.ProductoCurso producto = catalogoService.obtenerPorId(uuid);
            if (producto == null) {
                throw new IllegalStateException("CatalogoService devolvió null para UUID: " + uuid);
            }
            return producto.getLmsCursoId();
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo resolver el ID del curso: " + itemIdStr, e);
        }
    }

    @Override
    @Transactional
    public void reembolsarPedido(Long pedidoId) throws Exception {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId));

        if (pedido.getEstado() != EstadoPedido.COMPLETADO) {
            throw new IllegalStateException(
                    "Solo se pueden reembolsar pedidos en estado COMPLETADO. Estado actual: " + pedido.getEstado());
        }

        if (pedido.getStripePaymentIntentId() == null || pedido.getStripePaymentIntentId().isBlank()) {
            throw new IllegalStateException(
                    "El pedido " + pedidoId + " no tiene PaymentIntent de Stripe asociado. No se puede reembolsar.");
        }

        log.info("[REEMBOLSO] Procesando reembolso del pedido {} (PI: {})", pedidoId, pedido.getStripePaymentIntentId());

        // 1. Ejecutar el reembolso en Stripe
        stripeService.crearReembolso(pedido.getStripePaymentIntentId());
        log.info("[REEMBOLSO] ✅ Reembolso creado en Stripe para PI: {}", pedido.getStripePaymentIntentId());

        // 2. Cambiar estado del pedido a REEMBOLSADO
        pedido.setEstado(EstadoPedido.REEMBOLSADO);
        pedidoRepository.save(pedido);

        // 3. Revocar acceso: eliminar/expirar inscripciones de los cursos comprados
        String email = pedido.getEmailUsuario();
        Long studentId = userPublicService.getUserIdByEmail(email).orElse(null);

        if (studentId == null) {
            log.error("[REEMBOLSO] No se encontró usuario con email '{}'. El acceso NO ha sido revocado en el LMS.", email);
            return;
        }

        int inscripcionesRevocadas = 0;

        for (DetallePedido detalle : pedido.getDetalles()) {
            if ("curso".equalsIgnoreCase(detalle.getTipo())) {
                try {
                    Long lmsCursoId = parsearLmsCursoId(detalle.getReferenciaId());
                    if (lmsCursoId != null) {
                        var inscripcionOpt = inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, lmsCursoId);
                        if (inscripcionOpt.isPresent()) {
                            Inscripcion insc = inscripcionOpt.get();
                            if (insc.getFechaFin() == null) {
                                // Acceso permanente: eliminar
                                inscripcionRepository.delete(insc);
                            } else {
                                // Acceso temporal: expirar inmediatamente
                                insc.setFechaFin(LocalDateTime.now());
                                inscripcionRepository.save(insc);
                            }
                            inscripcionesRevocadas++;
                        }
                    }
                } catch (Exception e) {
                    log.error("[REEMBOLSO] Error al revocar acceso al curso '{}' para usuario {}: {}",
                            detalle.getReferenciaId(), email, e.getMessage(), e);
                }
            }
        }

        log.info("[REEMBOLSO] ✅ Acceso revocado para usuario '{}' - inscripciones revocadas: {}",
                email, inscripcionesRevocadas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorEmail(String email) {
        return pedidoRepository.findByEmailUsuarioOrderByFechaCompraDesc(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerTodosLosPedidos() {
        return pedidoRepository.findAllOrderByFechaCompraDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerTodosLosPedidosConDetalles() {
        return pedidoRepository.findAllWithDetallesOrderByFechaCompraDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + id));
    }

    /**
     * Intenta parsear el ID del curso para obtener el lmsCursoId.
     * Soporta formato Long directo. Los UUIDs son referencias al catálogo
     * y en el contexto del reembolso necesitamos el lmsCursoId.
     */
    private Long parsearLmsCursoId(String referenciaId) {
        try {
            return Long.parseLong(referenciaId);
        } catch (NumberFormatException e) {
            // Si es un UUID, necesitamos buscarlo en el catálogo
            try {
                java.util.UUID uuid = java.util.UUID.fromString(referenciaId);
                com.educore.platform.store.model.ProductoCurso prod = catalogoService.obtenerPorId(uuid);
                if (prod != null) {
                    return prod.getLmsCursoId();
                }
            } catch (Exception ex) {
                log.error("[REEMBOLSO] Error al resolver lmsCursoId desde UUID '{}': {}", referenciaId, ex.getMessage());
            }
            return null;
        }
    }

    @Override
    @Transactional
    public void forzarMatriculacionManual(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId));
        String email = pedido.getEmailUsuario();
        log.info("[MATRICULACION-MANUAL] 🛠️ Forzando matriculación manual para el pedido #{} (Usuario: {})", pedidoId, email);

        for (DetallePedido detalle : pedido.getDetalles()) {
            String tipo = detalle.getTipo();
            String referenciaId = detalle.getReferenciaId();
            try {
                if ("curso".equalsIgnoreCase(tipo)) {
                    Long lmsCursoId = parsearLmsCursoId(referenciaId);
                    if (lmsCursoId != null) {
                        aulaVirtualService.matricularAlumno(email, lmsCursoId);
                        log.info("[MATRICULACION-MANUAL] ✅ Matriculado en curso lmsCursoId: {}", lmsCursoId);
                    } else {
                        log.error("[MATRICULACION-MANUAL] ❌ No se pudo resolver lmsCursoId para referencia: {}", referenciaId);
                    }
                } else if ("paquete".equalsIgnoreCase(tipo)) {
                    Long paqueteId = Long.parseLong(referenciaId);
                    promocionService.comprarPaquete(paqueteId, email);
                    log.info("[MATRICULACION-MANUAL] ✅ Paquete {} procesado", paqueteId);
                } else {
                    log.info("[MATRICULACION-MANUAL] Ítem tipo '{}' no requiere matriculación automática", tipo);
                }
            } catch (Exception e) {
                log.error("[MATRICULACION-MANUAL] ❌ Error procesando ítem '{}': {}", referenciaId, e.getMessage(), e);
            }
        }
    }
}
