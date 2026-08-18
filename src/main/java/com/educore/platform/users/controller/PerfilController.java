package com.educore.platform.users.controller;

import com.educore.platform.store.service.PromocionService;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.educore.platform.store.model.Pedido;
import com.educore.platform.store.repository.PedidoRepository;
import com.educore.platform.store.repository.DatosFiscalesRepository;
import com.educore.platform.store.model.DatosFiscales;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

/**
 * Controlador para manejar la vista del perfil del usuario y el canje de tokens desde ahí.
 */
@Controller
public class PerfilController {

    private final UsuarioService usuarioService;
    private final PromocionService promocionService;
    private final PedidoRepository pedidoRepository;
    private final DatosFiscalesRepository datosFiscalesRepository;

    public PerfilController(UsuarioService usuarioService,
                            PromocionService promocionService,
                            PedidoRepository pedidoRepository,
                            DatosFiscalesRepository datosFiscalesRepository) {
        this.usuarioService = usuarioService;
        this.promocionService = promocionService;
        this.pedidoRepository = pedidoRepository;
        this.datosFiscalesRepository = datosFiscalesRepository;
    }

    /**
     * Muestra la sección de ajustes de perfil del usuario.
     */
    @GetMapping("/perfil")
    public String showProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String email = auth.getName();
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        model.addAttribute("usuario", usuario);

        return "perfil";
    }

    /**
     * Muestra el historial de compras del usuario autenticado.
     */
    @GetMapping("/perfil/compras")
    public String miHistorialCompras(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        String email = auth.getName();
        List<Pedido> pedidos = pedidoRepository.findByUsuarioEmail(email);
        model.addAttribute("pedidos", pedidos);
        return "perfil-compras";
    }

    /**
     * Endpoint para generar o descargar la factura de un pedido.
     */
    @GetMapping("/perfil/compras/{id}/factura")
    public String descargarFactura(@PathVariable("id") Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + id));
        String email = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!pedido.getEmailUsuario().equals(email) && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permiso para ver esta factura.");
        }

        // Fetch global fiscal settings, fallback if database is empty
        DatosFiscales empresa = datosFiscalesRepository.findAll().stream().findFirst().orElseGet(() ->
                DatosFiscales.builder()
                        .razonSocial("EduCore Platform S.L.")
                        .cifNif("B-88765432")
                        .direccionFiscal("Calle de la Táctica, 42, 28001 Madrid, España")
                        .emailContacto("soporte@educore.com")
                        .telefono("+34 912 345 678")
                        .build()
        );
        
        model.addAttribute("pedido", pedido);
        model.addAttribute("empresa", empresa);
        return "factura";
    }

    /**
     * Procesa el canje de un token de invitado desde la vista de perfil.
     */
    @PostMapping("/perfil/canjear-token")
    public String canjearToken(@RequestParam("token") String token) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String email = auth.getName();
        try {
            promocionService.canjearGuestToken(token, email);
            return "redirect:/perfil?token_exito=" + enc("¡Token canjeado con éxito! Se han activado tus suscripciones correspondientes.");
        } catch (IllegalArgumentException e) {
            return "redirect:/perfil?token_error=" + enc(e.getMessage());
        } catch (Exception e) {
            return "redirect:/perfil?token_error=" + enc("Error al canjear el token.");
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
