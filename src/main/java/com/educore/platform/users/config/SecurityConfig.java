package com.educore.platform.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Configuración de seguridad para la aplicación utilizando Spring Security 6.
 * Configura autenticación basada en sesiones tradicionales mediante cookies (JSESSIONID).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Autorización de peticiones HTTP con requestMatchers modernos
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/login"),
                    new AntPathRequestMatcher("/register"),
                    new AntPathRequestMatcher("/catalogo"),
                    new AntPathRequestMatcher("/cursos"),
                    new AntPathRequestMatcher("/juegos"),
                    new AntPathRequestMatcher("/minijuegos"),
                    new AntPathRequestMatcher("/minijuegos/**"),
                    new AntPathRequestMatcher("/blog/**"),
                    new AntPathRequestMatcher("/media/**"),
                    new AntPathRequestMatcher("/css/**"),
                    new AntPathRequestMatcher("/js/**"),
                    new AntPathRequestMatcher("/images/**"),
                    new AntPathRequestMatcher("/favicon.ico"),
                    new AntPathRequestMatcher("/uploads/**"),
                    new AntPathRequestMatcher("/interactivos/**"),
                    new AntPathRequestMatcher("/static/**"),
                    new AntPathRequestMatcher("/webjars/**"),
                    new AntPathRequestMatcher("/api/v1/stripe/webhook"),
                    new AntPathRequestMatcher("/canjear-token"),
                    new AntPathRequestMatcher("/tokens/canjear"),
                    new AntPathRequestMatcher("/invitado/**"),
                    new AntPathRequestMatcher("/contacto"),
                    new AntPathRequestMatcher("/contacto/**")
                ).permitAll()
                // Permitimos acceso a la consola de H2 para desarrollo local
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()
                // Subida de archivos multimedia
                .requestMatchers(
                    new AntPathRequestMatcher("/api/media/upload", "POST")
                ).hasAnyRole("ADMIN", "TEACHER")
                // Backoffice de Administración
                .requestMatchers(
                    new AntPathRequestMatcher("/admin/**"),
                    new AntPathRequestMatcher("/api/v1/admin/**")
                ).hasRole("ADMIN")
                // Rutas privadas del aula virtual (LMS)
                .requestMatchers(
                    new AntPathRequestMatcher("/mis-cursos"),
                    new AntPathRequestMatcher("/aula/**")
                ).hasAnyRole("STUDENT", "ADMIN", "GUEST")
                .anyRequest().authenticated()
            )
            // Configuración del login basado en formulario tradicional y Thymeleaf
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .usernameParameter("username") // En nuestro caso, mapea al email del usuario
                .passwordParameter("password")
                .permitAll()
            )
            // Recordar sesión durante 7 días
            .rememberMe(remember -> remember
                .key("ClaveSecretaEduCore2026")
                .tokenValiditySeconds(604800)
            )
            // Configuración del cierre de sesión
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Configuración de la sesión tradicional
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(2)
            )
            // Habilitación de CSRF (necesario para MVC/Thymeleaf) con excepción para la consola H2 y el webhook de Stripe
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/h2-console/**"),
                    new AntPathRequestMatcher("/api/v1/stripe/webhook")
                )
            )
            // Permitir frames de origen local (necesario para visualizar H2 e iframes de minijuegos)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }
}
