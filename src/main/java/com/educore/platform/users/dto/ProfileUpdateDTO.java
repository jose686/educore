package com.educore.platform.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Objeto de transferencia de datos (DTO) para capturar los datos de modificación de perfil de usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    private String password;
}
