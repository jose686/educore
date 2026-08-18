package com.educore.platform.users.repository;

import com.educore.platform.users.model.TicketSoporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para acceder a los tickets de soporte.
 */
@Repository
public interface TicketSoporteRepository extends JpaRepository<TicketSoporte, Long> {

    List<TicketSoporte> findByEstadoOrderByFechaCreacionDesc(String estado);

    List<TicketSoporte> findAllByOrderByFechaCreacionDesc();

    List<TicketSoporte> findByPedidoId(Long pedidoId);
}
