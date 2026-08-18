package com.educore.platform.blog.service;

import com.educore.platform.blog.model.Articulo;
import com.educore.platform.blog.repository.ArticuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Implementación del servicio BlogService.
 */
@Service
@Transactional(readOnly = true)
public class BlogServiceImpl implements BlogService {

    private final ArticuloRepository articuloRepository;

    public BlogServiceImpl(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    @Override
    public List<Articulo> obtenerTodosLosArticulos() {
        return articuloRepository.findAllByOrderByFechaPublicacionDesc();
    }

    @Override
    public Articulo obtenerPorSlug(String slug) {
        return articuloRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún artículo de blog con el slug: " + slug));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Articulo guardarArticulo(Articulo articulo) {
        return articuloRepository.save(articulo);
    }

    @Override
    public Articulo obtenerPorId(java.util.UUID id) {
        return articuloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ningún artículo con ID: " + id));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarArticulo(java.util.UUID id) {
        articuloRepository.deleteById(id);
    }
}
