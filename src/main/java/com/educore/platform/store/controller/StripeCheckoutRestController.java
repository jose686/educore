package com.educore.platform.store.controller;

import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.dto.CartItem;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.store.service.StripeService;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.users.service.UserPublicService;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rest controller for creating Stripe Checkout sessions for the shopping cart.
 */
@RestController
public class StripeCheckoutRestController {

    private final StripeService stripeService;
    private final PromocionService promocionService;
    private final UserPublicService userPublicService;
    private final CatalogoService catalogoService;

    public StripeCheckoutRestController(StripeService stripeService,
                                         PromocionService promocionService,
                                         UserPublicService userPublicService,
                                         CatalogoService catalogoService) {
        this.stripeService = stripeService;
        this.promocionService = promocionService;
        this.userPublicService = userPublicService;
        this.catalogoService = catalogoService;
    }

    /**
     * Genera una sesión de Stripe Checkout para los ítems actuales en el carrito.
     */
    @PostMapping("/api/v1/checkout/create-session")
    public ResponseEntity<?> createSession(
            @RequestParam(value = "coupon", required = false) String couponCode,
            HttpSession session,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Debe iniciar sesión para realizar la compra."));
        }
        String email = auth.getName();

        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");
        if (carrito == null || carrito.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El carrito de compras está vacío."));
        }

        int cupDesc = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            try {
                Cupon cupon = promocionService.validarYObtenerCupon(couponCode);
                if (cupon.getTipo() == TipoCupon.DESCUENTO) {
                    cupDesc = cupon.getDescuentoPorcentaje();
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
        }

        // Determinar la URL base dinámicamente para las URLs de éxito y cancelación
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort);

        String successUrl = baseUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = baseUrl + "/checkout/cancel";

        Long userId = userPublicService.getUserIdByEmail(email).orElse(null);
        java.util.StringJoiner cursoIdsJoiner = new java.util.StringJoiner(",");
        Long cursoId = null;
        for (CartItem item : carrito) {
            if ("curso".equalsIgnoreCase(item.getTipo())) {
                try {
                    ProductoCurso prod = catalogoService.obtenerPorId(UUID.fromString(item.getId()));
                    if (prod != null) {
                        cursoIdsJoiner.add(String.valueOf(prod.getLmsCursoId()));
                        if (cursoId == null) {
                            cursoId = prod.getLmsCursoId();
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing/retrieval error
                }
            }
        }
        String cursoIds = cursoIdsJoiner.length() > 0 ? cursoIdsJoiner.toString() : null;

        try {
            Session stripeSession = stripeService.crearSesionPagoCarrito(
                    carrito,
                    cupDesc,
                    email,
                    userId,
                    cursoId,
                    cursoIds,
                    successUrl,
                    cancelUrl
            );

            // Limpiamos el carrito tras generar la sesión correctamente
            session.removeAttribute("carrito");

            Map<String, String> response = new HashMap<>();
            response.put("id", stripeSession.getId());
            response.put("url", stripeSession.getUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear la sesión de pago: " + e.getMessage()));
        }
    }
}
