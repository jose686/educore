package com.educore.platform.store.service;

import com.educore.platform.store.model.PaqueteCreditos;
import com.educore.platform.store.repository.PaqueteCreditosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Implementación de la capa de servicios para gestionar la lógica de paquetes de créditos.
 */
@Service
@Transactional
public class PaqueteCreditosServiceImpl implements PaqueteCreditosService {

    private final PaqueteCreditosRepository repository;

    public PaqueteCreditosServiceImpl(PaqueteCreditosRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaqueteCreditos> obtenerTodos() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaqueteCreditos> obtenerPaquetesActivos() {
        return repository.findByActivoTrueOrderByOrdenVisualizacionAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public PaqueteCreditos obtenerPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public PaqueteCreditos guardar(PaqueteCreditos paquete) {
        return repository.save(paquete);
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
