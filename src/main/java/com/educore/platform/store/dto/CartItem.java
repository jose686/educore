package com.educore.platform.store.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO que representa un producto añadido al Carrito de Compras en la sesión del usuario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id", "tipo"})
public class CartItem {
    
    private String id;        // Identificador del producto (ID original en formato String)
    private String tipo;      // Tipo de producto ("curso", "paquete", "servicio")
    private String titulo;    // Título descriptivo
    private BigDecimal precio; // Precio final del producto al añadirlo
}
