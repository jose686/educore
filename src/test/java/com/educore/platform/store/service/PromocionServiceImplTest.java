package com.educore.platform.store.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.CursoRepository;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.lms.service.AccesoService;
import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.GuestToken;
import com.educore.platform.store.model.Paquete;
import com.educore.platform.store.model.PromocionCurso;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.repository.CuponRepository;
import com.educore.platform.store.repository.GuestTokenRepository;
import com.educore.platform.store.repository.PaqueteRepository;
import com.educore.platform.store.repository.PromocionCursoRepository;
import com.educore.platform.users.service.UserPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para PromocionServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class PromocionServiceImplTest {

    @Mock private CuponRepository cuponRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private UserPublicService userPublicService;
    @Mock private GuestTokenRepository guestTokenRepository;
    @Mock private PromocionCursoRepository promocionCursoRepository;
    @Mock private PaqueteRepository paqueteRepository;
    @Mock private AccesoService accesoService;

    @InjectMocks
    private PromocionServiceImpl promocionService;

    private String email;
    private Long userId;

    @BeforeEach
    void setUp() {
        email = "student@educore.com";
        userId = 1L;
    }

    @Test
    void obtenerTodosLosCupones_ShouldReturnList() {
        when(cuponRepository.findAll()).thenReturn(Collections.emptyList());
        List<Cupon> result = promocionService.obtenerTodosLosCupones();
        assertNotNull(result);
    }

    @Test
    void crearCupon_ShouldSaveAndReturn() {
        Cupon cupon = Cupon.builder().codigo("DISC10").build();
        when(cuponRepository.save(any(Cupon.class))).thenReturn(cupon);

        Cupon result = promocionService.crearCupon("disc10", "DESCUENTO", 10, 0, 100L);

        assertNotNull(result);
        verify(cuponRepository, times(1)).save(any(Cupon.class));
    }

    @Test
    void eliminarCupon_ShouldCallRepositoryDelete() {
        promocionService.eliminarCupon(10L);
        verify(cuponRepository, times(1)).deleteById(10L);
    }

    @Test
    void validarYObtenerCupon_WhenValid_ShouldReturnCupon() {
        Cupon cupon = Cupon.builder().codigo("SALE").activo(true).build();
        when(cuponRepository.findByCodigoAndActivoTrue("SALE")).thenReturn(Optional.of(cupon));

        Cupon result = promocionService.validarYObtenerCupon(" sale ");

        assertNotNull(result);
        assertEquals(cupon, result);
    }

    @Test
    void validarYObtenerCupon_WhenInvalid_ShouldThrowException() {
        when(cuponRepository.findByCodigoAndActivoTrue("BAD")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> promocionService.validarYObtenerCupon("BAD"));
    }

    @Test
    void aplicarAccesoTemporal_ShouldThrowException_WhenNotTemporalType() {
        Cupon cupon = Cupon.builder().tipo(TipoCupon.DESCUENTO).build();
        assertThrows(IllegalArgumentException.class, () -> promocionService.aplicarAccesoTemporal(cupon, email));
    }

    @Test
    void aplicarAccesoTemporal_ShouldThrowException_WhenUserNotFound() {
        Cupon cupon = Cupon.builder().tipo(TipoCupon.ACCESO_TEMPORAL).build();
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> promocionService.aplicarAccesoTemporal(cupon, email));
    }

    @Test
    void aplicarAccesoTemporal_ShouldThrowException_WhenCursoDoesNotExist() {
        Cupon cupon = Cupon.builder().tipo(TipoCupon.ACCESO_TEMPORAL).cursoId(999L).build();
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(cursoRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> promocionService.aplicarAccesoTemporal(cupon, email));
    }

    @Test
    void aplicarAccesoTemporal_ShouldSaveEnrollment_WhenValid() {
        Cupon cupon = Cupon.builder().tipo(TipoCupon.ACCESO_TEMPORAL).cursoId(100L).diasAcceso(7).build();
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(cursoRepository.existsById(100L)).thenReturn(true);

        promocionService.aplicarAccesoTemporal(cupon, email);

        verify(inscripcionRepository, times(1)).save(any(Inscripcion.class));
    }

    @Test
    void obtenerPromocionesCurso_ShouldReturnList() {
        when(promocionCursoRepository.findAll()).thenReturn(Collections.emptyList());
        List<PromocionCurso> result = promocionService.obtenerPromocionesCurso();
        assertNotNull(result);
    }

    @Test
    void crearPromocionCurso_ShouldSaveAndReturn() {
        PromocionCurso promo = PromocionCurso.builder().id(10L).build();
        when(promocionCursoRepository.save(any(PromocionCurso.class))).thenReturn(promo);

        PromocionCurso result = promocionService.crearPromocionCurso(100L, "AUTOMATICA", 15, LocalDateTime.now(), LocalDateTime.now().plusDays(5));

        assertNotNull(result);
        verify(promocionCursoRepository, times(1)).save(any(PromocionCurso.class));
    }

    @Test
    void eliminarPromocionCurso_ShouldCallRepositoryDelete() {
        promocionService.eliminarPromocionCurso(10L);
        verify(promocionCursoRepository, times(1)).deleteById(10L);
    }

    @Test
    void obtenerTodosLosPaquetes_ShouldReturnList() {
        when(paqueteRepository.findAll()).thenReturn(Collections.emptyList());
        List<Paquete> result = promocionService.obtenerTodosLosPaquetes();
        assertNotNull(result);
    }

    @Test
    void crearPaquete_ShouldSaveAndReturn() {
        Paquete paquete = Paquete.builder().titulo("Pack").build();
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(paquete);

        Paquete result = promocionService.crearPaquete("Pack", "Desc", BigDecimal.TEN, Set.of(1L, 2L));

        assertNotNull(result);
        verify(paqueteRepository, times(1)).save(any(Paquete.class));
    }

    @Test
    void eliminarPaquete_ShouldCallRepositoryDelete() {
        promocionService.eliminarPaquete(10L);
        verify(paqueteRepository, times(1)).deleteById(10L);
    }

    @Test
    void comprarPaquete_ShouldCreateEnrollmentAndPromoteUser() {
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));

        promocionService.comprarPaquete(10L, email);

        verify(accesoService, times(1)).crearInscripcionesDePaquete(10L, userId);
        verify(userPublicService, times(1)).promoverAEstudianteSiEsVisitante(email);
    }

    @Test
    void obtenerTodosLosTokens_ShouldReturnList() {
        when(guestTokenRepository.findAll()).thenReturn(Collections.emptyList());
        List<GuestToken> result = promocionService.obtenerTodosLosTokens();
        assertNotNull(result);
    }

    @Test
    void crearGuestToken_ShouldSaveAndReturn() {
        GuestToken token = GuestToken.builder().token("INV-ABC").build();
        when(guestTokenRepository.save(any(GuestToken.class))).thenReturn(token);

        GuestToken result = promocionService.crearGuestToken(Set.of(100L), 30);

        assertNotNull(result);
        verify(guestTokenRepository, times(1)).save(any(GuestToken.class));
    }

    @Test
    void eliminarGuestToken_ShouldCallRepositoryDelete() {
        promocionService.eliminarGuestToken(10L);
        verify(guestTokenRepository, times(1)).deleteById(10L);
    }

    @Test
    void canjearGuestToken_ShouldThrowException_WhenTokenInvalidOrInactive() {
        when(guestTokenRepository.findByTokenAndActivoTrue("BAD")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> promocionService.canjearGuestToken("BAD", email));
    }

    @Test
    void canjearGuestToken_ShouldThrowException_WhenUserNotFound() {
        GuestToken token = GuestToken.builder().token("INV-ABC").activo(true).build();
        when(guestTokenRepository.findByTokenAndActivoTrue("INV-ABC")).thenReturn(Optional.of(token));
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> promocionService.canjearGuestToken("INV-ABC", email));
    }

    @Test
    void canjearGuestToken_ShouldThrowException_WhenCursoDoesNotExist() {
        GuestToken token = GuestToken.builder().token("INV-ABC").cursoIds(Set.of(999L)).activo(true).build();
        when(guestTokenRepository.findByTokenAndActivoTrue("INV-ABC")).thenReturn(Optional.of(token));
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(cursoRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> promocionService.canjearGuestToken("INV-ABC", email));
    }

    @Test
    void canjearGuestToken_ShouldEnrollAndDeactivateToken_WhenValid() {
        GuestToken token = GuestToken.builder().token("INV-ABC").cursoIds(Set.of(100L)).diasAcceso(30).activo(true).build();
        when(guestTokenRepository.findByTokenAndActivoTrue("INV-ABC")).thenReturn(Optional.of(token));
        when(userPublicService.getUserIdByEmail(email)).thenReturn(Optional.of(userId));
        when(cursoRepository.existsById(100L)).thenReturn(true);

        promocionService.canjearGuestToken("INV-ABC", email);

        verify(inscripcionRepository, times(1)).save(any(Inscripcion.class));
        assertFalse(token.isActivo());
        assertEquals(userId, token.getUsuarioId());
        assertNotNull(token.getFechaCanje());
        verify(guestTokenRepository, times(1)).save(token);
    }
}
