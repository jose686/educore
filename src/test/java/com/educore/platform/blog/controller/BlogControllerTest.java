package com.educore.platform.blog.controller;

import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.service.BlogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para BlogController.
 * Valida la correcta exposición y renderización pública del blog.
 */
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogService blogService;

    @Test
    void blogListShouldBePubliclyAccessible() throws Exception {
        when(blogService.obtenerTodosLosArticulos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/blog"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog"))
                .andExpect(model().attributeExists("articulos"));
    }

    @Test
    void articleDetailShouldBePubliclyAccessible() throws Exception {
        String slug = "aperturas-de-ajedrez";
        Articulo mockArticulo = Articulo.builder()
                .id(UUID.randomUUID())
                .titulo("Aperturas de Ajedrez")
                .resumenCorto("Una guía corta.")
                .contenido("<p>Contenido detallado en HTML.</p>")
                .slug(slug)
                .fechaPublicacion(LocalDateTime.now())
                .usuarioId(1L)
                .build();

        when(blogService.obtenerPorSlug(slug)).thenReturn(mockArticulo);

        mockMvc.perform(get("/blog/" + slug))
                .andExpect(status().isOk())
                .andExpect(view().name("articulo"))
                .andExpect(model().attributeExists("articulo"));
    }
}
