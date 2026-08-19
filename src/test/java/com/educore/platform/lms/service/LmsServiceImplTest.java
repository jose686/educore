package com.educore.platform.lms.service;

import com.educore.platform.lms.dto.LeccionDTO;
import com.educore.platform.lms.dto.ModuloDTO;
import com.educore.platform.lms.model.Curso;
import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.model.Leccion;
import com.educore.platform.lms.model.Modulo;
import com.educore.platform.lms.repository.CursoRepository;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.lms.repository.LeccionRepository;
import com.educore.platform.lms.repository.ModuloRepository;
import com.educore.platform.users.service.UserPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para LmsServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class LmsServiceImplTest {

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private ModuloRepository moduloRepository;
    @Mock private LeccionRepository leccionRepository;
    @Mock private UserPublicService userPublicService;
    @Mock private AccesoService accesoService;

    @InjectMocks
    private LmsServiceImpl lmsService;

    private String email;
    private Long userId;
    private Long cursoId;
    private Long moduloId;
    private Long leccionId;

    @BeforeEach
    void setUp() {
        email = "student@educore.com";
        userId = 1L;
        cursoId = 100L;
        moduloId = 200L;
        leccionId = 300L;
    }

    @Test
    void obtenerCursosEstudiante_ShouldThrowException_WhenUserNotFound() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> lmsService.obtenerCursosEstudiante(email));
    }

    @Test
    void obtenerCursosEstudiante_ShouldReturnOnlyActiveCourses() {
        Inscripcion active = Inscripcion.builder().studentId(userId).lmsCursoId(10L).fechaFin(LocalDateTime.now().plusDays(5)).build();
        Inscripcion expired = Inscripcion.builder().studentId(userId).lmsCursoId(20L).fechaFin(LocalDateTime.now().minusDays(1)).build();
        Inscripcion permanent = Inscripcion.builder().studentId(userId).lmsCursoId(30L).fechaFin(null).build();

        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(inscripcionRepository.findByStudentId(userId)).thenReturn(List.of(active, expired, permanent));
        when(cursoRepository.findAllById(anyList())).thenReturn(List.of(new Curso()));

        List<Curso> result = lmsService.obtenerCursosEstudiante(email);

        assertNotNull(result);
        verify(cursoRepository, times(1)).findAllById(argThat(list -> {
            List<Long> ids = new java.util.ArrayList<>();
            list.forEach(ids::add);
            return ids.contains(10L) && ids.contains(30L) && !ids.contains(20L);
        }));
    }

    @Test
    void obtenerLeccionAsegurada_ShouldThrowException_WhenUserNotFound() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> lmsService.obtenerLeccionAsegurada(cursoId, leccionId, email));
    }

    @Test
    void obtenerLeccionAsegurada_ShouldThrowAccessDenied_WhenNoAccess() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(accesoService.tieneAcceso(userId, cursoId)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> lmsService.obtenerLeccionAsegurada(cursoId, leccionId, email));
    }

    @Test
    void obtenerLeccionAsegurada_ShouldThrowException_WhenLessonNotFound() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(accesoService.tieneAcceso(userId, cursoId)).thenReturn(true);
        when(leccionRepository.findById(leccionId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> lmsService.obtenerLeccionAsegurada(cursoId, leccionId, email));
    }

    @Test
    void obtenerLeccionAsegurada_ShouldThrowException_WhenLessonDoesNotBelongToCourse() {
        Curso other = Curso.builder().id(999L).build();
        Modulo mod = Modulo.builder().curso(other).build();
        Leccion lec = Leccion.builder().modulo(mod).build();

        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(accesoService.tieneAcceso(userId, cursoId)).thenReturn(true);
        when(leccionRepository.findById(leccionId)).thenReturn(Optional.of(lec));

        assertThrows(IllegalArgumentException.class, () -> lmsService.obtenerLeccionAsegurada(cursoId, leccionId, email));
    }

    @Test
    void obtenerLeccionAsegurada_ShouldReturnLesson_WhenValid() {
        Curso curso = Curso.builder().id(cursoId).build();
        Modulo mod = Modulo.builder().curso(curso).build();
        Leccion lec = Leccion.builder().modulo(mod).build();

        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(accesoService.tieneAcceso(userId, cursoId)).thenReturn(true);
        when(leccionRepository.findById(leccionId)).thenReturn(Optional.of(lec));

        Leccion result = lmsService.obtenerLeccionAsegurada(cursoId, leccionId, email);

        assertNotNull(result);
        assertEquals(lec, result);
    }

    @Test
    void obtenerCursoPorId_ShouldThrowException_WhenNotFound() {
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> lmsService.obtenerCursoPorId(cursoId));
    }

    @Test
    void obtenerModuloPorId_ShouldThrowException_WhenNotFound() {
        when(moduloRepository.findById(moduloId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> lmsService.obtenerModuloPorId(moduloId));
    }

    @Test
    void crearModulo_ShouldSetOrderAndSave() {
        Curso curso = new Curso();
        when(cursoRepository.findById(cursoId)).thenReturn(Optional.of(curso));
        when(moduloRepository.findMaxOrdenByCursoId(cursoId)).thenReturn(4);
        when(moduloRepository.save(any(Modulo.class))).thenAnswer(i -> i.getArgument(0));

        Modulo result = lmsService.crearModulo(cursoId, new ModuloDTO("Modulo Nuevo", null));

        assertNotNull(result);
        assertEquals(5, result.getOrden());
        assertEquals("Modulo Nuevo", result.getNombre());
    }

    @Test
    void crearLeccion_ShouldSetOrderAndSave() {
        Modulo modulo = new Modulo();
        when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(modulo));
        when(leccionRepository.findMaxOrdenByModuloId(moduloId)).thenReturn(null); // No previous lessons
        when(leccionRepository.save(any(Leccion.class))).thenAnswer(i -> i.getArgument(0));

        LeccionDTO dto = LeccionDTO.builder().titulo("L1").esVideoLocal(false).build();
        Leccion result = lmsService.crearLeccion(moduloId, dto);

        assertNotNull(result);
        assertEquals(1, result.getOrden());
        assertEquals("L1", result.getTitulo());
    }

    @Test
    void actualizarModulo_ShouldUpdateNameAndSave() {
        Modulo mod = Modulo.builder().id(moduloId).nombre("Old").build();
        when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(mod));
        when(moduloRepository.save(any(Modulo.class))).thenAnswer(i -> i.getArgument(0));

        Modulo result = lmsService.actualizarModulo(moduloId, new ModuloDTO("New", null));

        assertEquals("New", result.getNombre());
    }

    @Test
    void eliminarModulo_ShouldCallRepositoryDelete() {
        Modulo mod = new Modulo();
        when(moduloRepository.findById(moduloId)).thenReturn(Optional.of(mod));

        lmsService.eliminarModulo(moduloId);

        verify(moduloRepository, times(1)).delete(mod);
    }

    @Test
    void actualizarLeccion_ShouldUpdateFieldsAndSave() {
        Leccion lec = Leccion.builder().id(leccionId).titulo("Old").build();
        when(leccionRepository.findById(leccionId)).thenReturn(Optional.of(lec));
        when(leccionRepository.save(any(Leccion.class))).thenAnswer(i -> i.getArgument(0));

        LeccionDTO dto = LeccionDTO.builder().titulo("New").contenido("C").videoUrl("V").esVideoLocal(true).build();
        Leccion result = lmsService.actualizarLeccion(leccionId, dto);

        assertEquals("New", result.getTitulo());
        assertEquals("C", result.getContenido());
        assertEquals("V", result.getVideoUrl());
        assertTrue(result.getEsVideoLocal());
    }

    @Test
    void eliminarLeccion_ShouldCallRepositoryDelete() {
        Leccion lec = new Leccion();
        when(leccionRepository.findById(leccionId)).thenReturn(Optional.of(lec));

        lmsService.eliminarLeccion(leccionId);

        verify(leccionRepository, times(1)).delete(lec);
    }

    @Test
    void obtenerDiasRestantesAcceso_ShouldReturnEmpty_WhenNoEnrollment() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(inscripcionRepository.findByStudentIdAndLmsCursoId(userId, cursoId)).thenReturn(Optional.empty());

        Optional<Long> result = lmsService.obtenerDiasRestantesAcceso(email, cursoId);

        assertTrue(result.isEmpty());
    }

    @Test
    void obtenerDiasRestantesAcceso_ShouldReturnEmpty_WhenPermanent() {
        Inscripcion ins = Inscripcion.builder().fechaFin(null).build();
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(inscripcionRepository.findByStudentIdAndLmsCursoId(userId, cursoId)).thenReturn(Optional.of(ins));

        Optional<Long> result = lmsService.obtenerDiasRestantesAcceso(email, cursoId);

        assertTrue(result.isEmpty());
    }

    @Test
    void obtenerDiasRestantesAcceso_ShouldReturnEmpty_WhenExpired() {
        Inscripcion ins = Inscripcion.builder().fechaFin(LocalDateTime.now().minusDays(5)).build();
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(inscripcionRepository.findByStudentIdAndLmsCursoId(userId, cursoId)).thenReturn(Optional.of(ins));

        Optional<Long> result = lmsService.obtenerDiasRestantesAcceso(email, cursoId);

        assertTrue(result.isEmpty());
    }

    @Test
    void obtenerDiasRestantesAcceso_ShouldReturnDays_WhenActive() {
        Inscripcion ins = Inscripcion.builder().fechaFin(LocalDateTime.now().plusDays(10).plusHours(2)).build();
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(inscripcionRepository.findByStudentIdAndLmsCursoId(userId, cursoId)).thenReturn(Optional.of(ins));

        Optional<Long> result = lmsService.obtenerDiasRestantesAcceso(email, cursoId);

        assertTrue(result.isPresent());
        assertTrue(result.get() >= 9 && result.get() <= 11);
    }
}
