package com.educore.platform.store.controller;

import com.educore.platform.store.model.DatosFiscales;
import com.educore.platform.store.repository.DatosFiscalesRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAjustesController {

    private final DatosFiscalesRepository datosFiscalesRepository;

    public AdminAjustesController(DatosFiscalesRepository datosFiscalesRepository) {
        this.datosFiscalesRepository = datosFiscalesRepository;
    }

    /**
     * Muestra el formulario para editar los datos fiscales de la empresa.
     */
    @GetMapping("/admin/ajustes")
    public String mostrarAjustes(Model model, @RequestParam(value = "exito", required = false) String exito) {
        DatosFiscales config = datosFiscalesRepository.findAll().stream().findFirst().orElseGet(() ->
                DatosFiscales.builder()
                        .razonSocial("EduCore Platform S.L.")
                        .cifNif("B-88765432")
                        .direccionFiscal("Calle de la Táctica, 42, 28001 Madrid, España")
                        .emailContacto("soporte@educore.com")
                        .telefono("+34 912 345 678")
                        .build()
        );
        model.addAttribute("config", config);
        model.addAttribute("activeSection", "ajustes");
        if (exito != null) {
            model.addAttribute("exito", "Los datos fiscales se han guardado con éxito.");
        }
        return "admin-ajustes";
    }

    /**
     * Guarda los datos fiscales actualizados.
     */
    @PostMapping("/admin/ajustes")
    public String guardarAjustes(@ModelAttribute("config") DatosFiscales nuevoConfig) {
        DatosFiscales config = datosFiscalesRepository.findAll().stream().findFirst().orElse(new DatosFiscales());
        
        config.setRazonSocial(nuevoConfig.getRazonSocial());
        config.setCifNif(nuevoConfig.getCifNif());
        config.setDireccionFiscal(nuevoConfig.getDireccionFiscal());
        config.setEmailContacto(nuevoConfig.getEmailContacto());
        config.setTelefono(nuevoConfig.getTelefono());

        datosFiscalesRepository.save(config);
        return "redirect:/admin/ajustes?exito=true";
    }
}
