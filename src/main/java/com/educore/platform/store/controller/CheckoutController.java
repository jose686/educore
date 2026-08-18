package com.educore.platform.store.controller;

import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.Paquete;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.model.PromocionCurso;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.dto.CartItem;
import com.educore.platform.store.repository.PaqueteRepository;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.store.service.PedidoService;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.store.service.StripeService;
import com.educore.platform.users.service.UserPublicService;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.educore.platform.store.model.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controlador para manejar el carrito de compras (Shopping Cart), validación de cupones y checkout general.
 */
@Controller
public class CheckoutController {

    private final CatalogoService catalogoService;
    private final PromocionService promocionService;
    private final PaqueteRepository paqueteRepository;
    private final StripeService stripeService;
    private final PedidoService pedidoService;
    private final UserPublicService userPublicService;

    @Value("${stripe.api.publishable-key:}")
    private String stripePublishableKey;

    public CheckoutController(CatalogoService catalogoService,
                              PromocionService promocionService,
                              PaqueteRepository paqueteRepository,
                              StripeService stripeService,
                              PedidoService pedidoService,
                              UserPublicService userPublicService) {
        this.catalogoService = catalogoService;
        this.promocionService = promocionService;
        this.paqueteRepository = paqueteRepository;
        this.stripeService = stripeService;
        this.pedidoService = pedidoService;
        this.userPublicService = userPublicService;
    }

    /**
     * Agrega un producto al carrito almacenado en la sesión.
     */
    @PostMapping("/carrito/agregar")
    public String agregarAlCarrito(
            @RequestParam("tipo") String tipo,
            @RequestParam("id") String idStr,
            HttpSession session) {

        List<CartItem> carrito = obtenerCarritoDeSesion(session);

        // Evitar duplicados
        boolean yaExiste = carrito.stream()
                .anyMatch(item -> item.getId().equals(idStr) && item.getTipo().equalsIgnoreCase(tipo));

        if (!yaExiste) {
            String titulo;
            BigDecimal precio;

            if ("curso".equalsIgnoreCase(tipo)) {
                UUID id = UUID.fromString(idStr);
                ProductoCurso producto = catalogoService.obtenerPorId(id);
                titulo = producto.getTitulo();
                // Precio dinámico considerando descuentos automáticos activos
                int autoDesc = obtenerDescuentoAutomatico(producto.getLmsCursoId());
                precio = producto.getPrecio().multiply(BigDecimal.valueOf(100 - autoDesc))
                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            } else if ("paquete".equalsIgnoreCase(tipo)) {
                Long id = Long.parseLong(idStr);
                Paquete paquete = paqueteRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Paquete no encontrado"));
                titulo = paquete.getTitulo();
                precio = paquete.getPrecio();
            } else if ("servicio".equalsIgnoreCase(tipo)) {
                if ("servicio_retos_pro".equals(idStr)) {
                    titulo = "Acceso a Minijuegos: Retos de Táctica Pro";
                    precio = new BigDecimal("4.99");
                } else if ("servicio_analisis_partida".equals(idStr)) {
                    titulo = "Análisis de Partida por Maestro FIDE";
                    precio = new BigDecimal("14.99");
                } else {
                    throw new IllegalArgumentException("Servicio no identificado");
                }
            } else {
                return "redirect:/catalogo";
            }

            CartItem cartItem = CartItem.builder()
                    .id(idStr)
                    .tipo(tipo.toLowerCase())
                    .titulo(titulo)
                    .precio(precio)
                    .build();

            carrito.add(cartItem);
        }

        if ("servicio".equalsIgnoreCase(tipo)) {
            return "redirect:/juegos?agregado_exito=true";
        }
        return "redirect:/catalogo?agregado_exito=true";
    }

    /**
     * Elimina un producto del carrito.
     */
    @PostMapping("/carrito/eliminar")
    public String eliminarDelCarrito(
            @RequestParam("tipo") String tipo,
            @RequestParam("id") String idStr,
            HttpSession session) {

        List<CartItem> carrito = obtenerCarritoDeSesion(session);
        carrito.removeIf(item -> item.getId().equals(idStr) && item.getTipo().equalsIgnoreCase(tipo));

        return "redirect:/carrito";
    }

    /**
     * Muestra la vista del Carrito / Checkout general.
     */
    @GetMapping({"/carrito", "/checkout"})
    public String showCarrito(
            @RequestParam(value = "coupon", required = false) String couponCode,
            HttpSession session,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        List<CartItem> carrito = obtenerCarritoDeSesion(session);
        if (carrito.isEmpty()) {
            model.addAttribute("carritoVacio", true);
            return "checkout"; // Renders empty state
        }

        BigDecimal subtotal = carrito.stream()
                .map(CartItem::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int cupDesc = 0;
        Cupon cupon = null;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            try {
                cupon = promocionService.validarYObtenerCupon(couponCode);
                if (cupon.getTipo() != TipoCupon.DESCUENTO) {
                    model.addAttribute("cuponError", "El cupón ingresado no es aplicable a descuentos en el checkout.");
                } else {
                    cupDesc = cupon.getDescuentoPorcentaje();
                    model.addAttribute("cuponExito", "Cupón '" + cupon.getCodigo() + "' (" + cupDesc + "%) aplicado correctamente.");
                }
            } catch (IllegalArgumentException e) {
                model.addAttribute("cuponError", e.getMessage());
            }
        }

        BigDecimal descuentoCuponMonto = subtotal.multiply(BigDecimal.valueOf(cupDesc))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        BigDecimal precioFinal = subtotal.subtract(descuentoCuponMonto);

        model.addAttribute("carrito", carrito);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("cupDesc", cupDesc);
        model.addAttribute("descuentoCuponMonto", descuentoCuponMonto);
        model.addAttribute("precioFinal", precioFinal);
        model.addAttribute("cuponCodigo", cupon != null && cupDesc > 0 ? cupon.getCodigo() : null);
        model.addAttribute("stripePublishableKey", stripePublishableKey);

        return "checkout";
    }

    /**
     * Procesa la compra del carrito completo y redirige al checkout de Stripe.
     */
    @PostMapping({"/carrito/pay", "/checkout/pay"})
    public String processPayment(
            @RequestParam(value = "coupon", required = false) String couponCode,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        String email = auth.getName();

        List<CartItem> carrito = obtenerCarritoDeSesion(session);
        if (carrito.isEmpty()) {
            return "redirect:/carrito";
        }

        int cupDesc = 0;
        if (couponCode != null && !couponCode.trim().isEmpty()) {
            try {
                Cupon cupon = promocionService.validarYObtenerCupon(couponCode);
                if (cupon.getTipo() == TipoCupon.DESCUENTO) {
                    cupDesc = cupon.getDescuentoPorcentaje();
                }
            } catch (IllegalArgumentException e) {
                return "redirect:/carrito?coupon=" + couponCode;
            }
        }

        // Obtener la URL base
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

            // Limpiar el carrito de la sesión tras crear la sesión de pago correctamente
            session.removeAttribute("carrito");

            return "redirect:" + stripeSession.getUrl();
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar el pago con Stripe: " + e.getMessage());
            return "redirect:/carrito?coupon=" + couponCode + "&error_stripe=true";
        }
    }

    @GetMapping("/checkout/success")
    public String paymentSuccess(@RequestParam(value = "session_id", required = false) String sessionId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String email = auth.getName();
            refreshSecurityContextIfRoleChanged(auth, email);
        }
        return "checkout-success";
    }

    private void refreshSecurityContextIfRoleChanged(Authentication auth, String email) {
        userPublicService.getUserRoleByEmail(email).ifPresent(role -> {
            String currentRole = auth.getAuthorities().iterator().next().getAuthority();
            String dbRole = "ROLE_" + role;
            if (!currentRole.equals(dbRole)) {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(dbRole);
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(),
                        auth.getCredentials(),
                        List.of(authority)
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
        });
    }

    @GetMapping("/checkout/cancel")
    public String paymentCancel() {
        return "redirect:/catalogo?pago_cancelado=true";
    }




    @SuppressWarnings("unchecked")
    private List<CartItem> obtenerCarritoDeSesion(HttpSession session) {
        List<CartItem> carrito = (List<CartItem>) session.getAttribute("carrito");
        if (carrito == null) {
            carrito = new ArrayList<>();
            session.setAttribute("carrito", carrito);
        }
        return carrito;
    }

    private int obtenerDescuentoAutomatico(Long lmsCursoId) {
        List<PromocionCurso> todasPromociones = promocionService.obtenerPromocionesCurso();
        LocalDateTime now = LocalDateTime.now();
        int maxDescuento = 0;
        for (PromocionCurso promo : todasPromociones) {
            if (promo.getCursoId().equals(lmsCursoId) &&
                    promo.getFechaInicio().isBefore(now) &&
                    promo.getFechaFin().isAfter(now)) {
                maxDescuento = Math.max(maxDescuento, promo.getPorcentajeDescuento());
            }
        }
        return maxDescuento;
    }
}
