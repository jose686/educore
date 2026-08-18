package com.educore.platform.store.controller;

import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.model.PromocionCurso;
import com.educore.platform.store.event.CursoCompradoEvent;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.store.service.PromocionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controlador para la visualización del catálogo, precios dinámicos, paquetes y compras.
 */
@Controller
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final ApplicationEventPublisher eventPublisher;
    private final PromocionService promocionService;

    public CatalogoController(CatalogoService catalogoService,
                               ApplicationEventPublisher eventPublisher,
                               PromocionService promocionService) {
        this.catalogoService = catalogoService;
        this.eventPublisher = eventPublisher;
        this.promocionService = promocionService;
    }

    /**
     * Muestra el catálogo con precios dinámicos calculados por jerarquía de descuentos:
     * PrecioFinal = PrecioBase - DescuentoAutomático(Lanzamiento/Oferta) - DescuentoCupón(Manual)
     */
    @GetMapping({"/catalogo", "/cursos"})
    public String showCatalog(Model model, jakarta.servlet.http.HttpSession session) {
        List<ProductoCurso> productos = catalogoService.obtenerCatalogoPublico();
        model.addAttribute("productos", productos);

        // Calcular descuentos automáticos activos por curso
        List<PromocionCurso> todasPromociones = promocionService.obtenerPromocionesCurso();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Integer> descuentosAutomaticos = new HashMap<>();
        for (PromocionCurso promo : todasPromociones) {
            if (promo.getFechaInicio().isBefore(now) && promo.getFechaFin().isAfter(now)) {
                // Si hay varias activas, usar el porcentaje mayor
                descuentosAutomaticos.merge(promo.getCursoId(), promo.getPorcentajeDescuento(), Math::max);
            }
        }
        model.addAttribute("descuentosAutomaticos", descuentosAutomaticos);

        // Cupón de sesión activo (descuento manual)
        com.educore.platform.store.model.Cupon cupon =
                (com.educore.platform.store.model.Cupon) session.getAttribute("cuponDescuentoActivo");
        if (cupon != null && cupon.isActivo() &&
                cupon.getTipo() == com.educore.platform.store.model.TipoCupon.DESCUENTO) {
            model.addAttribute("cuponDescuento", cupon);
        }

        // Paquetes activos disponibles
        model.addAttribute("paquetes", promocionService.obtenerTodosLosPaquetes().stream()
                .filter(com.educore.platform.store.model.Paquete::isActivo)
                .toList());

        return "catalogo";
    }

    /**
     * Procesa la compra simulada de un producto de curso y dispara el evento de dominio.
     */
    @PostMapping("/comprar/{id}")
    public String comprarCurso(@PathVariable("id") UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        String email = auth.getName();
        ProductoCurso producto = catalogoService.obtenerPorId(id);
        eventPublisher.publishEvent(new CursoCompradoEvent(email, producto.getLmsCursoId()));
        return "redirect:/catalogo?compra_exitosa=true";
    }
}
