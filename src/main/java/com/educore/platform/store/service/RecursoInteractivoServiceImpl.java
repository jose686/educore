package com.educore.platform.store.service;

import com.educore.platform.media.model.CategoriaMedia;
import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.repository.MediaFileRepository;
import com.educore.platform.media.service.MediaService;
import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.repository.RecursoInteractivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación de RecursoInteractivoService con resolución de alias de medios.
 */
@Service
@Transactional(readOnly = true)
public class RecursoInteractivoServiceImpl implements RecursoInteractivoService {

    private final RecursoInteractivoRepository repository;
    private final MediaService mediaService;
    private final MediaFileRepository mediaFileRepository;

    public RecursoInteractivoServiceImpl(RecursoInteractivoRepository repository,
                                         MediaService mediaService,
                                         MediaFileRepository mediaFileRepository) {
        this.repository = repository;
        this.mediaService = mediaService;
        this.mediaFileRepository = mediaFileRepository;
    }

    private RecursoInteractivo resolveUrl(RecursoInteractivo r) {
        if (r != null) {
            r.setResolvedHtmlUrl(mediaService.resolveUrlByAliasOrPath(r.getHtmlUrl()));
        }
        return r;
    }

    private List<RecursoInteractivo> resolveUrls(List<RecursoInteractivo> list) {
        if (list != null) {
            list.forEach(this::resolveUrl);
        }
        return list;
    }

    private boolean perteneceACategoria(RecursoInteractivo r, CategoriaMedia categoria) {
        if (categoria == null) {
            return true;
        }
        String urlOrAlias = r.getHtmlUrl();
        if (urlOrAlias == null || urlOrAlias.isBlank()) {
            return false;
        }
        Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByAlias(urlOrAlias)
                .or(() -> mediaFileRepository.findByUrl(urlOrAlias))
                .or(() -> mediaFileRepository.findByFilename(urlOrAlias));
        
        return mediaFileOpt.map(mediaFile -> mediaFile.getCategoriaMedia() == categoria).orElse(false);
    }

    @Override
    public List<RecursoInteractivo> obtenerTodos() {
        return resolveUrls(repository.findAll());
    }

    @Override
    public List<RecursoInteractivo> obtenerActivosPorCategoriaMedia(CategoriaMedia categoria) {
        List<RecursoInteractivo> list = repository.findByActivo(true);
        if (categoria != null) {
            list = list.stream()
                    .filter(r -> perteneceACategoria(r, categoria))
                    .collect(Collectors.toList());
        }
        return resolveUrls(list);
    }

    @Override
    public List<RecursoInteractivo> obtenerBibliotecaFiltrada(CategoriaMedia categoria, String etiqueta, String search) {
        List<RecursoInteractivo> list = repository.filterBiblioteca(etiqueta, search);
        if (categoria != null) {
            list = list.stream()
                    .filter(r -> perteneceACategoria(r, categoria))
                    .collect(Collectors.toList());
        }
        return resolveUrls(list);
    }

    @Override
    public List<String> obtenerTodasEtiquetas() {
        return repository.findAllUniqueTags();
    }

    @Override
    public RecursoInteractivo obtenerPorId(Long id) {
        return resolveUrl(repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún recurso con ID: " + id)));
    }

    @Override
    public RecursoInteractivo obtenerPorIdentificador(String identificador) {
        return resolveUrl(repository.findByIdentificador(identificador).orElse(null));
    }

    @Override
    @Transactional
    public RecursoInteractivo guardar(RecursoInteractivo recurso) {
        // Autogenerar identificador (slug) si viene vacío
        if (recurso.getIdentificador() == null || recurso.getIdentificador().isBlank()) {
            if (recurso.getTitulo() != null && !recurso.getTitulo().isBlank()) {
                recurso.setIdentificador(generarSlug(recurso.getTitulo()));
            }
        }

        // Validar identificador único
        repository.findByIdentificador(recurso.getIdentificador())
                .filter(existing -> !existing.getId().equals(recurso.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("El identificador '" + recurso.getIdentificador() + "' ya está registrado en otro recurso.");
                });
        return resolveUrl(repository.save(recurso));
    }

    /**
     * Genera un slug limpio a partir de un texto: minúsculas, sin tildes, espacios → guiones bajos.
     */
    private String generarSlug(String texto) {
        String normalized = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s_-]", "")
                .trim()
                .replaceAll("[\\s-]+", "_");
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
