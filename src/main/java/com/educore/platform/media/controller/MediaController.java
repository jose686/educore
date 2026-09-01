package com.educore.platform.media.controller;

import com.educore.platform.media.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;


/**
 * Controlador REST para gestionar la carga y el consumo de recursos multimedia.
 */
@RestController
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Endpoint para subir archivos al servidor. Restringido mediante seguridad a ADMIN o TEACHER.
     *
     * @param file El archivo a subir.
     * @return URL de acceso en formato JSON.
     */
    @PostMapping("/api/media/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = mediaService.uploadFile(file);
        return ResponseEntity.ok(Map.of("url", fileUrl));
    }

    /**
     * Endpoint público para servir los archivos estáticos en el navegador.
     * Soporta peticiones de rango (HTTP 206 Partial Content) automáticamente delegando en Spring.
     *
     * @param filename Nombre único del archivo solicitado.
     * @return El recurso completo o parcial con su tipo de contenido correspondiente.
     */
    @CrossOrigin(origins = "*")
    @GetMapping("/media/**")
    public ResponseEntity<Resource> serveFile(jakarta.servlet.http.HttpServletRequest request) {
        try {
            String path = (String) request.getAttribute(org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String filename = path.substring("/media/".length());

            Resource resource = mediaService.loadFileAsResource(filename);
            if (resource.exists() || resource.isReadable()) {
                MediaType mediaType;
                String lower = filename.toLowerCase();
                if (lower.endsWith(".m3u8")) {
                    mediaType = MediaType.parseMediaType("application/x-mpegURL");
                } else if (lower.endsWith(".ts")) {
                    mediaType = MediaType.parseMediaType("video/MP2T");
                } else if (lower.endsWith(".mp4")) {
                    mediaType = MediaType.parseMediaType("video/mp4");
                } else if (lower.endsWith(".webm")) {
                    mediaType = MediaType.parseMediaType("video/webm");
                } else if (lower.endsWith(".css")) {
                    mediaType = MediaType.parseMediaType("text/css");
                } else if (lower.endsWith(".js")) {
                    mediaType = MediaType.parseMediaType("application/javascript");
                } else {
                    mediaType = org.springframework.http.MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
                }

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

}
