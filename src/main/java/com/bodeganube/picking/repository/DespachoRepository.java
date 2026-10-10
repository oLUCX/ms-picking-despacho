package com.bodeganube.picking.repository;

import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.model.EstadoDespacho;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    Optional<Despacho> findByOrdenId(Long ordenId);

    boolean existsByOrdenId(Long ordenId);

    List<Despacho> findByEstado(EstadoDespacho estado);
}
