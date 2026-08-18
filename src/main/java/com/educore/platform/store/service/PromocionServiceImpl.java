package com.educore.platform.store.service;

import com.educore.platform.lms.model.Inscripcion;
import com.educore.platform.lms.repository.CursoRepository;
import com.educore.platform.lms.repository.InscripcionRepository;
import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.repository.CuponRepository;
import com.educore.platform.users.service.UserPublicService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Implementación del servicio PromocionService.
 */
@Service
@Transactional
public class PromocionServiceImpl implements PromocionService {

    private final CuponRepository cuponRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CursoRepository cursoRepository;
    private final UserPublicService userPublicService;
    private final com.educore.platform.store.repository.GuestTokenRepository guestTokenRepository;
    private final com.educore.platform.store.repository.PromocionCursoRepository promocionCursoRepository;
    private final com.educore.platform.store.repository.PaqueteRepository paqueteRepository;
    private final com.educore.platform.lms.service.AccesoService accesoService;

    public PromocionServiceImpl(CuponRepository cuponRepository,
                                InscripcionRepository inscripcionRepository,
                                CursoRepository cursoRepository,
                                UserPublicService userPublicService,
                                com.educore.platform.store.repository.GuestTokenRepository guestTokenRepository,
                                com.educore.platform.store.repository.PromocionCursoRepository promocionCursoRepository,
                                com.educore.platform.store.repository.PaqueteRepository paqueteRepository,
                                com.educore.platform.lms.service.AccesoService accesoService) {
        this.cuponRepository = cuponRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.cursoRepository = cursoRepository;
        this.userPublicService = userPublicService;
        this.guestTokenRepository = guestTokenRepository;
        this.promocionCursoRepository = promocionCursoRepository;
        this.paqueteRepository = paqueteRepository;
        this.accesoService = accesoService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cupon> obtenerTodosLosCupones() {
        return cuponRepository.findAll();
    }

    @Override
    public Cupon crearCupon(String codigo, String tipo, Integer descuentoPorcentaje, Integer diasAcceso, Long cursoId) {
        Cupon cupon = Cupon.builder()
                .codigo(codigo.toUpperCase().trim())
                .tipo(TipoCupon.valueOf(tipo.toUpperCase()))
                .descuentoPorcentaje(descuentoPorcentaje)
                .diasAcceso(diasAcceso)
                .cursoId(cursoId)
                .activo(true)
                .build();
        return cuponRepository.save(cupon);
    }

    @Override
    public void eliminarCupon(Long id) {
        cuponRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Cupon validarYObtenerCupon(String codigo) {
        return cuponRepository.findByCodigoAndActivoTrue(codigo.toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("El cupón no es válido, no existe o está inactivo."));
    }

    @Override
    public void aplicarAccesoTemporal(Cupon cupon, String emailUsuario) {
        if (cupon.getTipo() != TipoCupon.ACCESO_TEMPORAL) {
            throw new IllegalArgumentException("El cupón no es de acceso temporal.");
        }
        
        Long userId = userPublicService.getUserIdByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + emailUsuario));

        Long cursoId = cupon.getCursoId();
        if (cursoId == null || !cursoRepository.existsById(cursoId)) {
            throw new IllegalArgumentException("El curso asignado al cupón no existe.");
        }

        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fin = inicio.plusDays(cupon.getDiasAcceso());

        Inscripcion inscripcion = Inscripcion.builder()
                .studentId(userId)
                .lmsCursoId(cursoId)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .build();

        inscripcionRepository.save(inscripcion);
    }

    // Promociones de Curso Automáticas
    @Override
    @Transactional(readOnly = true)
    public List<com.educore.platform.store.model.PromocionCurso> obtenerPromocionesCurso() {
        return promocionCursoRepository.findAll();
    }

    @Override
    public com.educore.platform.store.model.PromocionCurso crearPromocionCurso(Long cursoId, String tipo, Integer porcentajeDescuento, LocalDateTime inicio, LocalDateTime fin) {
        com.educore.platform.store.model.PromocionCurso promo = com.educore.platform.store.model.PromocionCurso.builder()
                .cursoId(cursoId)
                .tipo(tipo)
                .porcentajeDescuento(porcentajeDescuento)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .build();
        return promocionCursoRepository.save(promo);
    }

    @Override
    public void eliminarPromocionCurso(Long id) {
        promocionCursoRepository.deleteById(id);
    }

    // Paquetes / Bundles
    @Override
    @Transactional(readOnly = true)
    public List<com.educore.platform.store.model.Paquete> obtenerTodosLosPaquetes() {
        return paqueteRepository.findAll();
    }

    @Override
    public com.educore.platform.store.model.Paquete crearPaquete(String titulo, String descripcion, java.math.BigDecimal precio, Set<Long> cursoIds) {
        com.educore.platform.store.model.Paquete paquete = com.educore.platform.store.model.Paquete.builder()
                .titulo(titulo)
                .descripcion(descripcion)
                .precio(precio)
                .cursoIds(cursoIds)
                .activo(true)
                .build();
        return paqueteRepository.save(paquete);
    }

    @Override
    public void eliminarPaquete(Long id) {
        paqueteRepository.deleteById(id);
    }

    @Override
    public void comprarPaquete(Long paqueteId, String emailUsuario) {
        Long studentId = userPublicService.getUserIdByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + emailUsuario));
        accesoService.crearInscripcionesDePaquete(paqueteId, studentId);
        userPublicService.promoverAEstudianteSiEsVisitante(emailUsuario);
    }

    // Tokens de Invitados
    @Override
    @Transactional(readOnly = true)
    public List<com.educore.platform.store.model.GuestToken> obtenerTodosLosTokens() {
        return guestTokenRepository.findAll();
    }

    @Override
    public com.educore.platform.store.model.GuestToken crearGuestToken(Set<Long> cursoIds, Integer diasAcceso) {
        String code = "INV-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        com.educore.platform.store.model.GuestToken token = com.educore.platform.store.model.GuestToken.builder()
                .token(code)
                .cursoIds(cursoIds)
                .diasAcceso(diasAcceso)
                .activo(true)
                .build();
        return guestTokenRepository.save(token);
    }

    @Override
    public void eliminarGuestToken(Long id) {
        guestTokenRepository.deleteById(id);
    }

    @Override
    public void canjearGuestToken(String code, String emailUsuario) {
        com.educore.platform.store.model.GuestToken token = guestTokenRepository.findByTokenAndActivoTrue(code.toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido, inexistente o ya canjeado."));

        Long userId = userPublicService.getUserIdByEmail(emailUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + emailUsuario));

        for (Long cursoId : token.getCursoIds()) {
            if (!cursoRepository.existsById(cursoId)) {
                throw new IllegalArgumentException("Curso no encontrado: " + cursoId);
            }
            Inscripcion inscripcion = Inscripcion.builder()
                    .studentId(userId)
                    .lmsCursoId(cursoId)
                    .fechaInicio(LocalDateTime.now())
                    .fechaFin(LocalDateTime.now().plusDays(token.getDiasAcceso()))
                    .build();
            inscripcionRepository.save(inscripcion);
        }

        // Marcar token como inactivo y registrar quién lo canjeó
        token.setActivo(false);
        token.setUsuarioId(userId);
        token.setFechaCanje(LocalDateTime.now());
        guestTokenRepository.save(token);
    }
}
