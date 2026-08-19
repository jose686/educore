package com.educore.platform.store.service;

import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.repository.ProductoCursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CatalogoServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock
    private ProductoCursoRepository productoCursoRepository;

    @InjectMocks
    private CatalogoServiceImpl catalogoService;

    private ProductoCurso producto;
    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        producto = ProductoCurso.builder()
                .id(id)
                .titulo("Curso de Prueba")
                .estado("PUBLISHED")
                .build();
    }

    @Test
    void obtenerCatalogoPublico_ShouldReturnPublishedProducts() {
        when(productoCursoRepository.findByEstado("PUBLISHED")).thenReturn(List.of(producto));

        List<ProductoCurso> result = catalogoService.obtenerCatalogoPublico();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(producto, result.get(0));
    }

    @Test
    void obtenerPorId_WhenExists_ShouldReturnProduct() {
        when(productoCursoRepository.findById(id)).thenReturn(Optional.of(producto));

        ProductoCurso result = catalogoService.obtenerPorId(id);

        assertNotNull(result);
        assertEquals(producto, result);
    }

    @Test
    void obtenerPorId_WhenNotExists_ShouldThrowException() {
        when(productoCursoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.obtenerPorId(id));
    }

    @Test
    void guardarProducto_ShouldSaveAndReturnProduct() {
        when(productoCursoRepository.save(producto)).thenReturn(producto);

        ProductoCurso result = catalogoService.guardarProducto(producto);

        assertNotNull(result);
        assertEquals(producto, result);
    }

    @Test
    void eliminarProducto_ShouldCallRepositoryDelete() {
        doNothing().when(productoCursoRepository).deleteById(id);

        catalogoService.eliminarProducto(id);

        verify(productoCursoRepository, times(1)).deleteById(id);
    }

    @Test
    void obtenerTodos_ShouldReturnAllProducts() {
        when(productoCursoRepository.findAll()).thenReturn(List.of(producto));

        List<ProductoCurso> result = catalogoService.obtenerTodos();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
