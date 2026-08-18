package com.educore.platform.store.event;

/**
 * Evento de dominio publicado tras el pago/compra exitosa de un curso.
 * Contiene información mínima necesaria para que otros módulos respondan a este suceso.
 *
 * @param emailUsuario Email del usuario que realizó la compra.
 * @param lmsCursoId   ID del curso del LMS al que se le concede acceso.
 */
public record CursoCompradoEvent(
    String emailUsuario,
    Long lmsCursoId
) {
}
