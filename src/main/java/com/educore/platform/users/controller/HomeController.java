package com.educore.platform.users.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para la página principal de la plataforma.
 */
@Controller
public class HomeController {

    /**
     * Muestra la página principal de bienvenida.
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
