package com.educore.platform.store.controller;

import com.educore.platform.store.service.PaqueteCreditosService;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para la visualización pública de la recarga de créditos.
 */
@Controller
public class CreditoController {

    private final PaqueteCreditosService paqueteCreditosService;
    private final UsuarioService usuarioService;

    public CreditoController(PaqueteCreditosService paqueteCreditosService, UsuarioService usuarioService) {
        this.paqueteCreditosService = paqueteCreditosService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra la pantalla de recarga de créditos con las ofertas de paquetes activos.
     */
    @GetMapping("/creditos/recargar")
    public String mostrarRecarga(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("saldoCreditos", usuario.getSaldoCreditos());
        model.addAttribute("paquetes", paqueteCreditosService.obtenerPaquetesActivos());
        return "creditos-recargar";
    }
}
