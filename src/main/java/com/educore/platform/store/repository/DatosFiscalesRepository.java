package com.educore.platform.store.repository;

import com.educore.platform.store.model.DatosFiscales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para acceder a los datos fiscales de la empresa.
 */
@Repository
public interface DatosFiscalesRepository extends JpaRepository<DatosFiscales, Long> {
}
