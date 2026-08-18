package com.educore.platform.media.controller;

import com.educore.platform.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para MediaController.
 * Valida la seguridad pública de lectura de recursos y la restricción de subidas POST.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @Test
    void anonymousUser_ShouldBeAllowed_ToServeFile() throws Exception {
        // Mock de un recurso binario temporal
        Resource mockResource = new ByteArrayResource("contenido_simulado".getBytes()) {
            @Override
            public String getFilename() {
                return "imagen.png";
            }
        };

        when(mediaService.loadFileAsResource("imagen.png")).thenReturn(mockResource);

        mockMvc.perform(get("/media/imagen.png"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUser_ShouldRedirectToLogin_WhenUploading() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "fake_data".getBytes());

        mockMvc.perform(multipart("/api/media/upload").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alumno@educore.com", roles = "STUDENT")
    void studentUser_ShouldBeForbidden_WhenUploading() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "fake_data".getBytes());

        mockMvc.perform(multipart("/api/media/upload").file(file).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "profesor@educore.com", roles = "TEACHER")
    void teacherUser_ShouldBeAllowed_ToUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "fake_data".getBytes());
        
        when(mediaService.uploadFile(any())).thenReturn("/media/uuid-generado.png");

        mockMvc.perform(multipart("/api/media/upload").file(file).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/media/uuid-generado.png"));
    }
}
