package com.educore.platform.lms.event;

import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.lms.service.AulaVirtualService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Escuchador de eventos del módulo 'lms' que se encarga de matricular
 * al alumno cuando se realiza la compra de un curso.
 *
 * <p>NOTA TÉCNICA: Este listener es SÍNCRONO (sin @Async) de forma deliberada.
 * Combinar @Async con @Transactional en el mismo método causa que el proxy
 * transaccional se aplique sobre un hilo separado sin contexto de EntityManager
 * del hilo padre, provocando que la save() falle silenciosamente.
 * La lógica es rápida (una consulta + un insert), por lo que no justifica asincronía.</p>
 */
@Component
@Slf4j
public class LmsInscripcionEventListener {

    private final AulaVirtualService aulaVirtualService;

    public LmsInscripcionEventListener(AulaVirtualService aulaVirtualService) {
        this.aulaVirtualService = aulaVirtualService;
    }

    /**
     * Escucha el evento de compra y delega el enrolamiento a AulaVirtualService.
     */
    @EventListener
    @Transactional
    public void onCursoComprado(CursoCompradoEvent event) {
        log.info("[LMS-INSCRIPCION] Procesando evento CursoComprado - email: {}, lmsCursoId: {}",
                event.emailUsuario(), event.lmsCursoId());
        aulaVirtualService.matricularAlumno(event.emailUsuario(), event.lmsCursoId());
    }
}
