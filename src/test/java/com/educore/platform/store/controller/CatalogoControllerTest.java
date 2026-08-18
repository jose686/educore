package com.educore.platform.store.controller;

import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.service.CatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para el controlador CatalogoController.
 * Valida la accesibilidad de la ruta de catálogo y el comportamiento del endpoint de compra.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogoService catalogoService;

    @Test
    void catalogShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))
                .andExpect(model().attributeExists("productos"));
    }

    @Test
    void coursesShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/cursos"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))
                .andExpect(model().attributeExists("productos"));
    }

    @Test
    void buyingAnonymousShouldRedirectToLogin() throws Exception {
        UUID productId = UUID.randomUUID();
        
        // POST sin autenticación debe requerir login (redirección)
        mockMvc.perform(post("/comprar/" + productId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com")
    void buyingAuthenticatedShouldPublishEventAndRedirect() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductoCurso mockProduct = ProductoCurso.builder()
                .id(productId)
                .titulo("Curso de Táctica Avanzada")
                .precio(BigDecimal.valueOf(49.99))
                .lmsCursoId(12L)
                .estado("PUBLISHED")
                .build();

        // Stub del servicio para que no lance excepción
        when(catalogoService.obtenerPorId(productId)).thenReturn(mockProduct);

        // POST autenticado (con token CSRF) debe procesarse con redirección al catálogo y éxito
        mockMvc.perform(post("/comprar/" + productId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogo?compra_exitosa=true"));
    }
}
