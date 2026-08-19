package com.educore.platform.admin.controller;

import com.educore.platform.media.model.CategoriaMedia;
import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.service.RecursoInteractivoService;
import com.educore.platform.blog.service.BlogService;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.media.service.MediaService;
import com.educore.platform.users.service.UsuarioService;
import com.educore.platform.lms.service.LmsService;
import com.educore.platform.store.service.PedidoService;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.users.repository.TicketSoporteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para AdminBibliotecaController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBibliotecaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecursoInteractivoService recursoService;

    @MockBean
    private BlogService blogService;

    @MockBean
    private CatalogoService catalogoService;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private LmsService lmsService;

    @MockBean
    private PromocionService promocionService;

    @MockBean
    private PedidoService pedidoService;

    @MockBean
    private TicketSoporteRepository ticketSoporteRepository;

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldListBiblioteca() throws Exception {
        when(recursoService.obtenerBibliotecaFiltrada(any(), any(), any())).thenReturn(Collections.emptyList());
        when(recursoService.obtenerTodasEtiquetas()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/biblioteca-interactivos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-biblioteca"))
                .andExpect(model().attributeExists("recursos", "etiquetas"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldShowNewRecursoForm() throws Exception {
        mockMvc.perform(get("/admin/biblioteca-interactivos/nuevo")
                        .param("htmlUrl", "game.html")
                        .param("tagsRaw", "tag1,tag2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-recurso"))
                .andExpect(model().attributeExists("recurso", "tagsRaw"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateRecurso_WhenValid() throws Exception {
        RecursoInteractivo form = RecursoInteractivo.builder()
                .titulo("New Game")
                .esGratis(true)
                .build();

        mockMvc.perform(post("/admin/biblioteca-interactivos/nuevo")
                        .flashAttr("recurso", form)
                        .param("tagsRaw", "tag1,tag2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos?success=create"));

        verify(recursoService, times(1)).guardar(any(RecursoInteractivo.class));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldRedirectToForm_WhenCreateFails() throws Exception {
        RecursoInteractivo form = RecursoInteractivo.builder().titulo("Fail Game").build();
        doThrow(new IllegalArgumentException("id_exists")).when(recursoService).guardar(any());

        mockMvc.perform(post("/admin/biblioteca-interactivos/nuevo")
                        .flashAttr("recurso", form)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos/nuevo?error=id_exists"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldShowEditRecursoForm() throws Exception {
        RecursoInteractivo existing = RecursoInteractivo.builder()
                .id(10L)
                .etiquetas(Set.of("tag1", "tag2"))
                .build();
        when(recursoService.obtenerPorId(10L)).thenReturn(existing);

        mockMvc.perform(get("/admin/biblioteca-interactivos/editar/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-recurso"))
                .andExpect(model().attributeExists("recurso", "tagsRaw"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUpdateRecurso_WhenValid() throws Exception {
        RecursoInteractivo existing = RecursoInteractivo.builder().id(10L).esGratis(false).costeCreditos(10).build();
        RecursoInteractivo form = RecursoInteractivo.builder().titulo("Updated").esGratis(false).costeCreditos(20).build();

        when(recursoService.obtenerPorId(10L)).thenReturn(existing);

        mockMvc.perform(post("/admin/biblioteca-interactivos/editar/10")
                        .flashAttr("recurso", form)
                        .param("tagsRaw", "tag3")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos?success=update"));

        verify(recursoService, times(1)).guardar(existing);
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldRedirectToEditForm_WhenUpdateFails() throws Exception {
        RecursoInteractivo existing = RecursoInteractivo.builder().id(10L).build();
        RecursoInteractivo form = RecursoInteractivo.builder().titulo("Fail").build();

        when(recursoService.obtenerPorId(10L)).thenReturn(existing);
        doThrow(new IllegalArgumentException("id_exists")).when(recursoService).guardar(existing);

        mockMvc.perform(post("/admin/biblioteca-interactivos/editar/10")
                        .flashAttr("recurso", form)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos/editar/10?error=id_exists"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldToggleRecurso() throws Exception {
        RecursoInteractivo existing = RecursoInteractivo.builder().id(10L).activo(true).build();
        when(recursoService.obtenerPorId(10L)).thenReturn(existing);

        mockMvc.perform(post("/admin/biblioteca-interactivos/toggle/10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos?success=toggle"));

        verify(recursoService, times(1)).guardar(existing);
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteRecurso() throws Exception {
        mockMvc.perform(post("/admin/biblioteca-interactivos/eliminar/10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/biblioteca-interactivos?success=delete"));

        verify(recursoService, times(1)).eliminar(10L);
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldShowSelector() throws Exception {
        when(recursoService.obtenerBibliotecaFiltrada(eq(CategoriaMedia.RECURSO_CURSO), any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/biblioteca-interactivos/selector")
                        .param("search", "chess"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-selector-interactivos"))
                .andExpect(model().attributeExists("recursos", "search"));
    }
}
