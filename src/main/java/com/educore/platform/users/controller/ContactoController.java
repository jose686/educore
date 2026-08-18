package com.educore.platform.users.controller;

import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import com.educore.platform.users.model.TicketSoporte;
import com.educore.platform.users.repository.TicketSoporteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller to handle the contact/support requests.
 */
@Controller
@Slf4j
public class ContactoController {

    private final UsuarioService usuarioService;
    private final TicketSoporteRepository ticketSoporteRepository;

    public ContactoController(UsuarioService usuarioService, TicketSoporteRepository ticketSoporteRepository) {
        this.usuarioService = usuarioService;
        this.ticketSoporteRepository = ticketSoporteRepository;
    }

    @GetMapping("/contacto")
    public String showContacto(@RequestParam(value = "pedidoId", required = false) Long pedidoId, Model model) {
        model.addAttribute("pedidoId", pedidoId);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String email = auth.getName();
            model.addAttribute("emailUsuario", email);
            
            try {
                Usuario usuario = usuarioService.obtenerPorEmail(email);
                if (usuario != null) {
                    model.addAttribute("nombreUsuario", usuario.getNombre());
                }
            } catch (Exception e) {
                log.warn("[SOPORTE] No se pudo obtener el nombre completo del usuario '{}': {}", email, e.getMessage());
            }
        }
        
        return "contacto";
    }

    @PostMapping("/contacto")
    public String submitContacto(
            @RequestParam("nombre") String nombre,
            @RequestParam("email") String email,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "pedidoId", required = false) Long pedidoId,
            @RequestParam("mensaje") String mensaje,
            RedirectAttributes redirectAttrs) {

        log.info("[SOPORTE] Nuevo ticket recibido - Nombre: {}, Email: {}, Motivo: {}, PedidoId: {}, Mensaje: {}",
                nombre, email, motivo, pedidoId != null ? pedidoId : "N/A", mensaje);

        TicketSoporte ticket = TicketSoporte.builder()
                .nombre(nombre)
                .email(email)
                .motivo(motivo)
                .pedidoId(pedidoId)
                .mensaje(mensaje)
                .estado("PENDIENTE")
                .build();
        
        ticketSoporteRepository.save(ticket);
        log.info("[SOPORTE] Ticket guardado en BD con ID: {}", ticket.getId());

        redirectAttrs.addFlashAttribute("successMsg", "¡Tu solicitud de soporte ha sido enviada con éxito! Nos pondremos en contacto contigo lo antes posible.");
        
        return "redirect:/contacto";
    }
}
