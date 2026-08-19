package com.educore.platform.admin.controller;

import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.service.BlogService;
import com.educore.platform.media.service.MediaService;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.users.model.Role;
import com.educore.platform.users.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración actualizadas para AdminController.
 * Valida la seguridad y el correcto funcionamiento del CRUD, la carga de medios y la gestión de usuarios.
 */
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BlogService blogService;

    @MockBean
    private CatalogoService catalogoService;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    void anonymousUser_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void studentUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessMediaLibrary() throws Exception {
        when(mediaService.listAllFiles()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/media"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-media"))
                .andExpect(model().attributeExists("files"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUploadMediaFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "img_data".getBytes());
        when(mediaService.uploadFile(any())).thenReturn("/media/mock-uuid.png");
        when(mediaService.uploadFile(any(), any())).thenReturn("/media/mock-uuid.png");
        when(mediaService.uploadFile(any(), any(), any())).thenReturn("/media/mock-uuid.png");

        mockMvc.perform(multipart("/admin/media/upload").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/media?uploaded_url=/media/mock-uuid.png"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessBlogListado() throws Exception {
        when(blogService.obtenerTodosLosArticulos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/blog/listado"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-blog-listado"))
                .andExpect(model().attributeExists("articulos"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteArticle() throws Exception {
        UUID articleId = UUID.randomUUID();
        doNothing().when(blogService).eliminarArticulo(articleId);

        mockMvc.perform(post("/admin/blog/eliminar/" + articleId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blog/listado?success=delete"));

        verify(blogService, times(1)).eliminarArticulo(articleId);
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessCourseListado() throws Exception {
        when(catalogoService.obtenerCatalogoPublico()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/cursos/listado"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-cursos-listado"))
                .andExpect(model().attributeExists("cursos"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteCourse() throws Exception {
        UUID courseId = UUID.randomUUID();
        doNothing().when(catalogoService).eliminarProducto(courseId);

        mockMvc.perform(post("/admin/cursos/eliminar/" + courseId).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/cursos/listado?success=delete"));

        verify(catalogoService, times(1)).eliminarProducto(courseId);
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessUsuariosListado() throws Exception {
        when(usuarioService.obtenerTodosLosUsuarios()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-usuarios"))
                .andExpect(model().attributeExists("usuarios"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldChangeUserRole() throws Exception {
        doNothing().when(usuarioService).actualizarRol(eq(123L), eq(Role.TEACHER));

        mockMvc.perform(post("/admin/usuarios/123/rol")
                .param("role", "TEACHER")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/usuarios?success=role"));

        verify(usuarioService, times(1)).actualizarRol(123L, Role.TEACHER);
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessNewArticleForm() throws Exception {
        mockMvc.perform(get("/admin/blog/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-articulo"))
                .andExpect(model().attributeExists("articulo"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessEditArticleForm() throws Exception {
        UUID articleId = UUID.randomUUID();
        Articulo mockArticle = Articulo.builder().id(articleId).titulo("Edit Title").build();
        when(blogService.obtenerPorId(articleId)).thenReturn(mockArticle);

        mockMvc.perform(get("/admin/blog/editar/" + articleId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-articulo"))
                .andExpect(model().attributeExists("articulo"))
                .andExpect(model().attribute("isEdit", true));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateArticle_AndTruncateResumenCortoIfTooLong() throws Exception {
        String longSummary = "A".repeat(320); // 320 characters
        Articulo mockArticle = Articulo.builder()
                .titulo("New Article")
                .resumenCorto(longSummary)
                .contenido("<p>Content</p>")
                .slug("new-article")
                .build();

        mockMvc.perform(post("/admin/blog/nuevo")
                        .flashAttr("articulo", mockArticle)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blog/listado?success=create"));

        ArgumentCaptor<Articulo> captor = ArgumentCaptor.forClass(Articulo.class);
        verify(blogService, times(1)).guardarArticulo(captor.capture());
        
        Articulo saved = captor.getValue();
        assertNotNull(saved.getResumenCorto());
        assertEquals(300, saved.getResumenCorto().length());
        assertEquals("A".repeat(300), saved.getResumenCorto());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUpdateArticle_AndTruncateResumenCortoIfTooLong() throws Exception {
        UUID articleId = UUID.randomUUID();
        String longSummary = "B".repeat(320); // 320 characters
        
        Articulo existing = Articulo.builder()
                .id(articleId)
                .titulo("Old Title")
                .resumenCorto("Old Summary")
                .build();
        
        Articulo formArticle = Articulo.builder()
                .titulo("Updated Title")
                .resumenCorto(longSummary)
                .contenido("<p>Updated Content</p>")
                .slug("updated-title")
                .build();

        when(blogService.obtenerPorId(articleId)).thenReturn(existing);

        mockMvc.perform(post("/admin/blog/editar/" + articleId)
                        .flashAttr("articulo", formArticle)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blog/listado?success=update"));

        ArgumentCaptor<Articulo> captor = ArgumentCaptor.forClass(Articulo.class);
        verify(blogService, times(1)).guardarArticulo(captor.capture());
        
        Articulo saved = captor.getValue();
        assertNotNull(saved.getResumenCorto());
        assertEquals(300, saved.getResumenCorto().length());
        assertEquals("B".repeat(300), saved.getResumenCorto());
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void studentUser_ShouldBeForbidden_WhenAccessingBlogAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/blog/nuevo"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/blog/nuevo").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
