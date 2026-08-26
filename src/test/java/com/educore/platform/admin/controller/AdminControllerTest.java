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
import java.math.BigDecimal;
import com.educore.platform.store.model.Pedido;
import com.educore.platform.lms.service.LmsService;
import com.educore.platform.store.service.PedidoService;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.users.repository.TicketSoporteRepository;
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

    @MockBean
    private LmsService lmsService;

    @MockBean
    private PromocionService promocionService;

    @MockBean
    private PedidoService pedidoService;

    @MockBean
    private TicketSoporteRepository ticketSoporteRepository;

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

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUpdateMediaAlias() throws Exception {
        doNothing().when(mediaService).updateAlias("file.png", "new-alias");

        mockMvc.perform(post("/admin/media/alias/file.png")
                        .param("alias", "new-alias")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/media?success=alias_updated"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteMediaFile() throws Exception {
        doNothing().when(mediaService).deleteFile("file.png");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/admin/media/file.png")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldReturnInternalServerError_WhenDeleteMediaFileFails() throws Exception {
        doThrow(new RuntimeException("Error")).when(mediaService).deleteFile("file.png");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/admin/media/file.png")
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessNewCourseForm() throws Exception {
        mockMvc.perform(get("/admin/cursos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-curso"))
                .andExpect(model().attributeExists("producto"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateCourse() throws Exception {
        ProductoCurso course = ProductoCurso.builder().titulo("New Course").build();
        when(catalogoService.guardarProducto(any())).thenReturn(course);

        mockMvc.perform(post("/admin/cursos/nuevo")
                        .flashAttr("producto", course)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/cursos/listado?success=create"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessEditCourseForm() throws Exception {
        UUID courseId = UUID.randomUUID();
        ProductoCurso course = ProductoCurso.builder().id(courseId).titulo("Course").build();
        when(catalogoService.obtenerPorId(courseId)).thenReturn(course);

        mockMvc.perform(get("/admin/cursos/editar/" + courseId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-form-curso"))
                .andExpect(model().attributeExists("producto"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUpdateCourse() throws Exception {
        UUID courseId = UUID.randomUUID();
        ProductoCurso existing = ProductoCurso.builder().id(courseId).titulo("Old").build();
        ProductoCurso form = ProductoCurso.builder().titulo("New").build();

        when(catalogoService.obtenerPorId(courseId)).thenReturn(existing);
        when(catalogoService.guardarProducto(any())).thenReturn(existing);

        mockMvc.perform(post("/admin/cursos/editar/" + courseId)
                        .flashAttr("producto", form)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/cursos/listado?success=update"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessPromociones() throws Exception {
        when(promocionService.obtenerTodosLosCupones()).thenReturn(Collections.emptyList());
        when(promocionService.obtenerTodosLosTokens()).thenReturn(Collections.emptyList());
        when(promocionService.obtenerTodosLosPaquetes()).thenReturn(Collections.emptyList());
        when(promocionService.obtenerPromocionesCurso()).thenReturn(Collections.emptyList());
        when(catalogoService.obtenerTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/promociones"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-promociones"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreatePromocion() throws Exception {
        mockMvc.perform(post("/admin/promociones/nuevo")
                        .param("codigo", "SAVE10")
                        .param("tipo", "DESCUENTO")
                        .param("descuentoPorcentaje", "10")
                        .param("diasAcceso", "0")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=cupones&success=create"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeletePromocion() throws Exception {
        mockMvc.perform(post("/admin/promociones/eliminar/10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=cupones&success=delete"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateGuestToken() throws Exception {
        mockMvc.perform(post("/admin/tokens/nuevo")
                        .param("cursoIds", "1", "2")
                        .param("diasAcceso", "30")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=tokens&success=create"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteGuestToken() throws Exception {
        mockMvc.perform(post("/admin/tokens/eliminar/10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=tokens&success=delete"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreatePaquete() throws Exception {
        mockMvc.perform(post("/admin/paquetes/nuevo")
                        .param("titulo", "Bundle")
                        .param("descripcion", "Desc")
                        .param("precio", "9.99")
                        .param("cursoIds", "1", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=paquetes&success=create"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeletePaquete() throws Exception {
        mockMvc.perform(post("/admin/paquetes/eliminar/10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=paquetes&success=delete"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreatePromocionCurso() throws Exception {
        mockMvc.perform(post("/admin/promociones-curso/nuevo")
                        .param("cursoId", "100")
                        .param("tipo", "AUTOMATICA")
                        .param("porcentajeDescuento", "20")
                        .param("fechaInicio", "2026-08-19")
                        .param("fechaFin", "2026-08-25")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=descuentos&success=create"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeletePromocionCurso() throws Exception {
        mockMvc.perform(post("/admin/promociones-curso/eliminar/10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/promociones?tab=descuentos&success=delete"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessPedidos() throws Exception {
        when(pedidoService.obtenerTodosLosPedidosConDetalles()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/pedidos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-pedidos"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessPedidoDetail() throws Exception {
        Pedido pedido = Pedido.builder().id(1L).emailUsuario("user@educore.com").totalEuros(BigDecimal.TEN).build();
        when(pedidoService.obtenerPedidoPorId(1L)).thenReturn(pedido);

        mockMvc.perform(get("/admin/pedidos/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-pedido-detalle"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldForceMatriculacionManual() throws Exception {
        mockMvc.perform(post("/admin/pedidos/1/matricular-manual")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pedidos/1"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldReembolsarPedido() throws Exception {
        mockMvc.perform(post("/admin/pedidos/1/reembolsar")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pedidos/1"));
    }
}
