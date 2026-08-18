package com.educore.platform.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * DTO para la transferencia de datos y validación de Promociones / Bundles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromocionDTO {

    private UUID id;

    @NotBlank(message = "El título de la promoción es obligatorio")
    private String titulo;

    private String descripcion;

    @NotNull(message = "El precio de oferta es obligatorio")
    private BigDecimal precioOferta;

    @NotEmpty(message = "La promoción debe incluir al menos un curso")
    private Set<Long> lmsCursoIds;
}
