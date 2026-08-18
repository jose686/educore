package com.educore.platform.media.service;

import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.model.MediaType;
import com.educore.platform.media.model.CategoriaMedia;
import com.educore.platform.media.repository.MediaFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del servicio MediaService que administra los archivos subidos.
 */
@Service
public class MediaServiceImpl implements MediaService {

    private final Path fileStorageLocation;
    private final MediaFileRepository mediaFileRepository;
    private final VideoConversionService videoConversionService;

    public MediaServiceImpl(
            @Value("${file.upload-dir:./uploads}") String uploadDir,
            MediaFileRepository mediaFileRepository,
            VideoConversionService videoConversionService) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.mediaFileRepository = mediaFileRepository;
        this.videoConversionService = videoConversionService;

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo crear el directorio donde se guardarán los archivos subidos.", ex);
        }
    }

    @Override
    @Transactional
    public String uploadFile(MultipartFile file, String alias, CategoriaMedia categoria) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (originalFilename.contains("..")) {
                throw new IllegalArgumentException("El nombre de archivo contiene secuencias de ruta no válidas: " + originalFilename);
            }

            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }

            MediaType tipo = MediaType.IMAGEN;
            CategoriaMedia catFinal = CategoriaMedia.GENERAL;
            String lowerFilename = originalFilename.toLowerCase();
            String contentType = file.getContentType();

            if (lowerFilename.endsWith(".html") || lowerFilename.endsWith(".htm") || (contentType != null && contentType.equals("text/html"))) {
                tipo = MediaType.HTML_INTERACTIVO;
                catFinal = categoria != null ? categoria : CategoriaMedia.MINIJUEGO;
                if (catFinal != CategoriaMedia.MINIJUEGO && catFinal != CategoriaMedia.RECURSO_CURSO) {
                    catFinal = CategoriaMedia.MINIJUEGO;
                }
            } else if (lowerFilename.endsWith(".pdf") || (contentType != null && contentType.equals("application/pdf"))) {
                tipo = MediaType.DOCUMENTO_PDF;
                catFinal = CategoriaMedia.GENERAL;
            } else {
                catFinal = CategoriaMedia.GENERAL;
                if (contentType != null && contentType.startsWith("image/")) {
                    tipo = MediaType.IMAGEN;
                } else if ((contentType != null && contentType.startsWith("video/")) || lowerFilename.endsWith(".mp4") || lowerFilename.endsWith(".mov") || lowerFilename.endsWith(".avi") || lowerFilename.endsWith(".webm")) {
                    tipo = MediaType.VIDEO;
                } else {
                    tipo = detectMediaType(originalFilename);
                }
            }

            String uuid = UUID.randomUUID().toString();
            String generatedFilename;

            if (tipo == MediaType.VIDEO) {
                generatedFilename = uuid + ".m3u8";
                Path tempInputPath = this.fileStorageLocation.resolve("temp_" + uuid + extension);
                Files.copy(file.getInputStream(), tempInputPath, StandardCopyOption.REPLACE_EXISTING);
                videoConversionService.convertMp4ToHls(tempInputPath, this.fileStorageLocation.resolve(generatedFilename));
            } else {
                generatedFilename = uuid + extension;
                Path targetLocation = this.fileStorageLocation.resolve(generatedFilename);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String cleanAlias = alias != null ? alias.trim() : null;
            if (cleanAlias != null && cleanAlias.isEmpty()) {
                cleanAlias = null;
            }
            if (cleanAlias != null) {
                final String finalAlias = cleanAlias;
                mediaFileRepository.findByAlias(finalAlias).ifPresent(existing -> {
                    throw new IllegalArgumentException("El alias '" + finalAlias + "' ya está registrado en otro archivo.");
                });
            }

            MediaFile mediaFile = MediaFile.builder()
                    .filename(generatedFilename)
                    .url("/media/" + generatedFilename)
                    .uploadedAt(LocalDateTime.now())
                    .tipo(tipo)
                    .nombreOriginal(originalFilename)
                    .alias(cleanAlias)
                    .categoriaMedia(catFinal)
                    .build();
            mediaFileRepository.save(mediaFile);

            return "/media/" + generatedFilename;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo almacenar el archivo: " + originalFilename, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Acceso no autorizado al archivo solicitado: " + filename);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Archivo no encontrado o no legible: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Archivo no encontrado: " + filename, ex);
        }
    }

    @Override
    public List<MediaFile> listAllFiles() {
        return mediaFileRepository.findAllByOrderByUploadedAtDesc();
    }

    @Override
    public List<MediaFile> listFilesByTipo(MediaType tipo) {
        return mediaFileRepository.findByTipoOrderByUploadedAtDesc(tipo);
    }

    @Override
    public List<MediaFile> listFilesByCategoria(CategoriaMedia categoria) {
        return mediaFileRepository.findByCategoriaMediaOrderByUploadedAtDesc(categoria);
    }

    @Override
    public List<MediaFile> searchFiles(MediaType tipo, CategoriaMedia categoria, String search) {
        return mediaFileRepository.searchFiles(tipo, categoria, search);
    }

    @Override
    @Transactional
    public void deleteFile(String filename) {
        MediaFile mediaFile = mediaFileRepository.findByFilename(filename)
                .orElseThrow(() -> new IllegalArgumentException("No existe registro del archivo solicitado: " + filename));

        Path filePath = this.fileStorageLocation.resolve(mediaFile.getFilename()).normalize();
        try {
            Files.deleteIfExists(filePath);
            if (mediaFile.getTipo() == MediaType.VIDEO) {
                String baseName = mediaFile.getFilename().replace(".m3u8", "");
                Path tempInput = this.fileStorageLocation.resolve("temp_" + baseName + ".mp4");
                Files.deleteIfExists(tempInput);
            }
        } catch (IOException e) {
            System.err.println("[MediaService] Advertencia al borrar físicamente: " + e.getMessage());
        }

        mediaFileRepository.delete(mediaFile);
    }

    @Override
    @Transactional
    public void syncDatabaseWithStorage() {
        try (java.util.stream.Stream<Path> paths = Files.list(this.fileStorageLocation)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String filename = path.getFileName().toString();
                if (filename.startsWith("temp_")) {
                    return;
                }

                if (mediaFileRepository.findByFilename(filename).isEmpty()) {
                    MediaType tipo = detectMediaType(filename);
                    MediaFile mediaFile = MediaFile.builder()
                            .filename(filename)
                            .url("/media/" + filename)
                            .uploadedAt(LocalDateTime.now())
                            .tipo(tipo)
                            .nombreOriginal(filename)
                            .build();
                    mediaFileRepository.save(mediaFile);
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo sincronizar el almacenamiento con la base de datos.", ex);
        }
    }

    @Override
    @Transactional
    public void updateAlias(String filename, String alias) {
        MediaFile mediaFile = mediaFileRepository.findByFilename(filename)
                .orElseThrow(() -> new IllegalArgumentException("Archivo no encontrado: " + filename));

        String cleanAlias = alias != null ? alias.trim() : null;
        if (cleanAlias != null && cleanAlias.isEmpty()) {
            cleanAlias = null;
        }

        if (cleanAlias != null) {
            mediaFileRepository.findByAlias(cleanAlias)
                    .filter(existing -> !existing.getId().equals(mediaFile.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("El alias '" + alias + "' ya está registrado en otro archivo.");
                    });
        }

        mediaFile.setAlias(cleanAlias);
        mediaFileRepository.save(mediaFile);
    }

    @Override
    public String resolveUrlByAliasOrPath(String aliasOrPath) {
        if (aliasOrPath == null || aliasOrPath.isBlank()) {
            return aliasOrPath;
        }
        return mediaFileRepository.findByAlias(aliasOrPath)
                .map(MediaFile::getUrl)
                .orElse(aliasOrPath);
    }

    private MediaType detectMediaType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return MediaType.HTML_INTERACTIVO;
        } else if (lower.endsWith(".pdf")) {
            return MediaType.DOCUMENTO_PDF;
        } else if (lower.endsWith(".mp4") || lower.endsWith(".mpeg") || lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".webm") || lower.endsWith(".m3u8")) {
            return MediaType.VIDEO;
        } else {
            return MediaType.IMAGEN;
        }
    }
}
