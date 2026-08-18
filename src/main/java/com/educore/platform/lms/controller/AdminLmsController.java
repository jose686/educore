package com.educore.platform.lms.controller;

import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Modulo;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.dto.LeccionDTO;
import com.educore.platform.lms.dto.ModuloDTO;
import com.educore.platform.lms.service.LmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador administrativo de Backoffice para gestionar el temario del LMS (Módulos y Lecciones).
 * Protegido estrictamente con el rol ADMIN.
 */
@Controller
public class AdminLmsController {

    private final LmsService lmsService;

    public AdminLmsController(LmsService lmsService) {
        this.lmsService = lmsService;
    }

    /**
     * Muestra el temario completo de un curso específico (módulos y lecciones).
     */
    @GetMapping("/admin/lms/cursos/{cursoId}/temario")
    public String showSyllabus(@PathVariable("cursoId") String cursoIdStr, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            if (cursoIdStr == null || cursoIdStr.trim().isEmpty() || "null".equalsIgnoreCase(cursoIdStr)) {
                ra.addFlashAttribute("errorMessage", "El curso no tiene un ID de LMS asociado.");
                return "redirect:/admin/cursos/listado";
            }
            Long cursoId = Long.parseLong(cursoIdStr);
            Curso curso = lmsService.obtenerCursoPorId(cursoId);
            model.addAttribute("curso", curso);
            ModuloDTO moduloDto = new ModuloDTO();
            moduloDto.setOrden(0);
            model.addAttribute("moduloDto", moduloDto);
            return "admin-temario";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "No se pudo cargar el temario: " + e.getMessage());
            return "redirect:/admin/cursos/listado";
        }
    }

    /**
     * Procesa la adición de un nuevo módulo al curso.
     */
    @PostMapping("/admin/lms/cursos/{cursoId}/modulos/nuevo")
    public String saveModule(@PathVariable("cursoId") Long cursoId,
                             @ModelAttribute("moduloDto") ModuloDTO moduloDto) {
        lmsService.crearModulo(cursoId, moduloDto);
        return "redirect:/admin/lms/cursos/" + cursoId + "/temario";
    }

    /**
     * Muestra el formulario para redactar una lección en un módulo determinado.
     */
    @GetMapping("/admin/lms/modulos/{moduloId}/lecciones/nuevo")
    public String showNewLessonForm(@PathVariable("moduloId") Long moduloId, Model model) {
        Modulo modulo = lmsService.obtenerModuloPorId(moduloId);
        model.addAttribute("modulo", modulo);
        LeccionDTO leccionDto = new LeccionDTO();
        leccionDto.setOrden(0);
        model.addAttribute("leccionDto", leccionDto);
        return "admin-form-leccion";
    }

    /**
     * Procesa la creación de una nueva lección en el módulo.
     */
    @PostMapping("/admin/lms/modulos/{moduloId}/lecciones/nuevo")
    public String saveLesson(@PathVariable("moduloId") Long moduloId,
                             @ModelAttribute("leccionDto") LeccionDTO leccionDto) {
        Modulo modulo = lmsService.obtenerModuloPorId(moduloId);
        lmsService.crearLeccion(moduloId, leccionDto);
        
        // Redirigir de vuelta al temario del curso principal
        return "redirect:/admin/lms/cursos/" + modulo.getCurso().getId() + "/temario";
    }

    /**
     * Modifica un módulo existente (PUT).
     */
    @PutMapping("/admin/lms/modulos/{moduloId}")
    @ResponseBody
    public ResponseEntity<Void> updateModule(@PathVariable("moduloId") Long moduloId,
                                             @RequestBody ModuloDTO moduloDto) {
        lmsService.actualizarModulo(moduloId, moduloDto);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina un módulo existente (DELETE).
     */
    @DeleteMapping("/admin/lms/modulos/{moduloId}")
    @ResponseBody
    public ResponseEntity<Void> deleteModule(@PathVariable("moduloId") Long moduloId) {
        lmsService.eliminarModulo(moduloId);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtiene una lección específica en formato JSON para rellenar la modal de edición.
     */
    @GetMapping("/admin/lms/lecciones/{leccionId}")
    @ResponseBody
    public ResponseEntity<LeccionDTO> getLessonForEdit(@PathVariable("leccionId") Long leccionId) {
        Leccion leccion = lmsService.obtenerLeccionPorId(leccionId);
        LeccionDTO dto = LeccionDTO.builder()
                .titulo(leccion.getTitulo())
                .contenido(leccion.getContenido())
                .videoUrl(leccion.getVideoUrl())
                .esVideoLocal(leccion.getEsVideoLocal())
                .rutaScriptInteractivo(leccion.getRutaScriptInteractivo())
                .orden(leccion.getOrden())
                .build();

        return ResponseEntity.ok(dto);
    }

    /**
     * Modifica una lección existente (PUT).
     */
    @PutMapping("/admin/lms/lecciones/{leccionId}")
    @ResponseBody
    public ResponseEntity<Void> updateLesson(@PathVariable("leccionId") Long leccionId,
                                             @RequestBody LeccionDTO leccionDto) {
        lmsService.actualizarLeccion(leccionId, leccionDto);
        return ResponseEntity.ok().build();
    }

    /**
     * Elimina una lección existente (DELETE).
     */
    @DeleteMapping("/admin/lms/lecciones/{leccionId}")
    @ResponseBody
    public ResponseEntity<Void> deleteLesson(@PathVariable("leccionId") Long leccionId) {
        lmsService.eliminarLeccion(leccionId);
        return ResponseEntity.ok().build();
    }
}

