package com.educore.platform.users.controller;

import com.educore.platform.users.dto.UsuarioRegistroDTO;
import com.educore.platform.users.exception.EmailAlreadyExistsException;
import com.educore.platform.users.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Controlador para manejar las vistas públicas de autenticación (Login y Registro).
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra la vista del formulario de login.
     *
     * @return El nombre lógico de la plantilla Thymeleaf (login.html).
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    /**
     * Muestra la vista del formulario de registro y agrega un objeto vacío al modelo
     * para el binding con el formulario HTML de Thymeleaf.
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("usuario", new UsuarioRegistroDTO());
        return "register";
    }

    /**
     * Procesa la solicitud POST del formulario de registro de usuario.
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("usuario") UsuarioRegistroDTO dto,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            usuarioService.registrar(dto);
        } catch (EmailAlreadyExistsException ex) {
            // Rechazamos el campo email indicando el mensaje de la excepción de negocio
            bindingResult.rejectValue("email", "error.usuario", ex.getMessage());
            return "register";
        }

        // Redirige al login indicando mediante un parámetro el éxito del registro
        return "redirect:/login?success";
    }
}
