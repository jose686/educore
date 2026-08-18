package com.educore.platform.lms.service;

import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.model.Modulo;
import com.educore.platform.lms.dto.LeccionDTO;
import com.educore.platform.lms.dto.ModuloDTO;
import com.educore.platform.lms.repository.CursoRepository;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.lms.repository.LeccionRepository;
import com.educore.platform.lms.repository.ModuloRepository;
import com.educore.platform.users.service.UserPublicService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del servicio LmsService.
 */
@Service
@Transactional(readOnly = true)
public class LmsServiceImpl implements LmsService {

    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;
    private final ModuloRepository moduloRepository;
    private final LeccionRepository leccionRepository;
    private final UserPublicService userPublicService;
    private final AccesoService accesoService;

    public LmsServiceImpl(InscripcionRepository inscripcionRepository,
                          CursoRepository cursoRepository,
                          ModuloRepository moduloRepository,
                          LeccionRepository leccionRepository,
                          UserPublicService userPublicService,
                          AccesoService accesoService) {
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
        this.moduloRepository = moduloRepository;
        this.leccionRepository = leccionRepository;
        this.userPublicService = userPublicService;
        this.accesoService = accesoService;
    }

    @Override
    public List<Curso> obtenerCursosEstudiante(String emailUsuario) {
        Long studentId = userPublicService.getUserIdByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + emailUsuario));

        List<Inscripcion> inscripciones = inscripcionRepository.findByStudentId(studentId);
        LocalDateTime now = LocalDateTime.now();

        List<Long> cursoIds = inscripciones.stream()
                .filter(i -> i.getFechaFin() == null || i.getFechaFin().isAfter(now))
                .map(Inscripcion::getLmsCursoId)
                .distinct()
                .collect(Collectors.toList());

        return cursoRepository.findAllById(cursoIds);
    }

    @Override
    public Leccion obtenerLeccionAsegurada(Long cursoId, Long leccionId, String emailUsuario) {
        Long studentId = userPublicService.getUserIdByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + emailUsuario));

        if (!accesoService.tieneAcceso(studentId, cursoId)) {
            throw new AccessDeniedException("Acceso denegado: No tienes acceso activo para el curso con ID " + cursoId);
        }

        Leccion leccion = leccionRepository.findById(leccionId)
                .orElseThrow(() -> new IllegalArgumentException("La lección no existe: " + leccionId));

        if (!leccion.getModulo().getCurso().getId().equals(cursoId)) {
            throw new IllegalArgumentException("La lección no pertenece al curso especificado.");
        }

        return leccion;
    }

    @Override
    public Curso obtenerCursoPorId(Long cursoId) {
        return cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("El curso no existe: " + cursoId));
    }

    @Override
    public Modulo obtenerModuloPorId(Long moduloId) {
        return moduloRepository.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("El módulo no existe: " + moduloId));
    }

    @Override
    @Transactional
    public Modulo crearModulo(Long cursoId, ModuloDTO datos) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new IllegalArgumentException("El curso no existe: " + cursoId));

        Integer maxOrden = moduloRepository.findMaxOrdenByCursoId(cursoId);
        int nuevoOrden = (maxOrden != null) ? maxOrden + 1 : 1;

        Modulo modulo = Modulo.builder()
                .nombre(datos.getNombre())
                .orden(nuevoOrden)
                .curso(curso)
                .build();

        return moduloRepository.save(modulo);
    }

    @Override
    @Transactional
    public Leccion crearLeccion(Long moduloId, LeccionDTO datos) {
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("El módulo no existe: " + moduloId));

        Integer maxOrden = leccionRepository.findMaxOrdenByModuloId(moduloId);
        int nuevoOrden = (maxOrden != null) ? maxOrden + 1 : 1;

        Leccion leccion = Leccion.builder()
                .titulo(datos.getTitulo())
                .contenido(datos.getContenido())
                .videoUrl(datos.getVideoUrl())
                .esVideoLocal(datos.getEsVideoLocal())
                .rutaScriptInteractivo(datos.getRutaScriptInteractivo())
                .orden(nuevoOrden)
                .modulo(modulo)
                .build();

        return leccionRepository.save(leccion);
    }

    @Override
    public Leccion obtenerLeccionPorId(Long leccionId) {
        return leccionRepository.findById(leccionId)
                .orElseThrow(() -> new IllegalArgumentException("La lección no existe: " + leccionId));
    }

    @Override
    @Transactional
    public Modulo actualizarModulo(Long moduloId, ModuloDTO datos) {
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("El módulo no existe: " + moduloId));
        modulo.setNombre(datos.getNombre());
        return moduloRepository.save(modulo);
    }

    @Override
    @Transactional
    public void eliminarModulo(Long moduloId) {
        Modulo modulo = moduloRepository.findById(moduloId)
                .orElseThrow(() -> new IllegalArgumentException("El módulo no existe: " + moduloId));
        moduloRepository.delete(modulo);
    }

    @Override
    @Transactional
    public Leccion actualizarLeccion(Long leccionId, LeccionDTO datos) {
        Leccion leccion = leccionRepository.findById(leccionId)
                .orElseThrow(() -> new IllegalArgumentException("La lección no existe: " + leccionId));
        leccion.setTitulo(datos.getTitulo());
        leccion.setContenido(datos.getContenido());
        leccion.setVideoUrl(datos.getVideoUrl());
        leccion.setEsVideoLocal(datos.getEsVideoLocal());
        leccion.setRutaScriptInteractivo(datos.getRutaScriptInteractivo());
        return leccionRepository.save(leccion);
    }

    @Override
    @Transactional
    public void eliminarLeccion(Long leccionId) {
        Leccion leccion = leccionRepository.findById(leccionId)
                .orElseThrow(() -> new IllegalArgumentException("La lección no existe: " + leccionId));
        leccionRepository.delete(leccion);
    }

    @Override
    public Optional<Long> obtenerDiasRestantesAcceso(String emailUsuario, Long cursoId) {
        Long studentId = userPublicService.getUserIdByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + emailUsuario));

        var inscripcionOpt = inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, cursoId);
        if (inscripcionOpt.isEmpty()) {
            return Optional.empty();
        }

        Inscripcion inscripcion = inscripcionOpt.get();
        if (inscripcion.getFechaFin() == null) {
            return Optional.empty(); // Acceso permanente, no hay días restantes
        }

        if (inscripcion.getFechaFin().isBefore(LocalDateTime.now())) {
            return Optional.empty(); // Ya expirado
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), inscripcion.getFechaFin());
        if (days == 0 && inscripcion.getFechaFin().isAfter(LocalDateTime.now())) {
            days = 1;
        }
        return Optional.of(days);
    }

    @Override
    public List<Curso> obtenerTodosLosCursos() {
        return cursoRepository.findAll();
    }

    @Override
    @Transactional
    public Curso crearCurso(Curso curso) {
        return cursoRepository.save(curso);
    }
}
