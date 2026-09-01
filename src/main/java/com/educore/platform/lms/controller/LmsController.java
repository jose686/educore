package com.educore.platform.lms.controller;

import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.service.LmsService;
import com.educore.platform.users.service.UserPublicService;
import com.educore.platform.store.service.RecursoInteractivoService;
import com.educore.platform.store.model.RecursoInteractivo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

import com.educore.platform.store.service.CatalogoService;

import com.educore.platform.media.service.MediaService;

/**
 * Controlador de vistas privadas para el Aula Virtual (LMS).
 */
@Controller
public class LmsController {

    private final LmsService lmsService;
    private final UserPublicService userPublicService;
    private final RecursoInteractivoService recursoService;
    private final CatalogoService catalogoService;
    private final MediaService mediaService;

    public LmsController(LmsService lmsService,
                         UserPublicService userPublicService,
                         RecursoInteractivoService recursoService,
                         CatalogoService catalogoService,
                         MediaService mediaService) {
        this.lmsService = lmsService;
        this.userPublicService = userPublicService;
        this.recursoService = recursoService;
        this.catalogoService = catalogoService;
        this.mediaService = mediaService;
    }

    /**
     * Panel privado del estudiante donde visualiza sus cursos matriculados.
     */
    @GetMapping("/mis-cursos")
    public String verMisCursos(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String email = auth.getName();
            refreshSecurityContextIfRoleChanged(auth, email);
            
            // Re-fetch authentication after refresh
            auth = SecurityContextHolder.getContext().getAuthentication();
            List<Curso> cursosMatriculados = lmsService.obtenerCursosEstudiante(email);

            // Enriquecer datos del curso con la oferta comercial de ProductoCurso si está disponible
            for (Curso curso : cursosMatriculados) {
                try {
                    catalogoService.obtenerPorLmsCursoId(curso.getId()).ifPresent(prod -> {
                        if (prod.getTitulo() != null && !prod.getTitulo().isBlank()) {
                            curso.setTitulo(prod.getTitulo());
                        }
                        if (prod.getDescripcionCorta() != null && !prod.getDescripcionCorta().isBlank()) {
                            curso.setDescripcion(prod.getDescripcionCorta());
                        }
                        if (prod.getImagenPortadaUrl() != null && !prod.getImagenPortadaUrl().isBlank()) {
                            curso.setImagenUrl(prod.getImagenPortadaUrl());
                        }
                    });
                } catch (Exception e) {
                    // Ignorar posibles errores de enriquecimiento
                }
            }

            model.addAttribute("cursos", cursosMatriculados);
        }
        
        return "mis-cursos";
    }

    private void refreshSecurityContextIfRoleChanged(Authentication auth, String email) {
        userPublicService.getUserRoleByEmail(email).ifPresent(role -> {
            String currentRole = auth.getAuthorities().iterator().next().getAuthority();
            String dbRole = "ROLE_" + role;
            if (!currentRole.equals(dbRole)) {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(dbRole);
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(),
                        auth.getCredentials(),
                        List.of(authority)
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
        });
    }

    /**
     * Mapeo defensivo: redirige solicitudes accidentales con rutas relativas a recursos interactivos
     * (ej. GET /aula/1/leccion/tablero-ajedrez-interactivo) hacia la URL del recurso de medios correspondiente.
     */
    @GetMapping("/aula/{cursoId:\\d+}/leccion/{slug:[^0-9].*}")
    public String redirigirRecursoRelativo(@PathVariable("cursoId") Long cursoId,
                                           @PathVariable("slug") String slug) {
        String resolvedUrl = mediaService.resolveUrlByAliasOrPath(slug);
        return "redirect:" + resolvedUrl;
    }

    /**
     * Reproductor de lecciones del aula virtual. Comprueba que el alumno tenga acceso al curso.
     */
    @GetMapping("/aula/{cursoId:\\d+}/leccion/{leccionId:\\d+}")
    public String verLeccion(@PathVariable("cursoId") Long cursoId,
                             @PathVariable("leccionId") Long leccionId,
                             Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // Esta llamada asegura la validación interna de matrícula y suscripción activa
        Leccion leccion = lmsService.obtenerLeccionAsegurada(cursoId, leccionId, email);
        
        // Obtenemos el curso completo para renderizar el temario (módulos y lecciones en barra lateral)
        Curso curso = lmsService.obtenerCursoPorId(cursoId);

        // Calcular y pasar los días restantes de suscripción si existen
        java.util.Optional<Long> diasRestantesOpt = lmsService.obtenerDiasRestantesAcceso(email, cursoId);
        diasRestantesOpt.ifPresent(dias -> model.addAttribute("diasRestantes", dias));

        model.addAttribute("leccion", leccion);
        model.addAttribute("curso", curso);

        if (leccion.getRutaScriptInteractivo() != null && !leccion.getRutaScriptInteractivo().isEmpty()) {
            RecursoInteractivo recurso = recursoService.obtenerPorIdentificador(leccion.getRutaScriptInteractivo());
            if (recurso != null && recurso.isActivo()) {
                model.addAttribute("recursoInteractivo", recurso);
            } else {
                String resolvedUrl = mediaService.resolveUrlByAliasOrPath(leccion.getRutaScriptInteractivo());
                model.addAttribute("resolvedInteractiveUrl", resolvedUrl);
            }
        }
        
        return "reproductor";
    }
}
