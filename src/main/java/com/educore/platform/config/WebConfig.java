package com.educore.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para la aplicación.
 * Registra gestores de recursos para poder servir archivos subidos físicamente al disco local.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.media.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapea la ruta lógica /uploads/** al directorio físico de archivos subidos
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
