package com.educore.platform.users.controller;

import com.educore.platform.users.model.Role;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones administrativas sobre usuarios.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUsuarioRestController {

    private final UsuarioService usuarioService;

    public AdminUsuarioRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Obtiene todos los usuarios.
     */
    @GetMapping
    public ResponseEntity<List<Usuario>> listUsers() {
        return ResponseEntity.ok(usuarioService.obtenerTodosLosUsuarios());
    }

    /**
     * Obtiene el detalle de un usuario por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        return ResponseEntity.ok(usuario);
    }

    /**
     * Actualiza la información de un usuario.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody AdminUserUpdateDTO dto) {
        try {
            Usuario actualizado = usuarioService.actualizarUsuarioPorAdmin(
                    id,
                    dto.getNombre(),
                    dto.getEmail(),
                    dto.getRole(),
                    dto.isActivo(),
                    dto.getPassword()
            );
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un usuario por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok("Usuario eliminado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se pudo eliminar el usuario: " + e.getMessage());
        }
    }

    /**
     * DTO interno para capturar la petición REST de actualización de usuario.
     */
    @Data
    public static class AdminUserUpdateDTO {
        private String nombre;
        private String email;
        private Role role;
        private boolean activo;
        private String password;
    }
}
