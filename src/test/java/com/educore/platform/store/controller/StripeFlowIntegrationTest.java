package com.educore.platform.store.controller;

import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.repository.ProductoCursoRepository;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.store.service.StripeService;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración completa del flujo de compra con carrito, cupones y Stripe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class StripeFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoCursoRepository productoCursoRepository;

    @MockBean
    private StripeService stripeService;

    @MockBean
    private PromocionService promocionService;

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void testFullFlowAddApplyCouponAndRedirect() throws Exception {
        // 1. Registrar un curso en base de datos H2
        ProductoCurso curso = ProductoCurso.builder()
                .titulo("Curso de Prueba de Flujo Completo")
                .precio(BigDecimal.valueOf(100.00))
                .lmsCursoId(99L)
                .estado("PUBLISHED")
                .build();
        curso = productoCursoRepository.save(curso);
        UUID cursoId = curso.getId();

        MockHttpSession session = new MockHttpSession();

        // 2. Añadir el curso al carrito
        mockMvc.perform(post("/carrito/agregar")
                        .session(session)
                        .param("tipo", "curso")
                        .param("id", cursoId.toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogo?agregado_exito=true"));

        // 3. Simular la aplicación del cupón
        Cupon cupon = Cupon.builder()
                .codigo("INTEGRACION20")
                .tipo(TipoCupon.DESCUENTO)
                .descuentoPorcentaje(20)
                .activo(true)
                .build();
        when(promocionService.validarYObtenerCupon("INTEGRACION20")).thenReturn(cupon);

        mockMvc.perform(get("/carrito")
                        .session(session)
                        .param("coupon", "INTEGRACION20"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("precioFinal", BigDecimal.valueOf(80.00)))
                .andExpect(model().attribute("cupDesc", 20));

        // 4. Simular la redirección a Stripe
        Session mockStripeSession = mock(Session.class);
        when(mockStripeSession.getId()).thenReturn("cs_flow_test_id");
        when(mockStripeSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_flow_test_id");

        when(stripeService.crearSesionPagoCarrito(
                any(),
                eq(20),
                eq("alumno@educore.com"),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(mockStripeSession);

        mockMvc.perform(post("/api/v1/checkout/create-session")
                        .session(session)
                        .param("coupon", "INTEGRACION20")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cs_flow_test_id"))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/pay/cs_flow_test_id"));
    }
}
