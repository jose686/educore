package com.educore.platform.store.service;

import com.educore.platform.store.model.PaqueteCreditos;
import java.util.List;

/**
 * Servicio para gestionar la lógica de negocio de los paquetes de créditos.
 */
public interface PaqueteCreditosService {

    List<PaqueteCreditos> obtenerTodos();

    List<PaqueteCreditos> obtenerPaquetesActivos();

    PaqueteCreditos obtenerPorId(Long id);

    PaqueteCreditos guardar(PaqueteCreditos paquete);

    void eliminar(Long id);
}
