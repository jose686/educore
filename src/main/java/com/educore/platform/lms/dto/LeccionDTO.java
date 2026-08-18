package com.educore.platform.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO para capturar los datos necesarios al crear una Lección en el temario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeccionDTO {

    @NotBlank(message = "El título de la lección es obligatorio")
    private String titulo;

    @NotBlank(message = "El contenido teórico es obligatorio")
    private String contenido;

    private String videoUrl;

    private Boolean esVideoLocal = false;

    private String rutaScriptInteractivo;

    private Integer orden;

}

