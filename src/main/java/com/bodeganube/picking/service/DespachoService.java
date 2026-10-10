package com.bodeganube.picking.service;

import com.bodeganube.picking.exception.RecursoNoEncontradoException;
import com.bodeganube.picking.exception.ReglaNegocioException;
import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.model.EstadoDespacho;
import com.bodeganube.picking.repository.DespachoRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de bodega: empaque y despacho de ordenes. El controller solo traduce HTTP y delega aqui.
 */
@Service
@Transactional(readOnly = true)
public class DespachoService {

    private final DespachoRepository despachoRepository;

    public DespachoService(DespachoRepository despachoRepository) {
        this.despachoRepository = despachoRepository;
    }

    /** El operario confirma que la orden quedo empacada. Una orden ya despachada no se puede volver a empacar. */
    @Transactional
    public Despacho confirmarEmpaque(Long ordenId) {
        Despacho despacho = despachoRepository.findByOrdenId(ordenId)
                .orElseGet(() -> {
                    Despacho nuevo = new Despacho();
                    nuevo.setOrdenId(ordenId);
                    return nuevo;
                });
        if (despacho.getEstado() == EstadoDespacho.DESPACHADO) {
            throw new ReglaNegocioException("La orden " + ordenId + " ya fue despachada");
        }
        despacho.setEstado(EstadoDespacho.EMPACADO);
        return despachoRepository.save(despacho);
    }

    /** RF-05: al despachar es obligatorio registrar un numero de seguimiento (tracking). */
    @Transactional
    public Despacho despachar(Long ordenId) {
        Despacho despacho = buscarPorOrden(ordenId);
        if (despacho.getEstado() != EstadoDespacho.EMPACADO) {
            throw new ReglaNegocioException("Solo se puede despachar una orden empacada; estado actual: "
                    + despacho.getEstado());
        }
        despacho.setTrackingNumber(generarTracking());
        despacho.setEstado(EstadoDespacho.DESPACHADO);
        despacho.setFechaDespacho(LocalDateTime.now());
        return despachoRepository.save(despacho);
    }

    private Despacho buscarPorOrden(Long ordenId) {
        return despachoRepository.findByOrdenId(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe registro de picking para la orden " + ordenId));
    }

    private String generarTracking() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
