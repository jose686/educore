package com.educore.platform.event;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.users.model.Role;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para verificar el comportamiento desacoplado de eventos.
 * Comprueba que al publicarse un CursoCompradoEvent se actualiza el rol en 'users'
 * y se registra la inscripción en 'lms' de forma reactiva.
 */
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Test
    void whenCursoCompradoEventFired_thenUserRoleUpgradedAndEnrollmentCreated() {
        // Arrange: Registrar un usuario de prueba en estado VISITOR
        Usuario usuario = Usuario.builder()
                .nombre("Alumno Visitante")
                .email("visitante@educore.com")
                .password("clave123")
                .role(Role.VISITOR)
                .activo(true)
                .build();
        usuario = usuarioRepository.save(usuario);
        
        Long studentId = usuario.getId();
        Long lmsCursoId = 101L;

        // Act: Disparar el evento de compra del catálogo
        eventPublisher.publishEvent(new CursoCompradoEvent("visitante@educore.com", lmsCursoId));

        // Assert: Esperar mediante un bucle de sondeo para tolerar el procesamiento asíncrono
        boolean completadoConExito = false;
        
        for (int i = 0; i < 20; i++) {
            Optional<Usuario> usuarioActualizado = usuarioRepository.findById(studentId);
            Optional<Inscripcion> inscripcion = inscripcionRepository.findByStudentIdAndLmsCursoId(studentId, lmsCursoId);
            
            if (usuarioActualizado.isPresent() 
                    && usuarioActualizado.get().getRole() == Role.STUDENT 
                    && inscripcion.isPresent()) {
                completadoConExito = true;
                break;
            }
            
            try {
                Thread.sleep(100); // Espera 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verificamos que el cambio de rol y la matrícula se hayan ejecutado
        assertThat(completadoConExito).isTrue();
    }
}
