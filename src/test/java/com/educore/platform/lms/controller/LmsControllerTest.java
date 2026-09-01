package com.educore.platform.lms.controller;

import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.model.Modulo;
import com.educore.platform.lms.service.LmsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para LmsController.
 * Verifica la protección de rutas del aula virtual según el rol y la comprobación de matrícula.
 */
import org.springframework.transaction.annotation.Transactional;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.store.model.ProductoCurso;
import java.util.List;
import java.util.Optional;

import com.educore.platform.media.service.MediaService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LmsService lmsService;

    @MockBean
    private CatalogoService catalogoService;

    @MockBean
    private MediaService mediaService;

    @Test
    void anonymousUser_ShouldRedirectToLogin_WhenAccessingPrivateArea() throws Exception {
        mockMvc.perform(get("/mis-cursos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/aula/1/leccion/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "visita@educore.com", roles = "VISITOR")
    void visitorUser_ShouldBeForbidden_WhenAccessingPrivateArea() throws Exception {
        // Los visitantes (que no han comprado nada) no deben tener acceso a las rutas privadas
        mockMvc.perform(get("/mis-cursos"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/aula/1/leccion/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "no_matriculado@educore.com", roles = "STUDENT")
    void studentWithoutEnrollment_ShouldGetForbidden_WhenAccessingLesson() throws Exception {
        // Simulamos que el servicio lanza AccessDeniedException al no tener matrícula
        when(lmsService.obtenerLeccionAsegurada(1L, 1L, "no_matriculado@educore.com"))
                .thenThrow(new AccessDeniedException("No estás matriculado"));

        mockMvc.perform(get("/aula/1/leccion/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void studentWithEnrollment_ShouldGetOk_WhenAccessingLesson() throws Exception {
        Curso mockCurso = Curso.builder().id(1L).titulo("Curso Ajedrez").modulos(new java.util.ArrayList<>()).build();
        Modulo mockModulo = Modulo.builder().id(1L).nombre("Módulo 1").curso(mockCurso).lecciones(new java.util.ArrayList<>()).build();
        mockCurso.getModulos().add(mockModulo);
        Leccion mockLeccion = Leccion.builder().id(1L).titulo("Aperturas").modulo(mockModulo).contenido("Contenido").build();
        mockModulo.getLecciones().add(mockLeccion);
        
        when(lmsService.obtenerLeccionAsegurada(1L, 1L, "alumno@educore.com")).thenReturn(mockLeccion);
        when(lmsService.obtenerCursoPorId(1L)).thenReturn(mockCurso);

        mockMvc.perform(get("/aula/1/leccion/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("reproductor"))
                .andExpect(model().attributeExists("leccion"))
                .andExpect(model().attributeExists("curso"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void studentWithEnrollment_ShouldGetMisCursosWithEnrichedData() throws Exception {
        Curso mockCurso = Curso.builder().id(10L).titulo("Curso Base LMS 10").descripcion("Desc Base").build();
        ProductoCurso mockProducto = ProductoCurso.builder()
                .lmsCursoId(10L)
                .titulo("Aprende Ajedrez desde Cero")
                .descripcionCorta("Curso comercial completo")
                .imagenPortadaUrl("/images/chess-cover.jpg")
                .build();

        when(lmsService.obtenerCursosEstudiante("alumno@educore.com")).thenReturn(List.of(mockCurso));
        when(catalogoService.obtenerPorLmsCursoId(10L)).thenReturn(Optional.of(mockProducto));

        mockMvc.perform(get("/mis-cursos"))
                .andExpect(status().isOk())
                .andExpect(view().name("mis-cursos"))
                .andExpect(model().attributeExists("cursos"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void nonNumericLessonSlug_ShouldRedirectToMediaUrl() throws Exception {
        when(mediaService.resolveUrlByAliasOrPath("tablero-ajedrez-interactivo"))
                .thenReturn("/media/tablero-ajedrez-interactivo.html");

        mockMvc.perform(get("/aula/1/leccion/tablero-ajedrez-interactivo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/media/tablero-ajedrez-interactivo.html"));
    }
}
