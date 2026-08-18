package com.educore.platform.store.controller;

import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.service.RecursoInteractivoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración actualizadas para validar la seguridad en la Biblioteca de Recursos Interactivos.
 * Comprueba que solo ROLE_ADMIN pueda gestionar (CRUD) la biblioteca.
 */
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MinijuegoSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecursoInteractivoService recursoService;

    @Test
    void anonymousUser_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/biblioteca-interactivos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "student@educore.com", roles = "STUDENT")
    void studentUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/admin/biblioteca-interactivos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessListado() throws Exception {
        when(recursoService.obtenerBibliotecaFiltrada(any(), any(), any())).thenReturn(Collections.emptyList());
        when(recursoService.obtenerTodasEtiquetas()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/biblioteca-interactivos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-biblioteca"))
                .andExpect(model().attributeExists("recursos"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessCreateForm() throws Exception {
        mockMvc.perform(get("/admin/biblioteca-interactivos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-recurso"))
                .andExpect(model().attributeExists("recurso"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateRecurso() throws Exception {
        RecursoInteractivo mockRecurso = RecursoInteractivo.builder().id(1L).identificador("NEW-ID").build();
        when(recursoService.guardar(any(RecursoInteractivo.class))).thenReturn(mockRecurso);

        mockMvc.perform(post("/admin/biblioteca-interactivos/nuevo")
                .param("identificador", "NEW-ID")
                .param("titulo", "Nuevo Recurso")
                .param("htmlUrl", "/minijuegos/demo.html")
                .param("esGratis", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos?success=create"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteRecurso() throws Exception {
        doNothing().when(recursoService).eliminar(1L);

        mockMvc.perform(post("/admin/biblioteca-interactivos/eliminar/1")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos?success=delete"));
    }
}
