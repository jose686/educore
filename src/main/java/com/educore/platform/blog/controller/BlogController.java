package com.educore.platform.blog.controller;

import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.service.BlogService;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controlador público para el blog de la plataforma.
 * Permite SSR (Server-Side Rendering) para asegurar la indexación y SEO.
 */
@Controller
public class BlogController {

    private final BlogService blogService;
    private final UsuarioService usuarioService;

    public BlogController(BlogService blogService, UsuarioService usuarioService) {
        this.blogService = blogService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra la lista de todos los artículos publicados.
     */
    @GetMapping("/blog")
    public String showBlogList(Model model) {
        List<Articulo> articulos = blogService.obtenerTodosLosArticulos();
        model.addAttribute("articulos", articulos);
        
        Map<Long, String> autoresMap = new HashMap<>();
        for (Articulo art : articulos) {
            if (art.getUsuarioId() != null) {
                try {
                    Usuario u = usuarioService.obtenerPorId(art.getUsuarioId());
                    autoresMap.put(art.getUsuarioId(), u.getNombre());
                } catch (Exception e) {
                    autoresMap.put(art.getUsuarioId(), "Usuario #" + art.getUsuarioId());
                }
            }
        }
        model.addAttribute("autoresMap", autoresMap);
        return "blog";
    }

    /**
     * Muestra el detalle de un artículo específico seleccionado por su slug.
     */
    @GetMapping("/blog/{slug}")
    public String showArticleDetail(@PathVariable("slug") String slug, Model model) {
        Articulo articulo = blogService.obtenerPorSlug(slug);
        model.addAttribute("articulo", articulo);
        
        String autorNombre = "Desconocido";
        if (articulo.getUsuarioId() != null) {
            try {
                Usuario u = usuarioService.obtenerPorId(articulo.getUsuarioId());
                autorNombre = u.getNombre();
            } catch (Exception e) {
                autorNombre = "Usuario #" + articulo.getUsuarioId();
            }
        }
        model.addAttribute("autorNombre", autorNombre);
        return "articulo";
    }
}
