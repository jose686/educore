package com.educore.platform.users.controller;

import com.educore.platform.users.dto.ProfileUpdateDTO;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Controlador REST para gestionar operaciones del perfil de usuario a través de llamadas de API.
 */
@RestController
@RequestMapping("/api/v1/users")
public class PerfilRestController {

    private final UsuarioService usuarioService;

    public PerfilRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Actualiza el perfil del usuario autenticado actualmente.
     *
     * @param dto Datos actualizados del perfil.
     * @param principal Principal de seguridad del usuario autenticado.
     * @return El usuario actualizado o un mensaje de error.
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateDTO dto, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("No autorizado");
        }

        Usuario usuarioActual = usuarioService.obtenerPorEmail(principal.getName());
        if (usuarioActual == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }

        try {
            Usuario actualizado = usuarioService.actualizarPerfil(
                    usuarioActual.getId(),
                    dto.getNombre(),
                    dto.getEmail(),
                    dto.getPassword()
            );

            // Actualizar la sesión en Spring Security si cambia el correo electrónico
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && !auth.getName().equalsIgnoreCase(dto.getEmail())) {
                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                        actualizado.getEmail(),
                        auth.getCredentials(),
                        auth.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }

            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
