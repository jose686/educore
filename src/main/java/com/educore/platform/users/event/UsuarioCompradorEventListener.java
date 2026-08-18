package com.educore.platform.users.event;

import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.users.service.UserPublicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Escuchador de eventos del módulo 'users' que reacciona a la compra de un curso.
 *
 * <p>NOTA TÉCNICA: La promoción real del rol (VISITOR → STUDENT) se delega a
 * {@link UserPublicService#promoverAEstudianteSiEsVisitante(String)}, que está correctamente
 * marcada con {@code @Transactional} y es llamada desde el hilo del evento.
 * Esta clase ya no duplica la lógica de acceso al repositorio directamente.</p>
 *
 * <p>IMPORTANTE: No se intenta refrescar el SecurityContext aquí porque este evento
 * puede procesarse en cualquier contexto (incluyendo el hilo del webhook de Stripe)
 * donde no existe una sesión HTTP activa. El usuario verá su nuevo rol en el siguiente
 * inicio de sesión o cuando Spring Security recargue sus credenciales.</p>
 */
@Component
@Slf4j
public class UsuarioCompradorEventListener {

    private final UserPublicService userPublicService;

    public UsuarioCompradorEventListener(UserPublicService userPublicService) {
        this.userPublicService = userPublicService;
    }

    /**
     * Reacciona al evento de compra de curso para asegurar que el comprador
     * tiene el rol de STUDENT.
     *
     * <p>La promoción real ya es gestionada por LmsInscripcionEventListener dentro
     * de la misma transacción que crea la inscripción. Este listener actúa como
     * respaldo de seguridad (en caso de que no haya lmsCursoId pero sí haya compra).</p>
     */
    @EventListener
    public void onCursoComprado(CursoCompradoEvent event) {
        log.info("[USER-COMPRADOR] Verificando rol del usuario '{}' tras compra.", event.emailUsuario());
        try {
            userPublicService.promoverAEstudianteSiEsVisitante(event.emailUsuario());
            log.debug("[USER-COMPRADOR] Rol de '{}' verificado correctamente.", event.emailUsuario());
        } catch (Exception ex) {
            log.error("[USER-COMPRADOR] Error al verificar/actualizar el rol del usuario '{}': {}",
                    event.emailUsuario(), ex.getMessage(), ex);
        }
    }
}
