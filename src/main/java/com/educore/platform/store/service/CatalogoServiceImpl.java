package com.educore.platform.store.service;

import com.educore.platform.store.model.ProductoCurso;
import com.educore.platform.store.repository.ProductoCursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del servicio CatalogoService.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoCursoRepository productoCursoRepository;

    public CatalogoServiceImpl(ProductoCursoRepository productoCursoRepository) {
        this.productoCursoRepository = productoCursoRepository;
    }

    @Override
    public List<ProductoCurso> obtenerCatalogoPublico() {
        return productoCursoRepository.findByEstado("PUBLISHED");
    }

    @Override
    public ProductoCurso obtenerPorId(UUID id) {
        return productoCursoRepository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("No se encontró ningún producto de curso con ID: " + id));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public ProductoCurso guardarProducto(ProductoCurso producto) {
        return productoCursoRepository.save(producto);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarProducto(java.util.UUID id) {
        productoCursoRepository.deleteById(id);
    }

    @Override
    public List<ProductoCurso> obtenerTodos() {
        return productoCursoRepository.findAll();
    }

    @Override
    public java.util.Optional<ProductoCurso> obtenerPorLmsCursoId(Long lmsCursoId) {
        return productoCursoRepository.findByLmsCursoId(lmsCursoId);
    }
}
