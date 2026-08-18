package com.educore.platform.admin.controller;

import com.educore.platform.media.model.CategoriaMedia;
import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.service.RecursoInteractivoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador de administración seguro para la Biblioteca de Recursos Interactivos HTML5.
 */
@Controller
@RequestMapping("/admin/biblioteca-interactivos")
public class AdminBibliotecaController {

    private final RecursoInteractivoService recursoService;

    public AdminBibliotecaController(RecursoInteractivoService recursoService) {
        this.recursoService = recursoService;
    }

    /**
     * Muestra el catálogo de la biblioteca interactiva con soporte para búsqueda, categoría y etiquetas.
     */
    @GetMapping
    public String listarBiblioteca(
            @RequestParam(value = "categoria", required = false) CategoriaMedia categoria,
            @RequestParam(value = "etiqueta", required = false) String etiqueta,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<RecursoInteractivo> recursos = recursoService.obtenerBibliotecaFiltrada(categoria, etiqueta, search);
        List<String> todasEtiquetas = recursoService.obtenerTodasEtiquetas();

        model.addAttribute("recursos", recursos);
        model.addAttribute("etiquetas", todasEtiquetas);
        model.addAttribute("activeSection", "biblioteca");
        
        model.addAttribute("currentCategoria", categoria);
        model.addAttribute("currentEtiqueta", etiqueta);
        model.addAttribute("currentSearch", search);

        return "admin-biblioteca";
    }

    @GetMapping("/nuevo")
    public String nuevoRecursoForm(
            @RequestParam(value = "htmlUrl", required = false) String htmlUrl,
            @RequestParam(value = "tagsRaw", required = false) String tagsRaw,
            Model model) {
        
        RecursoInteractivo recurso = new RecursoInteractivo();
        if (htmlUrl != null && !htmlUrl.isBlank()) {
            recurso.setHtmlUrl(htmlUrl);
        }
        
        model.addAttribute("recurso", recurso);
        model.addAttribute("tagsRaw", tagsRaw != null ? tagsRaw : "");
        model.addAttribute("isEdit", false);
        model.addAttribute("activeSection", "biblioteca");
        return "admin-form-recurso";
    }

    /**
     * Procesa la creación de un nuevo recurso.
     */
    @PostMapping("/nuevo")
    public String guardarNuevoRecurso(
            @ModelAttribute("recurso") RecursoInteractivo recurso,
            @RequestParam(value = "tagsRaw", required = false) String tagsRaw,
            RedirectAttributes ra) {
        
        parseTags(recurso, tagsRaw);
        if (recurso.isEsGratis()) {
            recurso.setCosteCreditos(0);
        }

        try {
            recursoService.guardar(recurso);
            ra.addAttribute("success", "create");
        } catch (IllegalArgumentException e) {
            ra.addAttribute("error", "id_exists");
            return "redirect:/admin/biblioteca-interactivos/nuevo";
        }
        return "redirect:/admin/biblioteca-interactivos";
    }

    /**
     * Formulario de edición de un recurso.
     */
    @GetMapping("/editar/{id}")
    public String editarRecursoForm(@PathVariable("id") Long id, Model model) {
        RecursoInteractivo recurso = recursoService.obtenerPorId(id);
        String tagsRaw = String.join(", ", recurso.getEtiquetas());

        model.addAttribute("recurso", recurso);
        model.addAttribute("tagsRaw", tagsRaw);
        model.addAttribute("isEdit", true);
        model.addAttribute("activeSection", "biblioteca");
        return "admin-form-recurso";
    }

    /**
     * Procesa la edición de un recurso.
     */
    @PostMapping("/editar/{id}")
    public String actualizarRecurso(
            @PathVariable("id") Long id,
            @ModelAttribute("recurso") RecursoInteractivo form,
            @RequestParam(value = "tagsRaw", required = false) String tagsRaw,
            RedirectAttributes ra) {
        
        RecursoInteractivo dbRecurso = recursoService.obtenerPorId(id);
        dbRecurso.setIdentificador(form.getIdentificador());
        dbRecurso.setTitulo(form.getTitulo());
        dbRecurso.setDescripcion(form.getDescripcion());
        dbRecurso.setHtmlUrl(form.getHtmlUrl());
        dbRecurso.setImagenPortadaUrl(form.getImagenPortadaUrl());
        dbRecurso.setEsGratis(form.isEsGratis());
        dbRecurso.setActivo(form.isActivo());

        if (form.isEsGratis()) {
            dbRecurso.setCosteCreditos(0);
        } else {
            dbRecurso.setCosteCreditos(form.getCosteCreditos());
        }

        parseTags(dbRecurso, tagsRaw);

        try {
            recursoService.guardar(dbRecurso);
            ra.addAttribute("success", "update");
        } catch (IllegalArgumentException e) {
            ra.addAttribute("error", "id_exists");
            return "redirect:/admin/biblioteca-interactivos/editar/" + id;
        }
        return "redirect:/admin/biblioteca-interactivos";
    }

    /**
     * Activa/desactiva rápidamente un recurso de la biblioteca.
     */
    @PostMapping("/toggle/{id}")
    public String toggleEstadoRecurso(@PathVariable("id") Long id, RedirectAttributes ra) {
        RecursoInteractivo recurso = recursoService.obtenerPorId(id);
        recurso.setActivo(!recurso.isActivo());
        recursoService.guardar(recurso);
        ra.addAttribute("success", "toggle");
        return "redirect:/admin/biblioteca-interactivos";
    }

    /**
     * Elimina un recurso.
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarRecurso(@PathVariable("id") Long id, RedirectAttributes ra) {
        recursoService.eliminar(id);
        ra.addAttribute("success", "delete");
        return "redirect:/admin/biblioteca-interactivos";
    }

    /**
     * Modal selector integrado en la edición de lecciones/temarios del curso.
     * Muestra solo recursos autorizados para curso (Categoría RECURSO_CURSO).
     */
    @GetMapping("/selector")
    public String verSelectorInteractivos(
            @RequestParam(value = "search", required = false) String search,
            Model model) {
        
        List<RecursoInteractivo> recursos = recursoService.obtenerBibliotecaFiltrada(CategoriaMedia.RECURSO_CURSO, null, search);
        model.addAttribute("recursos", recursos);
        model.addAttribute("search", search);
        return "admin-selector-interactivos";
    }

    private void parseTags(RecursoInteractivo recurso, String tagsRaw) {
        Set<String> tags = new HashSet<>();
        if (tagsRaw != null && !tagsRaw.isBlank()) {
            for (String tag : tagsRaw.split(",")) {
                String cleanTag = tag.trim();
                if (!cleanTag.isEmpty()) {
                    tags.add(cleanTag);
                }
            }
        }
        recurso.setEtiquetas(tags);
    }
}
