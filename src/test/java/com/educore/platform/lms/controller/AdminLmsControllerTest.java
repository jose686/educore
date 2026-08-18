package com.educore.platform.lms.controller;

import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.model.Modulo;
import com.educore.platform.lms.dto.LeccionDTO;
import com.educore.platform.lms.dto.ModuloDTO;
import com.educore.platform.lms.service.LmsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Pruebas de integración para AdminLmsController.
 * Valida la seguridad y los flujos de creación del temario del aula virtual (LMS).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminLmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LmsService lmsService;

    @Test
    void anonymousUser_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/lms/cursos/1/temario"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "estudiante@educore.com", roles = "STUDENT")
    void studentUser_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/admin/lms/cursos/1/temario"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldAccessTemario() throws Exception {
        Curso mockCurso = Curso.builder().id(1L).titulo("Curso Ajedrez").modulos(new java.util.ArrayList<>()).build();
        when(lmsService.obtenerCursoPorId(1L)).thenReturn(mockCurso);

        mockMvc.perform(get("/admin/lms/cursos/1/temario"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-temario"))
                .andExpect(model().attributeExists("curso"))
                .andExpect(model().attributeExists("moduloDto"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateModuleAndRedirect() throws Exception {
        Modulo mockModulo = Modulo.builder().id(1L).nombre("Mod 1").build();
        when(lmsService.crearModulo(eq(1L), any(ModuloDTO.class))).thenReturn(mockModulo);

        mockMvc.perform(post("/admin/lms/cursos/1/modulos/nuevo")
                .param("nombre", "Módulo Uno")
                .param("orden", "1")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/lms/cursos/1/temario"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldCreateLessonAndRedirect() throws Exception {
        Curso mockCurso = Curso.builder().id(10L).titulo("Curso").build();
        Modulo mockModulo = Modulo.builder().id(1L).nombre("Modulo").curso(mockCurso).build();
        Leccion mockLeccion = Leccion.builder().id(1L).titulo("Leccion").build();
        
        when(lmsService.obtenerModuloPorId(1L)).thenReturn(mockModulo);
        when(lmsService.crearLeccion(eq(1L), any(LeccionDTO.class))).thenReturn(mockLeccion);

        mockMvc.perform(post("/admin/lms/modulos/1/lecciones/nuevo")
                .param("titulo", "Lección Táctica")
                .param("contenido", "<p>Texto de la clase.</p>")
                .param("orden", "1")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/lms/cursos/10/temario"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUpdateModule() throws Exception {
        mockMvc.perform(put("/admin/lms/modulos/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Modulo Modificado\"}")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteModule() throws Exception {
        mockMvc.perform(delete("/admin/lms/modulos/1")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldGetLessonForEdit() throws Exception {
        Leccion mockLeccion = Leccion.builder()
                .id(1L)
                .titulo("Lec 1")
                .contenido("Texto")
                .videoUrl("/video")
                .rutaScriptInteractivo("/script")
                .orden(1)
                .build();
        when(lmsService.obtenerLeccionPorId(1L)).thenReturn(mockLeccion);

        mockMvc.perform(get("/admin/lms/lecciones/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Lec 1"))
                .andExpect(jsonPath("$.contenido").value("Texto"))
                .andExpect(jsonPath("$.videoUrl").value("/video"))
                .andExpect(jsonPath("$.rutaScriptInteractivo").value("/script"));
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldUpdateLesson() throws Exception {
        mockMvc.perform(put("/admin/lms/lecciones/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"Lec Modificada\",\"contenido\":\"Nuevo contenido\",\"videoUrl\":\"/video2\",\"rutaScriptInteractivo\":\"/script2\"}")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@educore.com", roles = "ADMIN")
    void adminUser_ShouldDeleteLesson() throws Exception {
        mockMvc.perform(delete("/admin/lms/lecciones/1")
                .with(csrf()))
                .andExpect(status().isOk());
    }
}

