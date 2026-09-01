package com.educore.platform.admin.controller;

import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.service.BlogService;
import com.educore.platform.lms.service.LmsService;
import com.educore.platform.media.service.MediaService;
import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.model.Pedido;
import com.educore.platform.store.service.CatalogoService;
import com.educore.platform.store.service.PedidoService;
import com.educore.platform.store.service.PromocionService;
import com.educore.platform.users.model.Role;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.model.TicketSoporte;
import com.educore.platform.users.service.UsuarioService;
import com.educore.platform.users.repository.TicketSoporteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

/**
 * Controlador de Backoffice unificado para la gestión administrativa de la
 * plataforma.
 * Protegido estrictamente con el rol ADMIN.
 */
@Controller
public class AdminController {

    private final BlogService blogService;
    private final CatalogoService catalogoService;
    private final MediaService mediaService;
    private final UsuarioService usuarioService;
    private final PromocionService promocionService;
    private final LmsService lmsService;
    private final PedidoService pedidoService;
    private final TicketSoporteRepository ticketSoporteRepository;

    public AdminController(BlogService blogService,
            CatalogoService catalogoService,
            MediaService mediaService,
            UsuarioService usuarioService,
            PromocionService promocionService,
            LmsService lmsService,
            PedidoService pedidoService,
            TicketSoporteRepository ticketSoporteRepository) {
        this.blogService = blogService;
        this.catalogoService = catalogoService;
        this.mediaService = mediaService;
        this.usuarioService = usuarioService;
        this.promocionService = promocionService;
        this.lmsService = lmsService;
        this.pedidoService = pedidoService;
        this.ticketSoporteRepository = ticketSoporteRepository;
    }

    /**
     * Muestra la pantalla principal del panel de administración.
     */
    @GetMapping("/admin")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    // ==========================================
    // SECCIÓN DE MEDIOS / IMÁGENES
    // ==========================================

    /**
     * Listado visual de imágenes del servidor.
     */
    @GetMapping("/admin/media")
    public String listMedia(@RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "categoria", required = false) com.educore.platform.media.model.CategoriaMedia categoria,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "selectMode", required = false, defaultValue = "false") boolean selectMode,
            @RequestParam(value = "hideFilters", required = false, defaultValue = "false") boolean hideFilters,
            Model model) {
        java.util.List<com.educore.platform.media.model.MediaFile> files;
        boolean actualHideFilters = hideFilters;

        if (tipo != null && tipo.contains(",")) {
            actualHideFilters = true;
            String[] parts = tipo.split(",");
            java.util.List<com.educore.platform.media.model.MediaType> mediaTypes = new java.util.ArrayList<>();
            for (String p : parts) {
                try {
                    mediaTypes.add(com.educore.platform.media.model.MediaType.valueOf(p.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Ignorar tipos inválidos
                }
            }
            files = mediaService.searchFiles(null, categoria, search);
            files = files.stream()
                    .filter(f -> mediaTypes.contains(f.getTipo()))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            com.educore.platform.media.model.MediaType mediaType = null;
            if (tipo != null && !tipo.isBlank()) {
                try {
                    mediaType = com.educore.platform.media.model.MediaType.valueOf(tipo.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Ignorar tipos inválidos
                }
            }
            files = mediaService.searchFiles(mediaType, categoria, search);
        }

        model.addAttribute("files", files);
        model.addAttribute("currentTipo", tipo);
        model.addAttribute("currentCategoria", categoria);
        model.addAttribute("search", search);
        model.addAttribute("selectMode", selectMode);
        model.addAttribute("hideFilters", actualHideFilters);
        return "admin-media";
    }

    @PostMapping("/admin/media/upload")
    public String uploadMediaFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "alias", required = false) String alias,
            @RequestParam(value = "categoriaMedia", required = false) com.educore.platform.media.model.CategoriaMedia categoriaMedia) {
        if (file.isEmpty()) {
            return "redirect:/admin/media?error=empty";
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        boolean isValid = false;
        if (contentType != null) {
            if (contentType.startsWith("image/") ||
                    contentType.equals("video/mp4") ||
                    contentType.equals("text/html") ||
                    contentType.equals("application/pdf")) {
                isValid = true;
            }
        }
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".mp4") ||
                    lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".pdf")) {
                isValid = true;
            }
        }

        if (!isValid) {
            return "redirect:/admin/media?error=invalid_type";
        }

        try {
            String relativeUrl = mediaService.uploadFile(file, alias, categoriaMedia);
            return "redirect:/admin/media?uploaded_url=" + relativeUrl;
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/media?error=alias_exists";
        }
    }

    @PostMapping("/admin/media/alias/{filename:.+}")
    public String updateMediaAlias(
            @PathVariable("filename") String filename,
            @RequestParam("alias") String alias,
            RedirectAttributes ra) {
        try {
            mediaService.updateAlias(filename, alias);
            ra.addAttribute("success", "alias_updated");
        } catch (IllegalArgumentException e) {
            ra.addAttribute("error", "alias_exists");
        }
        return "redirect:/admin/media";
    }

    /**
     * Elimina un archivo de la biblioteca de medios.
     * Recibe el nombre del archivo y lo elimina del almacenamiento y la base de
     * datos de manera transaccional.
     */
    @DeleteMapping("/admin/media/{filename:.+}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteMediaFile(@PathVariable("filename") String filename) {
        try {
            mediaService.deleteFile(filename);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("success", true));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==========================================
    // GESTIÓN DEL BLOG (CRUD)
    // ==========================================

    /**
     * Muestra el listado de todos los artículos del blog.
     */
    @GetMapping("/admin/blog/listado")
    public String listArticles(Model model) {
        List<Articulo> articulos = blogService.obtenerTodosLosArticulos();
        model.addAttribute("articulos", articulos);

        // Cargar nombres de autores (Usuarios)
        java.util.Map<Long, String> autoresMap = new java.util.HashMap<>();
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
        return "admin-blog-listado";
    }

    /**
     * Muestra el formulario para redactar un nuevo artículo.
     */
    @GetMapping("/admin/blog/nuevo")
    public String showNewArticleForm(Model model) {
        model.addAttribute("articulo", new Articulo());
        model.addAttribute("isEdit", false);
        return "admin-form-articulo";
    }

    /**
     * Procesa el guardado de un nuevo artículo de blog.
     */
    @PostMapping("/admin/blog/nuevo")
    public String saveNewArticle(@ModelAttribute("articulo") Articulo articulo, java.security.Principal principal) {
        if (articulo.getResumenCorto() != null && articulo.getResumenCorto().length() > 300) {
            articulo.setResumenCorto(articulo.getResumenCorto().substring(0, 300));
        }
        articulo.setFechaPublicacion(LocalDateTime.now());
        Long authorId = 1L; // Fallback
        if (principal != null) {
            Usuario usuario = usuarioService.obtenerPorEmail(principal.getName());
            if (usuario != null) {
                authorId = usuario.getId();
            }
        }
        articulo.setUsuarioId(authorId);
        blogService.guardarArticulo(articulo);
        return "redirect:/admin/blog/listado?success=create";
    }

    /**
     * Muestra el formulario para editar un artículo de blog existente.
     */
    @GetMapping("/admin/blog/editar/{id}")
    public String showEditArticleForm(@PathVariable("id") UUID id, Model model) {
        Articulo articulo = blogService.obtenerPorId(id);
        model.addAttribute("articulo", articulo);
        model.addAttribute("isEdit", true);
        return "admin-form-articulo";
    }

    /**
     * Procesa la modificación de un artículo de blog existente.
     */
    @PostMapping("/admin/blog/editar/{id}")
    public String updateArticle(@PathVariable("id") UUID id, @ModelAttribute("articulo") Articulo formArticulo) {
        Articulo dbArticulo = blogService.obtenerPorId(id);
        dbArticulo.setTitulo(formArticulo.getTitulo());
        dbArticulo.setSlug(formArticulo.getSlug());
        String resumen = formArticulo.getResumenCorto();
        if (resumen != null && resumen.length() > 300) {
            resumen = resumen.substring(0, 300);
        }
        dbArticulo.setResumenCorto(resumen);
        dbArticulo.setContenido(formArticulo.getContenido());
        dbArticulo.setUsuarioId(formArticulo.getUsuarioId());
        dbArticulo.setFeaturedImageUrl(formArticulo.getFeaturedImageUrl());

        blogService.guardarArticulo(dbArticulo);
        return "redirect:/admin/blog/listado?success=update";
    }

    /**
     * Elimina un artículo de blog.
     */
    @PostMapping("/admin/blog/eliminar/{id}")
    public String deleteArticle(@PathVariable("id") UUID id) {
        blogService.eliminarArticulo(id);
        return "redirect:/admin/blog/listado?success=delete";
    }

    // ==========================================
    // GESTIÓN DEL CATÁLOGO DE CURSOS (CRUD)
    // ==========================================

    /**
     * Muestra el listado de todos los cursos catalogados.
     */
    @GetMapping("/admin/cursos/listado")
    public String listCourses(Model model) {
        model.addAttribute("cursos", catalogoService.obtenerTodos());
        return "admin-cursos-listado";
    }

    /**
     * Muestra el formulario para registrar un nuevo curso.
     */
    @GetMapping("/admin/cursos/nuevo")
    public String showNewCourseForm(Model model) {
        model.addAttribute("producto", new ProductoCurso());
        model.addAttribute("isEdit", false);
        return "admin-form-curso";
    }

    /**
     * Procesa la inserción del nuevo producto del catálogo.
     */
    @PostMapping("/admin/cursos/nuevo")
    public String saveNewCourse(@ModelAttribute("producto") ProductoCurso producto) {
        if (producto.getEstado() == null || producto.getEstado().isBlank()) {
            producto.setEstado("DRAFT");
        }
        catalogoService.guardarProducto(producto);
        syncLmsCursoData(producto);
        return "redirect:/admin/cursos/listado?success=create";
    }

    /**
     * Muestra el formulario para editar un curso existente.
     */
    @GetMapping("/admin/cursos/editar/{id}")
    public String showEditCourseForm(@PathVariable("id") UUID id, Model model) {
        ProductoCurso producto = catalogoService.obtenerPorId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("isEdit", true);
        return "admin-form-curso";
    }

    /**
     * Procesa la modificación de un curso existente.
     */
    @PostMapping("/admin/cursos/editar/{id}")
    public String updateCourse(@PathVariable("id") UUID id, @ModelAttribute("producto") ProductoCurso formProducto) {
        ProductoCurso dbProducto = catalogoService.obtenerPorId(id);
        dbProducto.setTitulo(formProducto.getTitulo());
        dbProducto.setDescripcionCorta(formProducto.getDescripcionCorta());
        dbProducto.setPrecio(formProducto.getPrecio());
        dbProducto.setImagenPortadaUrl(formProducto.getImagenPortadaUrl());
        dbProducto.setLmsCursoId(formProducto.getLmsCursoId());
        if (formProducto.getEstado() != null && !formProducto.getEstado().isBlank()) {
            dbProducto.setEstado(formProducto.getEstado());
        }

        catalogoService.guardarProducto(dbProducto);
        syncLmsCursoData(dbProducto);
        return "redirect:/admin/cursos/listado?success=update";
    }

    private void syncLmsCursoData(ProductoCurso producto) {
        if (producto.getLmsCursoId() != null) {
            try {
                com.educore.platform.lms.model.Curso lmsCurso;
                try {
                    lmsCurso = lmsService.obtenerCursoPorId(producto.getLmsCursoId());
                } catch (Exception e) {
                    lmsCurso = com.educore.platform.lms.model.Curso.builder()
                            .id(producto.getLmsCursoId())
                            .teacherId(1L)
                            .build();
                }
                lmsCurso.setTitulo(producto.getTitulo());
                lmsCurso.setDescripcion(producto.getDescripcionCorta());
                lmsCurso.setImagenUrl(producto.getImagenPortadaUrl());
                if (producto.getPrecio() != null) {
                    lmsCurso.setPrecio(producto.getPrecio().doubleValue());
                }
                lmsService.guardarCurso(lmsCurso);
            } catch (Exception e) {
                // Ignorar error puntual de sync en LMS
            }
        }
    }

    /**
     * Elimina un curso del catálogo.
     */
    @PostMapping("/admin/cursos/eliminar/{id}")
    public String deleteCourse(@PathVariable("id") UUID id) {
        catalogoService.eliminarProducto(id);
        return "redirect:/admin/cursos/listado?success=delete";
    }

    // ==========================================
    // SECCIÓN DE PROMOCIONES Y CUPONES
    // ==========================================

    /**
     * Muestra la vista del gestor de promociones con cupones, tokens, packs y
     * promociones de curso.
     */
    @GetMapping("/admin/promociones")
    public String listPromociones(Model model) {
        model.addAttribute("cupones", promocionService.obtenerTodosLosCupones());
        model.addAttribute("tokens", promocionService.obtenerTodosLosTokens());
        model.addAttribute("paquetes", promocionService.obtenerTodosLosPaquetes());
        model.addAttribute("promocionesCurso", promocionService.obtenerPromocionesCurso());
        model.addAttribute("cursos", lmsService.obtenerTodosLosCursos());
        model.addAttribute("activeSection", "promociones");
        return "admin-promociones";
    }

    /**
     * Crea un nuevo cupón de descuento o acceso temporal.
     */
    @PostMapping("/admin/promociones/nuevo")
    public String savePromocion(@RequestParam("codigo") String codigo,
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "descuentoPorcentaje", required = false) Integer descuentoPorcentaje,
            @RequestParam(value = "diasAcceso", required = false) Integer diasAcceso,
            @RequestParam(value = "cursoId", required = false) Long cursoId) {
        promocionService.crearCupon(codigo, tipo, descuentoPorcentaje, diasAcceso, cursoId);
        return "redirect:/admin/promociones?tab=cupones&success=create";
    }

    /**
     * Elimina un cupón existente.
     */
    @PostMapping("/admin/promociones/eliminar/{id}")
    public String deletePromocion(@PathVariable("id") Long id) {
        promocionService.eliminarCupon(id);
        return "redirect:/admin/promociones?tab=cupones&success=delete";
    }

    // ==========================================
    // TOKENS DE INVITADO
    // ==========================================

    /**
     * Genera un nuevo token de acceso para invitados con cursos y duración
     * configurados.
     */
    @PostMapping("/admin/tokens/nuevo")
    public String crearToken(@RequestParam("cursoIds") String cursoIds,
            @RequestParam("diasAcceso") Integer diasAcceso) {
        java.util.Set<Long> setIds = new java.util.HashSet<>();
        if (cursoIds != null && !cursoIds.isBlank()) {
            for (String s : cursoIds.split(",")) {
                if (!s.trim().isEmpty()) {
                    setIds.add(Long.parseLong(s.trim()));
                }
            }
        }
        promocionService.crearGuestToken(setIds, diasAcceso);
        return "redirect:/admin/promociones?tab=tokens&success=create";
    }

    /**
     * Elimina un token de invitado existente.
     */
    @PostMapping("/admin/tokens/eliminar/{id}")
    public String deleteToken(@PathVariable("id") Long id) {
        promocionService.eliminarGuestToken(id);
        return "redirect:/admin/promociones?tab=tokens&success=delete";
    }

    // ==========================================
    // PAQUETES / BUNDLES
    // ==========================================

    /**
     * Crea un nuevo paquete (bundle) de cursos.
     */
    @PostMapping("/admin/paquetes/nuevo")
    public String crearPaquete(@RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("precio") java.math.BigDecimal precio,
            @RequestParam("cursoIds") String cursoIds) {
        java.util.Set<Long> setIds = new java.util.HashSet<>();
        if (cursoIds != null && !cursoIds.isBlank()) {
            for (String s : cursoIds.split(",")) {
                if (!s.trim().isEmpty()) {
                    setIds.add(Long.parseLong(s.trim()));
                }
            }
        }
        promocionService.crearPaquete(titulo, descripcion, precio, setIds);
        return "redirect:/admin/promociones?tab=paquetes&success=create";
    }

    /**
     * Elimina un paquete existente.
     */
    @PostMapping("/admin/paquetes/eliminar/{id}")
    public String deletePaquete(@PathVariable("id") Long id) {
        promocionService.eliminarPaquete(id);
        return "redirect:/admin/promociones?tab=paquetes&success=delete";
    }

    // ==========================================
    // PROMOCIONES AUTOMÁTICAS DE CURSO
    // ==========================================

    /**
     * Crea una nueva promoción automática vinculada a un curso específico.
     */
    @PostMapping("/admin/promociones-curso/nuevo")
    public String crearPromocionCurso(@RequestParam("cursoId") Long cursoId,
            @RequestParam("tipo") String tipo,
            @RequestParam("porcentajeDescuento") Integer porcentajeDescuento,
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin) {
        java.time.LocalDateTime inicio = java.time.LocalDateTime.parse(fechaInicio + "T00:00:00");
        java.time.LocalDateTime fin = java.time.LocalDateTime.parse(fechaFin + "T23:59:59");
        promocionService.crearPromocionCurso(cursoId, tipo, porcentajeDescuento, inicio, fin);
        return "redirect:/admin/promociones?tab=descuentos&success=create";
    }

    /**
     * Elimina una promoción automática de curso.
     */
    @PostMapping("/admin/promociones-curso/eliminar/{id}")
    public String deletePromocionCurso(@PathVariable("id") Long id) {
        promocionService.eliminarPromocionCurso(id);
        return "redirect:/admin/promociones?tab=descuentos&success=delete";
    }

    // ==========================================
    // GESTIÓN DE PEDIDOS Y REEMBOLSOS
    // ==========================================

    /**
     * Muestra todos los pedidos del sistema para el backoffice con filtros.
     */
    @GetMapping("/admin/pedidos")
    public String listPedidos(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "pedidoId", required = false) Long pedidoId,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "fechaInicio", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin,
            Model model) {

        List<Pedido> pedidos = pedidoService.obtenerTodosLosPedidosConDetalles();

        if (email != null && !email.trim().isEmpty()) {
            String lowerEmail = email.toLowerCase().trim();
            pedidos = pedidos.stream()
                    .filter(p -> p.getEmailUsuario() != null && p.getEmailUsuario().toLowerCase().contains(lowerEmail))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (pedidoId != null) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getId().equals(pedidoId))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (estado != null && !estado.trim().isEmpty() && !"TODOS".equalsIgnoreCase(estado)) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getEstado() != null && p.getEstado().name().equalsIgnoreCase(estado.trim()))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (fechaInicio != null) {
            java.time.LocalDateTime start = fechaInicio.atStartOfDay();
            pedidos = pedidos.stream()
                    .filter(p -> p.getFechaCompra() != null && !p.getFechaCompra().isBefore(start))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (fechaFin != null) {
            java.time.LocalDateTime end = fechaFin.atTime(23, 59, 59);
            pedidos = pedidos.stream()
                    .filter(p -> p.getFechaCompra() != null && !p.getFechaCompra().isAfter(end))
                    .collect(java.util.stream.Collectors.toList());
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("emailFilter", email);
        model.addAttribute("pedidoIdFilter", pedidoId);
        model.addAttribute("estadoFilter", estado);
        model.addAttribute("fechaInicioFilter", fechaInicio);
        model.addAttribute("fechaFinFilter", fechaFin);

        return "admin-pedidos";
    }

    /**
     * Muestra el detalle completo de un pedido individual.
     */
    @GetMapping("/admin/pedidos/{id}")
    public String verDetallePedido(@PathVariable("id") Long id, Model model) {
        Pedido pedido = pedidoService.obtenerPedidoPorId(id);
        model.addAttribute("pedido", pedido);
        return "admin-pedido-detalle";
    }

    /**
     * Fuerza la matriculación manual y limpia/resuelve la incidencia asociada.
     */
    @PostMapping("/admin/pedidos/{id}/matricular-manual")
    public String matricularManualPedido(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        try {
            pedidoService.forzarMatriculacionManual(id);

            // Resolving support tickets associated with this pedidoId
            List<TicketSoporte> tickets = ticketSoporteRepository.findByPedidoId(id);
            for (TicketSoporte ticket : tickets) {
                ticket.setEstado("RESUELTO");
                ticketSoporteRepository.save(ticket);
            }

            redirectAttrs.addFlashAttribute("successMsg",
                    "Matriculación forzada manualmente y ticket de soporte resuelto para el pedido #" + id + ".");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg",
                    "Error al forzar la matriculación manual: " + e.getMessage());
        }
        return "redirect:/admin/pedidos/" + id;
    }

    /**
     * Procesa un reembolso de pedido desde el backoffice.
     * Ejecuta la devolución en Stripe, cambia el estado del pedido a REEMBOLSADO
     * y revoca automáticamente el acceso del alumno al Aula Virtual.
     */
    @PostMapping("/admin/pedidos/{id}/reembolsar")
    public String reembolsarPedido(@PathVariable("id") Long id, RedirectAttributes redirectAttrs) {
        try {
            pedidoService.reembolsarPedido(id);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Reembolso ejecutado correctamente para el pedido #" + id + ".");
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg",
                    "Error al procesar el reembolso con Stripe: " + e.getMessage());
        }
        return "redirect:/admin/pedidos/" + id;
    }
}
