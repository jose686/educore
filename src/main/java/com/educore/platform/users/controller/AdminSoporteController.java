package com.educore.platform.users.controller;

import com.educore.platform.users.model.TicketSoporte;
import com.educore.platform.users.repository.TicketSoporteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminSoporteController {

    private final TicketSoporteRepository ticketSoporteRepository;

    public AdminSoporteController(TicketSoporteRepository ticketSoporteRepository) {
        this.ticketSoporteRepository = ticketSoporteRepository;
    }

    @GetMapping("/admin/soporte")
    public String verSoporte(
            @RequestParam(value = "estado", required = false) String estado,
            Model model) {
        
        List<TicketSoporte> tickets;
        if (estado != null && !estado.trim().isEmpty() && !"TODOS".equalsIgnoreCase(estado)) {
            tickets = ticketSoporteRepository.findByEstadoOrderByFechaCreacionDesc(estado.toUpperCase().trim());
        } else {
            tickets = ticketSoporteRepository.findAllByOrderByFechaCreacionDesc();
        }

        model.addAttribute("tickets", tickets);
        model.addAttribute("estadoFilter", estado != null ? estado.toUpperCase().trim() : "TODOS");
        model.addAttribute("activeSection", "soporte");
        return "admin-soporte";
    }

    @PostMapping("/admin/soporte/{id}/resolver")
    public String resolverTicket(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        TicketSoporte ticket = ticketSoporteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado con ID: " + id));
        ticket.setEstado("RESUELTO");
        ticketSoporteRepository.save(ticket);
        
        redirectAttrs.addFlashAttribute("successMsg", "El ticket #" + id + " ha sido marcado como RESUELTO.");
        return "redirect:/admin/soporte";
    }
}
