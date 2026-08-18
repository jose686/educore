package com.educore.platform.store.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad que almacena los datos fiscales y de facturación de la empresa/plataforma.
 */
@Entity
@Table(name = "datos_fiscales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosFiscales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "cif_nif", nullable = false, length = 50)
    private String cifNif;

    @Column(name = "direccion_fiscal", nullable = false, length = 500)
    private String direccionFiscal;

    @Column(name = "email_contacto", nullable = false, length = 320)
    private String emailContacto;

    @Column(name = "telefono", nullable = false, length = 50)
    private String telefono;
}
