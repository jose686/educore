package com.educore.platform.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO para capturar los datos necesarios al crear un Módulo en el temario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuloDTO {

    @NotBlank(message = "El nombre del módulo es obligatorio")
    private String nombre;

    private Integer orden;

}
