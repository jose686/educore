package com.educore.platform.store.controller;


import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.service.RecursoInteractivoService;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Controlador público de minijuegos adaptado para usar la biblioteca de recursos interactivos.
 */
@Controller
public class MinijuegoController {

    private final RecursoInteractivoService recursoService;
    private final UsuarioService usuarioService;

    public MinijuegoController(RecursoInteractivoService recursoService, UsuarioService usuarioService) {
        this.recursoService = recursoService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra el catálogo de minijuegos públicos activos (Marcados como SOLO_MINIJUEGO o AMBOS).
     */
    @GetMapping("/minijuegos")
    public String listarMinijuegosPublico(Model model, Authentication authentication) {
        // Filtrar recursos interactivos marcados para Minijuegos (SOLO_MINIJUEGO o AMBOS)
        List<RecursoInteractivo> juegos = recursoService.obtenerActivosPorCategoriaMedia(com.educore.platform.media.model.CategoriaMedia.MINIJUEGO);
        model.addAttribute("minijuegos", juegos);

        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
            model.addAttribute("usuario", usuario);
            model.addAttribute("saldoCreditos", usuario.getSaldoCreditos());
            model.addAttribute("minijuegosDesbloqueados", usuario.getMinijuegosDesbloqueados());
        } else {
            model.addAttribute("usuario", null);
            model.addAttribute("saldoCreditos", 0);
            model.addAttribute("minijuegosDesbloqueados", Collections.emptySet());
        }

        return "juegos";
    }

    /**
     * Redirección amigable.
     */
    @GetMapping("/juegos")
    public String redirigirAJuegos() {
        return "redirect:/minijuegos";
    }

    /**
     * Endpoint para jugar/cargar un minijuego de forma segura.
     */
    @GetMapping("/minijuegos/jugar/{id}")
    public String jugarMinijuego(@PathVariable("id") Long id, Authentication authentication, RedirectAttributes ra) {
        RecursoInteractivo juego = recursoService.obtenerPorId(id);
        
        boolean isAnonymous = authentication == null || !authentication.isAuthenticated() 
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
        
        if (isAnonymous) {
            if (juego.isEsGratis()) {
                return "redirect:" + juego.getResolvedHtmlUrl();
            } else {
                return "redirect:/login?redirect=/minijuegos";
            }
        }
        
        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        if (juego.isEsGratis() || usuario.getMinijuegosDesbloqueados().contains(juego)) {
            return "redirect:" + juego.getResolvedHtmlUrl();
        } else {
            ra.addAttribute("error", "not_unlocked");
            return "redirect:/minijuegos";
        }
    }

    /**
     * Desbloquea un minijuego de pago deduciendo el coste en créditos de la cuenta del estudiante.
     */
    @PostMapping("/minijuegos/desbloquear/{id}")
    public String desbloquearMinijuego(@PathVariable("id") Long id, Authentication authentication, RedirectAttributes ra) {
        boolean isAnonymous = authentication == null || !authentication.isAuthenticated() 
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;

        if (isAnonymous) {
            return "redirect:/login?redirect=/minijuegos";
        }

        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        RecursoInteractivo recurso = recursoService.obtenerPorId(id);

        if (usuario.getMinijuegosDesbloqueados().contains(recurso) || recurso.isEsGratis()) {
            ra.addAttribute("info", "already_unlocked");
            return "redirect:/minijuegos";
        }

        if (usuario.getSaldoCreditos() < recurso.getCosteCreditos()) {
            ra.addAttribute("error", "insufficient_credits");
            return "redirect:/minijuegos";
        }

        // Deducir saldo y añadir el recurso a desbloqueados
        usuario.setSaldoCreditos(usuario.getSaldoCreditos() - recurso.getCosteCreditos());
        usuario.getMinijuegosDesbloqueados().add(recurso);
        usuarioService.guardar(usuario);

        ra.addAttribute("success", "unlocked");
        return "redirect:/minijuegos";
    }

    /**
     * Recarga rápida de saldo.
     */
    @PostMapping("/minijuegos/recargar")
    public String recargarSaldo(Authentication authentication, RedirectAttributes ra) {
        boolean isAnonymous = authentication == null || !authentication.isAuthenticated() 
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;

        if (isAnonymous) {
            return "redirect:/login?redirect=/minijuegos";
        }

        Usuario usuario = usuarioService.obtenerPorEmail(authentication.getName());
        usuario.setSaldoCreditos(usuario.getSaldoCreditos() + 100);
        usuarioService.guardar(usuario);

        ra.addAttribute("success", "reloaded");
        return "redirect:/minijuegos";
    }
}
