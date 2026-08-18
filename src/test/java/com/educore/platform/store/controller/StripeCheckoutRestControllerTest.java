package com.educore.platform.store.controller;

import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.dto.CartItem;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.store.service.StripeService;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para StripeCheckoutRestController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StripeCheckoutRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StripeService stripeService;

    @MockBean
    private PromocionService promocionService;

    @Test
    void createSessionAnonymousShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/create-session")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void createSessionWithEmptyCartShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/create-session")
                        .sessionAttr("carrito", Collections.emptyList())
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El carrito de compras está vacío."));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void createSessionWithValidCartAndCouponShouldReturnSessionUrl() throws Exception {
        List<CartItem> carrito = new ArrayList<>();
        carrito.add(CartItem.builder()
                .id("1")
                .titulo("Curso de Ajedrez")
                .precio(BigDecimal.valueOf(10.00))
                .tipo("curso")
                .build());

        Cupon mockCupon = Cupon.builder()
                .codigo("DISC10")
                .tipo(TipoCupon.DESCUENTO)
                .descuentoPorcentaje(10)
                .activo(true)
                .build();

        Session mockSession = mock(Session.class);
        when(mockSession.getId()).thenReturn("cs_test_id");
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_id");

        when(promocionService.validarYObtenerCupon("DISC10")).thenReturn(mockCupon);
        when(stripeService.crearSesionPagoCarrito(
                any(), eq(10), eq("alumno@educore.com"), any(), any(), any(), any(), any()
        )).thenReturn(mockSession);

        mockMvc.perform(post("/api/v1/checkout/create-session")
                        .sessionAttr("carrito", carrito)
                        .param("coupon", "DISC10")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cs_test_id"))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/pay/cs_test_id"));
    }
}
