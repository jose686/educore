package com.educore.platform.store.controller;

import com.educore.platform.store.model.Cupon;
import com.educore.platform.store.model.GuestToken;
import com.educore.platform.store.model.TipoCupon;
import com.educore.platform.store.repository.GuestTokenRepository;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.model.Role;
import com.educore.platform.users.repository.UsuarioRepository;
import com.educore.platform.lms.repository.InscripcionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Controlador para la aplicación de cupones, canje público de tokens de invitado y compra de paquetes.
 */
@Controller
public class PromocionController {

    private final PromocionService promocionService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final InscripcionRepository inscripcionRepository;
    private final GuestTokenRepository guestTokenRepository;

    public PromocionController(PromocionService promocionService,
                               UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               UserDetailsService userDetailsService,
                               InscripcionRepository inscripcionRepository,
                               GuestTokenRepository guestTokenRepository) {
        this.promocionService = promocionService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.inscripcionRepository = inscripcionRepository;
        this.guestTokenRepository = guestTokenRepository;
    }

    @PostMapping("/promociones/aplicar")
    public String aplicarCupon(@RequestParam("codigo") String codigo, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        String email = auth.getName();
        try {
            Cupon cupon = promocionService.validarYObtenerCupon(codigo);
            if (cupon.getTipo() == TipoCupon.ACCESO_TEMPORAL) {
                promocionService.aplicarAccesoTemporal(cupon, email);
                String msg = "¡Acceso temporal concedido! " + cupon.getDiasAcceso() + " días de acceso activados.";
                return "redirect:/mis-cursos?cupon_exito=" + enc(msg);
            } else {
                session.setAttribute("cuponDescuentoActivo", cupon);
                String msg = "¡Cupón " + cupon.getCodigo() + " (" + cupon.getDescuentoPorcentaje() + "%) aplicado!";
                return "redirect:/catalogo?cupon_exito=" + enc(msg);
            }
        } catch (IllegalArgumentException e) {
            return "redirect:/catalogo?cupon_error=" + enc(e.getMessage());
        } catch (Exception e) {
            return "redirect:/catalogo?cupon_error=" + enc("Error al aplicar el cupón.");
        }
    }

    @GetMapping("/canjear-token")
    public String showCanjearTokenPublic(
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request,
            Model model) {

        if (token != null && !token.trim().isEmpty()) {
            return procesarCanjeDeTokenAnonimo(token.trim(), request, model);
        }
        return "canjear-token";
    }

    @PostMapping("/canjear-token")
    public String canjearTokenPublic(
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model) {
        return procesarCanjeDeTokenAnonimo(token, request, model);
    }

    @GetMapping("/tokens/canjear")
    public String redirectTokensCanjear() {
        return "redirect:/canjear-token";
    }

    @PostMapping("/tokens/canjear")
    public String redirectTokensCanjearPost(@RequestParam("token") String token) {
        return "redirect:/canjear-token?token=" + enc(token);
    }

    @PostMapping("/paquetes/comprar/{id}")
    public String comprarPaquete(@PathVariable("id") Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        String email = auth.getName();
        try {
            promocionService.comprarPaquete(id, email);
            return "redirect:/mis-cursos?paquete_exito=1";
        } catch (Exception e) {
            return "redirect:/catalogo?paquete_error=" + enc(e.getMessage());
        }
    }

    private String procesarCanjeDeTokenAnonimo(String tokenStr, HttpServletRequest request, Model model) {
        String tokenLimpio = tokenStr.toUpperCase().trim();
        try {
            GuestToken guestToken = guestTokenRepository.findByTokenAndActivoTrue(tokenLimpio)
                    .orElse(null);

            String email;
            if (guestToken != null) {
                email = "guest_" + tokenLimpio.toLowerCase() + "@educore.com";

                if (usuarioRepository.findByEmail(email).isEmpty()) {
                    Usuario guestUser = Usuario.builder()
                            .nombre("Invitado " + tokenLimpio)
                            .email(email)
                            .password(passwordEncoder.encode("guest_pass_dummy"))
                            .role(Role.GUEST)
                            .activo(true)
                            .build();
                    usuarioRepository.save(guestUser);
                }

                promocionService.canjearGuestToken(tokenLimpio, email);
            } else {
                GuestToken tokenCanjeado = guestTokenRepository.findAll().stream()
                        .filter(t -> t.getToken().equalsIgnoreCase(tokenLimpio))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Token inválido, inexistente o ya canjeado."));

                if (tokenCanjeado.getUsuarioId() == null) {
                    throw new IllegalArgumentException("El token ingresado no es válido.");
                }

                Usuario guestUser = usuarioRepository.findById(tokenCanjeado.getUsuarioId())
                        .orElseThrow(() -> new IllegalArgumentException("Usuario invitado no encontrado."));

                email = guestUser.getEmail();

                // Verificar si las inscripciones temporales de este usuario siguen activas
                boolean tieneActivas = !inscripcionRepository.findByStudentIdAndFechaFinAfter(
                        guestUser.getId(), LocalDateTime.now()).isEmpty();

                if (!tieneActivas) {
                    throw new IllegalArgumentException("El período de acceso de este token ha expirado.");
                }
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authToken);
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            return "redirect:/mis-cursos?token_exito=1";

        } catch (IllegalArgumentException e) {
            return "redirect:/canjear-token?error=" + enc(e.getMessage());
        } catch (Exception e) {
            return "redirect:/canjear-token?error=" + enc("Error al procesar el canje de invitación.");
        }
    }

    private String enc(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
    }
}
