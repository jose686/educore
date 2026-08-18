package com.educore.platform.store.controller;

import com.educore.platform.store.model.GuestToken;
import com.educore.platform.store.repository.GuestTokenRepository;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.model.Role;
import com.educore.platform.users.repository.UsuarioRepository;
import com.educore.platform.lms.repository.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para la lógica pública de canje de tokens de invitado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PromocionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromocionService promocionService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private GuestTokenRepository guestTokenRepository;

    @MockBean
    private InscripcionRepository inscripcionRepository;

    @Test
    void showCanjearTokenShouldRenderPage() throws Exception {
        mockMvc.perform(get("/canjear-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("canjear-token"));
    }

    @Test
    void canjearTokenWithValidTokenShouldAuthenticateGuestAndRedirect() throws Exception {
        GuestToken token = GuestToken.builder()
                .token("INV-TEST123")
                .activo(true)
                .cursoIds(Set.of(1L, 2L))
                .diasAcceso(7)
                .build();

        Usuario guestUser = Usuario.builder()
                .id(999L)
                .nombre("Invitado INV-TEST123")
                .email("guest_inv-test123@educore.com")
                .password("encoded_pass")
                .role(Role.GUEST)
                .activo(true)
                .build();

        when(guestTokenRepository.findByTokenAndActivoTrue("INV-TEST123")).thenReturn(Optional.of(token));
        when(usuarioRepository.findByEmail("guest_inv-test123@educore.com")).thenReturn(Optional.empty(), Optional.of(guestUser));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guestUser);

        MvcResult result = mockMvc.perform(post("/canjear-token")
                        .param("token", "INV-TEST123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-cursos?token_exito=1"))
                .andReturn();

        // Verificar que el usuario se haya autenticado en la sesión
        SecurityContext context = (SecurityContext) result.getRequest().getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        assertNotNull(context);
        assertNotNull(context.getAuthentication());
        assertEquals("guest_inv-test123@educore.com", context.getAuthentication().getName());
        assertTrue(context.getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST")));

        verify(promocionService).canjearGuestToken("INV-TEST123", "guest_inv-test123@educore.com");
    }

    @Test
    void canjearTokenWithInvalidTokenShouldRedirectWithError() throws Exception {
        when(guestTokenRepository.findByTokenAndActivoTrue("INV-INVALID")).thenReturn(Optional.empty());
        when(guestTokenRepository.findAll()).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(post("/canjear-token")
                        .param("token", "INV-INVALID")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/canjear-token?error=*"));
    }
}
