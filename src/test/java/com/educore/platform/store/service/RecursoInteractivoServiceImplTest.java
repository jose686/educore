package com.educore.platform.store.service;

import com.educore.platform.media.model.CategoriaMedia;
import com.educore.platform.media.model.MediaFile;
import com.educore.platform.media.repository.MediaFileRepository;
import com.educore.platform.media.service.MediaService;
import com.educore.platform.store.model.RecursoInteractivo;
import com.educore.platform.store.repository.RecursoInteractivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para RecursoInteractivoServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class RecursoInteractivoServiceImplTest {

    @Mock private RecursoInteractivoRepository repository;
    @Mock private MediaService mediaService;
    @Mock private MediaFileRepository mediaFileRepository;

    @InjectMocks
    private RecursoInteractivoServiceImpl service;

    private RecursoInteractivo recurso;

    @BeforeEach
    void setUp() {
        recurso = RecursoInteractivo.builder()
                .id(1L)
                .identificador("slug-recurso")
                .titulo("Recurso de Ajedrez")
                .htmlUrl("alias-media")
                .activo(true)
                .build();
    }

    @Test
    void obtenerTodos_ShouldReturnListWithResolvedUrls() {
        when(repository.findAll()).thenReturn(List.of(recurso));
        when(mediaService.resolveUrlByAliasOrPath("alias-media")).thenReturn("/media/game.html");

        List<RecursoInteractivo> result = service.obtenerTodos();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("/media/game.html", result.get(0).getResolvedHtmlUrl());
    }

    @Test
    void obtenerActivosPorCategoriaMedia_ShouldFilterCorrectly() {
        RecursoInteractivo r1 = RecursoInteractivo.builder().id(1L).htmlUrl("alias-1").activo(true).build();
        RecursoInteractivo r2 = RecursoInteractivo.builder().id(2L).htmlUrl("alias-2").activo(true).build();

        MediaFile m1 = MediaFile.builder().categoriaMedia(CategoriaMedia.MINIJUEGO).build();
        MediaFile m2 = MediaFile.builder().categoriaMedia(CategoriaMedia.RECURSO_CURSO).build();

        when(repository.findByActivo(true)).thenReturn(List.of(r1, r2));
        when(mediaFileRepository.findByAlias("alias-1")).thenReturn(Optional.of(m1));
        when(mediaFileRepository.findByAlias("alias-2")).thenReturn(Optional.of(m2));

        List<RecursoInteractivo> result = service.obtenerActivosPorCategoriaMedia(CategoriaMedia.MINIJUEGO);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void obtenerBibliotecaFiltrada_ShouldReturnList() {
        when(repository.filterBiblioteca("tag", "search")).thenReturn(List.of(recurso));
        when(mediaFileRepository.findByAlias("alias-media")).thenReturn(Optional.of(MediaFile.builder().categoriaMedia(CategoriaMedia.MINIJUEGO).build()));

        List<RecursoInteractivo> result = service.obtenerBibliotecaFiltrada(CategoriaMedia.MINIJUEGO, "tag", "search");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void obtenerTodasEtiquetas_ShouldReturnList() {
        when(repository.findAllUniqueTags()).thenReturn(List.of("tag1", "tag2"));
        List<String> result = service.obtenerTodasEtiquetas();
        assertEquals(2, result.size());
    }

    @Test
    void obtenerPorId_WhenExists_ShouldReturnResolved() {
        when(repository.findById(1L)).thenReturn(Optional.of(recurso));
        when(mediaService.resolveUrlByAliasOrPath("alias-media")).thenReturn("/media/game.html");

        RecursoInteractivo result = service.obtenerPorId(1L);

        assertNotNull(result);
        assertEquals("/media/game.html", result.getResolvedHtmlUrl());
    }

    @Test
    void obtenerPorId_WhenNotFound_ShouldThrowException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.obtenerPorId(1L));
    }

    @Test
    void obtenerPorIdentificador_ShouldReturnResolved() {
        when(repository.findByIdentificador("slug")).thenReturn(Optional.of(recurso));
        RecursoInteractivo result = service.obtenerPorIdentificador("slug");
        assertNotNull(result);
    }

    @Test
    void guardar_ShouldGenerateSlugIfBlank() {
        RecursoInteractivo newRec = RecursoInteractivo.builder().titulo("Tácticas de Apertura").build();
        when(repository.findByIdentificador(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(RecursoInteractivo.class))).thenAnswer(i -> i.getArgument(0));

        RecursoInteractivo result = service.guardar(newRec);

        assertNotNull(result.getIdentificador());
        assertEquals("tacticas_de_apertura", result.getIdentificador());
    }

    @Test
    void guardar_ShouldThrowException_WhenSlugConflictExists() {
        RecursoInteractivo newRec = RecursoInteractivo.builder().id(2L).identificador("dup").build();
        RecursoInteractivo existing = RecursoInteractivo.builder().id(3L).identificador("dup").build();

        when(repository.findByIdentificador("dup")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.guardar(newRec));
    }

    @Test
    void eliminar_ShouldCallRepositoryDelete() {
        service.eliminar(1L);
        verify(repository, times(1)).deleteById(1L);
    }
}
