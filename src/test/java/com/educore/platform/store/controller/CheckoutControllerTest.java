package com.educore.platform.store.controller;

import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.dto.CartItem;
import com.educore.platform.store.service.CatalogoService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para CheckoutController con soporte de Carrito de Compras.
 */
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogoService catalogoService;

    @MockBean
    private PromocionService promocionService;

    @MockBean
    private StripeService stripeService;

    @Test
    void checkoutGetWithoutAuthShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/carrito"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void checkoutGetWithEmptyCartShouldShowEmptyState() throws Exception {
        mockMvc.perform(get("/carrito"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("carritoVacio", true));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void checkoutGetWithItemsShouldShowSummary() throws Exception {
        MockHttpSession session = new MockHttpSession();
        List<CartItem> carrito = new ArrayList<>();
        carrito.add(new CartItem("1", "curso", "Curso de Ajedrez", BigDecimal.valueOf(50.00)));
        session.setAttribute("carrito", carrito);

        mockMvc.perform(get("/carrito").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("subtotal", BigDecimal.valueOf(50.00)))
                .andExpect(model().attribute("precioFinal", BigDecimal.valueOf(50.00)));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void checkoutGetWithCouponShouldApplyDiscountToCart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        List<CartItem> carrito = new ArrayList<>();
        carrito.add(new CartItem("1", "curso", "Curso de Ajedrez", BigDecimal.valueOf(100.00)));
        session.setAttribute("carrito", carrito);

        Cupon mockCoupon = Cupon.builder()
                .codigo("PROMO20")
                .tipo(TipoCupon.DESCUENTO)
                .descuentoPorcentaje(20)
                .activo(true)
                .build();

        when(promocionService.validarYObtenerCupon("PROMO20")).thenReturn(mockCoupon);

        mockMvc.perform(get("/carrito")
                        .session(session)
                        .param("coupon", "PROMO20"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attribute("cupDesc", 20))
                .andExpect(model().attribute("precioFinal", BigDecimal.valueOf(80.00)))
                .andExpect(model().attributeExists("cuponExito"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void addingToCartShouldUpdateSessionList() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductoCurso mockProduct = ProductoCurso.builder()
                .id(productId)
                .titulo("Curso de Táctica")
                .precio(BigDecimal.valueOf(25.00))
                .lmsCursoId(1L)
                .estado("PUBLISHED")
                .build();

        when(catalogoService.obtenerPorId(productId)).thenReturn(mockProduct);

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/carrito/agregar")
                        .session(session)
                        .param("tipo", "curso")
                        .param("id", productId.toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogo?agregado_exito=true"));

        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");
        assertNotNull(carrito);
        assertEquals(1, carrito.size());
        assertEquals("Curso de Táctica", carrito.get(0).getTitulo());
        assertEquals(BigDecimal.valueOf(25.00), carrito.get(0).getPrecio());
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void addingServiceToCartShouldRedirectToJuegos() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/carrito/agregar")
                        .session(session)
                        .param("tipo", "servicio")
                        .param("id", "servicio_retos_pro")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/juegos?agregado_exito=true"));

        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");
        assertNotNull(carrito);
        assertEquals(1, carrito.size());
        assertEquals("Acceso a Minijuegos: Retos de Táctica Pro", carrito.get(0).getTitulo());
        assertEquals(new BigDecimal("4.99"), carrito.get(0).getPrecio());
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void deletingFromCartShouldRemoveFromSessionList() throws Exception {
        MockHttpSession session = new MockHttpSession();
        List<CartItem> carrito = new ArrayList<>();
        carrito.add(new CartItem("1", "curso", "Curso de Ajedrez", BigDecimal.valueOf(50.00)));
        session.setAttribute("carrito", carrito);

        mockMvc.perform(post("/carrito/eliminar")
                        .session(session)
                        .param("tipo", "curso")
                        .param("id", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"));

        List<CartItem> resultCart = (List<CartItem>) session.getAttribute("carrito");
        assertNotNull(resultCart);
        assertTrue(resultCart.isEmpty());
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void checkoutPayShouldRedirectToStripe() throws Exception {
        MockHttpSession session = new MockHttpSession();
        List<CartItem> carrito = new ArrayList<>();
        carrito.add(new CartItem("1", "curso", "Curso de Ajedrez", BigDecimal.valueOf(100.00)));
        session.setAttribute("carrito", carrito);

        Session mockSession = org.mockito.Mockito.mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_999");

        when(stripeService.crearSesionPagoCarrito(
                any(),
                eq(0),
                eq("alumno@educore.com"),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(mockSession);

        mockMvc.perform(post("/carrito/pay")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/pay/cs_test_999"));
        
        // El carrito debe haber sido removido de la sesión tras pagar
        assertNull(session.getAttribute("carrito"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void checkoutSuccessShouldRenderSuccessView() throws Exception {
        mockMvc.perform(get("/checkout/success"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout-success"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void checkoutCancelShouldRedirectToCatalog() throws Exception {
        mockMvc.perform(get("/checkout/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogo?pago_cancelado=true"));
    }

}
