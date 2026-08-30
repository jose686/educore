package com.educore.platform.store.controller;

import com.educore.platform.store.model.PaqueteCreditos;
import com.educore.platform.store.service.PaqueteCreditosService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador administrativo para realizar el CRUD de paquetes de créditos.
 */
@Controller
@RequestMapping("/admin/paquetes-creditos")
public class AdminPaqueteCreditosController {

    private final PaqueteCreditosService service;

    public AdminPaqueteCreditosController(PaqueteCreditosService service) {
        this.service = service;
    }

    /**
     * Lista todos los paquetes de créditos registrados.
     */
    @GetMapping
    public String listado(Model model) {
        model.addAttribute("paquetes", service.obtenerTodos());
        model.addAttribute("activeSection", "paquetes-creditos");
        return "admin-paquetes-creditos";
    }

    /**
     * Muestra el formulario para crear un nuevo paquete.
     */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("paquete", new PaqueteCreditos());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeSection", "paquetes-creditos");
        return "admin-paquetes-creditos-form";
    }

    /**
     * Muestra el formulario para editar un paquete existente.
     */
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model) {
        PaqueteCreditos pc = service.obtenerPorId(id);
        if (pc == null) {
            return "redirect:/admin/paquetes-creditos";
        }
        model.addAttribute("paquete", pc);
        model.addAttribute("isEdit", true);
        model.addAttribute("activeSection", "paquetes-creditos");
        return "admin-paquetes-creditos-form";
    }

    /**
     * Guarda o actualiza un paquete de créditos.
     */
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("paquete") PaqueteCreditos paquete, RedirectAttributes ra) {
        service.guardar(paquete);
        ra.addAttribute("success", "save");
        return "redirect:/admin/paquetes-creditos";
    }

    /**
     * Activa o desactiva de forma rápida un paquete.
     */
    @PostMapping("/toggle/{id}")
    public String toggleActivo(@PathVariable("id") Long id, RedirectAttributes ra) {
        PaqueteCreditos pc = service.obtenerPorId(id);
        if (pc != null) {
            pc.setActivo(!pc.isActivo());
            service.guardar(pc);
            ra.addAttribute("success", "save");
        }
        return "redirect:/admin/paquetes-creditos";
    }

    /**
     * Elimina físicamente un paquete.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes ra) {
        service.eliminar(id);
        ra.addAttribute("success", "delete");
        return "redirect:/admin/paquetes-creditos";
    }
}
