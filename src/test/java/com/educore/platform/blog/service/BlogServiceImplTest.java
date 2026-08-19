package com.educore.platform.blog.service;

import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.repository.ArticuloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para BlogServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock
    private ArticuloRepository articuloRepository;

    @InjectMocks
    private BlogServiceImpl blogService;

    private Articulo articulo;
    private UUID id;
    private String slug;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        slug = "articulo-de-prueba";
        articulo = Articulo.builder()
                .id(id)
                .titulo("Artículo de prueba")
                .resumenCorto("Resumen corto")
                .contenido("<p>Contenido</p>")
                .slug(slug)
                .fechaPublicacion(LocalDateTime.now())
                .usuarioId(1L)
                .build();
    }

    @Test
    void obtenerTodosLosArticulos_ShouldReturnListOrdered() {
        when(articuloRepository.findAllByOrderByFechaPublicacionDesc())
                .thenReturn(List.of(articulo));

        List<Articulo> result = blogService.obtenerTodosLosArticulos();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(articulo, result.get(0));
        verify(articuloRepository, times(1)).findAllByOrderByFechaPublicacionDesc();
    }

    @Test
    void obtenerPorSlug_WhenExists_ShouldReturnArticle() {
        when(articuloRepository.findBySlug(slug)).thenReturn(Optional.of(articulo));

        Articulo result = blogService.obtenerPorSlug(slug);

        assertNotNull(result);
        assertEquals(articulo, result);
        verify(articuloRepository, times(1)).findBySlug(slug);
    }

    @Test
    void obtenerPorSlug_WhenNotExists_ShouldThrowException() {
        when(articuloRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> blogService.obtenerPorSlug(slug));
        verify(articuloRepository, times(1)).findBySlug(slug);
    }

    @Test
    void guardarArticulo_ShouldSaveAndReturnSaved() {
        when(articuloRepository.save(any(Articulo.class))).thenReturn(articulo);

        Articulo result = blogService.guardarArticulo(articulo);

        assertNotNull(result);
        assertEquals(articulo, result);
        verify(articuloRepository, times(1)).save(articulo);
    }

    @Test
    void obtenerPorId_WhenExists_ShouldReturnArticle() {
        when(articuloRepository.findById(id)).thenReturn(Optional.of(articulo));

        Articulo result = blogService.obtenerPorId(id);

        assertNotNull(result);
        assertEquals(articulo, result);
        verify(articuloRepository, times(1)).findById(id);
    }

    @Test
    void obtenerPorId_WhenNotExists_ShouldThrowException() {
        when(articuloRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> blogService.obtenerPorId(id));
        verify(articuloRepository, times(1)).findById(id);
    }

    @Test
    void eliminarArticulo_ShouldCallRepositoryDelete() {
        doNothing().when(articuloRepository).deleteById(id);

        blogService.eliminarArticulo(id);

        verify(articuloRepository, times(1)).deleteById(id);
    }
}
