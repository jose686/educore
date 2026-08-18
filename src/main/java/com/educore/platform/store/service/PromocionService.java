package com.educore.platform.store.service;

import com.educore.platform.store.model.Cupon;
import java.util.List;
import java.util.Set;

/**
 * Servicio para gestionar la administración y activación de cupones promocionales.
 */
public interface PromocionService {
    List<Cupon> obtenerTodosLosCupones();
    Cupon crearCupon(String codigo, String tipo, Integer descuentoPorcentaje, Integer diasAcceso, Long cursoId);
    void eliminarCupon(Long id);
    Cupon validarYObtenerCupon(String codigo);
    void aplicarAccesoTemporal(Cupon cupon, String emailUsuario);

    // Promociones de Curso Automáticas
    List<com.educore.platform.store.model.PromocionCurso> obtenerPromocionesCurso();
    com.educore.platform.store.model.PromocionCurso crearPromocionCurso(Long cursoId, String tipo, Integer porcentajeDescuento, java.time.LocalDateTime inicio, java.time.LocalDateTime fin);
    void eliminarPromocionCurso(Long id);

    // Paquetes / Bundles
    List<com.educore.platform.store.model.Paquete> obtenerTodosLosPaquetes();
    com.educore.platform.store.model.Paquete crearPaquete(String titulo, String descripcion, java.math.BigDecimal precio, Set<Long> cursoIds);
    void eliminarPaquete(Long id);
    void comprarPaquete(Long paqueteId, String emailUsuario);

    // Tokens de Invitados
    List<com.educore.platform.store.model.GuestToken> obtenerTodosLosTokens();
    com.educore.platform.store.model.GuestToken crearGuestToken(Set<Long> cursoIds, Integer diasAcceso);
    void eliminarGuestToken(Long id);
    void canjearGuestToken(String token, String emailUsuario);
}
