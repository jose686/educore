package com.educore.platform.users.controller;

import com.educore.platform.users.model.Role;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controlador MVC para que los administradores gestionen los usuarios de la plataforma.
 */
@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra la lista de todos los usuarios registrados.
     */
    @GetMapping
    public String listadoUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.obtenerTodosLosUsuarios());
        model.addAttribute("roles", Role.values());
        return "admin-usuarios";
    }

    /**
     * Modifica el rol de un usuario de forma directa (compatibilidad con tests).
     */
    @PostMapping("/{id}/rol")
    public String changeUserRole(@PathVariable("id") Long id, @RequestParam("role") Role role) {
        usuarioService.actualizarRol(id, role);
        return "redirect:/admin/usuarios?success=role";
    }

    /**
     * Muestra el formulario para editar un usuario existente.
     */
    @GetMapping("/editar/{id}")
    public String editarUsuarioForm(@PathVariable("id") Long id, Model model) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario == null) {
            return "redirect:/admin/usuarios?error=" + enc("Usuario no encontrado.");
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", Role.values());
        return "admin-usuarios-editar";
    }

    /**
     * Procesa la actualización de los datos del usuario.
     */
    @PostMapping("/actualizar/{id}")
    public String actualizarUsuario(@PathVariable("id") Long id,
                                    @RequestParam("nombre") String nombre,
                                    @RequestParam("email") String email,
                                    @RequestParam("role") Role role,
                                    @RequestParam(value = "activo", required = false, defaultValue = "false") boolean activo,
                                    @RequestParam(value = "password", required = false) String password) {
        try {
            usuarioService.actualizarUsuarioPorAdmin(id, nombre, email, role, activo, password);
            return "redirect:/admin/usuarios?success=" + enc("Usuario actualizado correctamente.");
        } catch (Exception e) {
            return "redirect:/admin/usuarios/editar/" + id + "?error=" + enc(e.getMessage());
        }
    }

    /**
     * Elimina o da de baja a un usuario.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable("id") Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return "redirect:/admin/usuarios?success=" + enc("Usuario eliminado correctamente.");
        } catch (Exception e) {
            return "redirect:/admin/usuarios?error=" + enc("No se pudo eliminar el usuario: " + e.getMessage());
        }
    }

    private String enc(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
    }
}
