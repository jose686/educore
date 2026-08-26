package com.educore.platform.store.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.lms.service.AulaVirtualService;
import com.educore.platform.store.model.DetallePedido;
import com.educore.platform.store.model.EstadoPedido;
import com.educore.platform.store.model.Paquete;
import com.educore.platform.store.model.Pedido;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.repository.PaqueteRepository;
import com.educore.platform.store.repository.PedidoRepository;
import com.stripe.model.checkout.Session;
import com.educore.platform.users.service.UserPublicService;
import com.educore.platform.store.event.CursoCompradoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PedidoServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private StripeService stripeService;
    @Mock private UserPublicService userPublicService;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CatalogoService catalogoService;
    @Mock private PromocionService promocionService;
    @Mock private PaqueteRepository paqueteRepository;
    @Mock private AulaVirtualService aulaVirtualService;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private Session stripeSession;
    private Pedido pedido;
    private Long pedidoId;
    private String email;

    @BeforeEach
    void setUp() {
        pedidoId = 1L;
        email = "student@educore.com";

        // Instanciar Session de Stripe a partir de JSON usando el GSON interno de Stripe
        String json = "{"
                + "\"id\": \"sess_123\","
                + "\"payment_intent\": \"pi_123\","
                + "\"amount_total\": 4990,"
                + "\"metadata\": {"
                + "  \"email\": \"student@educore.com\","
                + "  \"tipo_producto\": \"carrito\","
                + "  \"producto_id\": \"curso_100,paquete_200,servicio_servicio_retos_pro\""
                + "}"
                + "}";
        stripeSession = com.stripe.net.ApiResource.GSON.fromJson(json, Session.class);

        pedido = Pedido.builder()
                .id(pedidoId)
                .stripeSessionId("sess_123")
                .stripePaymentIntentId("pi_123")
                .emailUsuario(email)
                .totalEuros(new BigDecimal("49.90"))
                .estado(EstadoPedido.COMPLETADO)
                .build();
    }

    @Test
    void crearPedidoDesdeWebhook_ShouldReturnExisting_WhenSessionAlreadyRegistered() {
        when(pedidoRepository.findByStripeSessionId("sess_123")).thenReturn(Optional.of(pedido));

        Pedido result = pedidoService.crearPedidoDesdeWebhook(stripeSession, "curso_100", BigDecimal.TEN);

        assertNotNull(result);
        assertEquals(pedido, result);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void crearPedidoDesdeWebhook_ShouldCreateAndSave_WhenNew() {
        when(pedidoRepository.findByStripeSessionId("sess_123")).thenReturn(Optional.empty());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        // Mock para resolver curso
        ProductoCurso prod = ProductoCurso.builder().titulo("Curso X").precio(BigDecimal.TEN).build();
        when(catalogoService.obtenerPorId(any(UUID.class))).thenReturn(prod);

        // Mock para resolver paquete
        Paquete paq = Paquete.builder().titulo("Pack Y").precio(BigDecimal.valueOf(20)).build();
        when(paqueteRepository.findById(200L)).thenReturn(Optional.of(paq));

        Pedido result = pedidoService.crearPedidoDesdeWebhook(stripeSession, "curso_" + UUID.randomUUID() + ",paquete_200,servicio_servicio_retos_pro", new BigDecimal("49.90"));

        assertNotNull(result);
        assertEquals("sess_123", result.getStripeSessionId());
        assertEquals("pi_123", result.getStripePaymentIntentId());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void procesarCheckoutCompleted_ShouldDoNothing_WhenSessionNull() {
        pedidoService.procesarCheckoutCompleted(null);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void procesarCheckoutCompleted_ShouldDoNothing_WhenSessionHasNoMetadata() {
        Session sessionNoMetadata = com.stripe.net.ApiResource.GSON.fromJson("{\"id\": \"sess_empty\"}", Session.class);
        pedidoService.procesarCheckoutCompleted(sessionNoMetadata);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void procesarCheckoutCompleted_ShouldProcessCartAndSavePedido_WhenValid() {
        when(pedidoRepository.findByStripeSessionId("sess_123")).thenReturn(Optional.empty());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        pedidoService.procesarCheckoutCompleted(stripeSession);

        verify(eventPublisher, times(1)).publishEvent(any(CursoCompradoEvent.class));
        verify(promocionService, times(1)).comprarPaquete(200L, email);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void procesarCheckoutCompleted_ShouldProcessIndividualProduct_WhenNotCart() {
        String json = "{"
                + "\"id\": \"sess_single\","
                + "\"amount_total\": 1000,"
                + "\"metadata\": {"
                + "  \"email\": \"student@educore.com\","
                + "  \"tipo_producto\": \"curso\","
                + "  \"producto_id\": \"100\""
                + "}"
                + "}";
        Session session = com.stripe.net.ApiResource.GSON.fromJson(json, Session.class);

        when(pedidoRepository.findByStripeSessionId("sess_single")).thenReturn(Optional.empty());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        pedidoService.procesarCheckoutCompleted(session);

        verify(eventPublisher, times(1)).publishEvent(any(CursoCompradoEvent.class));
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void reembolsarPedido_ShouldThrowException_WhenPedidoNotFound() throws Exception {
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.reembolsarPedido(pedidoId));
    }

    @Test
    void reembolsarPedido_ShouldThrowException_WhenPedidoNotCompletado() {
        Pedido refPedido = Pedido.builder().estado(EstadoPedido.REEMBOLSADO).build();
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(refPedido));
        assertThrows(IllegalStateException.class, () -> pedidoService.reembolsarPedido(pedidoId));
    }

    @Test
    void reembolsarPedido_ShouldThrowException_WhenNoStripePaymentIntent() {
        Pedido refPedido = Pedido.builder().estado(EstadoPedido.COMPLETADO).stripePaymentIntentId(null).build();
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(refPedido));
        assertThrows(IllegalStateException.class, () -> pedidoService.reembolsarPedido(pedidoId));
    }

    @Test
    void reembolsarPedido_ShouldRefundInStripeAndRevokeAccess() throws Exception {
        DetallePedido detail = DetallePedido.builder().tipo("curso").referenciaId("100").build();
        pedido.addDetalle(detail);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(2L));
        
        Inscripcion ins = Inscripcion.builder().fechaFin(null).build(); // Permanente
        when(inscripcionRepository.findByStudentIdAndLmsCursoId(2L, 100L)).thenReturn(Optional.of(ins));

        pedidoService.reembolsarPedido(pedidoId);

        verify(stripeService, times(1)).crearReembolso("pi_123");
        assertEquals(EstadoPedido.REEMBOLSADO, pedido.getEstado());
        verify(inscripcionRepository, times(1)).delete(ins);
        verify(pedidoRepository, times(1)).save(pedido);
    }

    @Test
    void obtenerPedidosPorEmail_ShouldReturnList() {
        when(pedidoRepository.findByEmailUsuarioOrderByFechaCompraDesc(email)).thenReturn(Collections.emptyList());
        List<Pedido> result = pedidoService.obtenerPedidosPorEmail(email);
        assertNotNull(result);
    }

    @Test
    void obtenerTodosLosPedidos_ShouldReturnList() {
        when(pedidoRepository.findAllOrderByFechaCompraDesc()).thenReturn(Collections.emptyList());
        List<Pedido> result = pedidoService.obtenerTodosLosPedidos();
        assertNotNull(result);
    }

    @Test
    void obtenerTodosLosPedidosConDetalles_ShouldUseFetchMethod() {
        when(pedidoRepository.findAllWithDetallesOrderByFechaCompraDesc()).thenReturn(Collections.emptyList());

        List<Pedido> result = pedidoService.obtenerTodosLosPedidosConDetalles();

        assertNotNull(result);
        verify(pedidoRepository).findAllWithDetallesOrderByFechaCompraDesc();
    }

    @Test
    void obtenerPedidoPorId_WhenExists_ShouldReturn() {
        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        Pedido result = pedidoService.obtenerPedidoPorId(pedidoId);
        assertNotNull(result);
        assertEquals(pedido, result);
    }

    @Test
    void forzarMatriculacionManual_ShouldEnrollAllItems() {
        DetallePedido cursoDetail = DetallePedido.builder().tipo("curso").referenciaId("100").build();
        DetallePedido paqueteDetail = DetallePedido.builder().tipo("paquete").referenciaId("200").build();
        pedido.addDetalle(cursoDetail);
        pedido.addDetalle(paqueteDetail);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));

        pedidoService.forzarMatriculacionManual(pedidoId);

        verify(aulaVirtualService, times(1)).matricularAlumno(email, 100L);
        verify(promocionService, times(1)).comprarPaquete(200L, email);
    }
}
