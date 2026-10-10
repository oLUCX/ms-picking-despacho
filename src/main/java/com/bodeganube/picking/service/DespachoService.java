package com.bodeganube.picking.service;

import com.bodeganube.picking.dto.ActualizarDespachoRequest;
import com.bodeganube.picking.dto.CrearDespachoRequest;
import com.bodeganube.picking.dto.DespachoResponse;
import com.bodeganube.picking.exception.RecursoNoEncontradoException;
import com.bodeganube.picking.exception.ReglaNegocioException;
import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.model.EstadoDespacho;
import com.bodeganube.picking.repository.DespachoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de bodega: registro de picking, empaque y despacho de ordenes.
 * Ciclo de vida: EN_PREPARACION -> EMPACADO -> DESPACHADO.
 */
@Service
@Transactional(readOnly = true)
public class DespachoService {

    private final DespachoRepository despachoRepository;

    public DespachoService(DespachoRepository despachoRepository) {
        this.despachoRepository = despachoRepository;
    }

    /** RF-03: lista los despachos, opcionalmente filtrados por estado. */
    public List<DespachoResponse> listar(EstadoDespacho estado) {
        List<Despacho> despachos = (estado == null)
                ? despachoRepository.findAll()
                : despachoRepository.findByEstado(estado);
        return despachos.stream()
                .map(DespachoResponse::de)
                .toList();
    }

    public DespachoResponse obtener(Long ordenId) {
        return DespachoResponse.de(buscarPorOrden(ordenId));
    }

    /** Abre el registro de picking de una orden. Cada orden tiene a lo mas un despacho. */
    @Transactional
    public DespachoResponse crear(CrearDespachoRequest request) {
        if (despachoRepository.existsByOrdenId(request.ordenId())) {
            throw new ReglaNegocioException("Ya existe un despacho para la orden " + request.ordenId());
        }
        Despacho despacho = new Despacho();
        despacho.setOrdenId(request.ordenId());
        despacho.setTransportista(request.transportista());
        despacho.setEstado(EstadoDespacho.EN_PREPARACION);
        return DespachoResponse.de(despachoRepository.save(despacho));
    }

    @Transactional
    public DespachoResponse actualizar(Long ordenId, ActualizarDespachoRequest request) {
        Despacho despacho = buscarPorOrden(ordenId);
        validarQueNoEsteDespachado(despacho, "modificar");
        despacho.setTransportista(request.transportista());
        return DespachoResponse.de(despachoRepository.save(despacho));
    }

    @Transactional
    public void eliminar(Long ordenId) {
        Despacho despacho = buscarPorOrden(ordenId);
        validarQueNoEsteDespachado(despacho, "eliminar");
        despachoRepository.delete(despacho);
    }

    /** El operario confirma que la orden quedo empacada. */
    @Transactional
    public DespachoResponse confirmarEmpaque(Long ordenId) {
        Despacho despacho = buscarPorOrden(ordenId);
        if (despacho.getEstado() != EstadoDespacho.EN_PREPARACION) {
            throw new ReglaNegocioException("Solo se puede empacar un despacho en preparacion; estado actual: "
                    + despacho.getEstado());
        }
        despacho.setEstado(EstadoDespacho.EMPACADO);
        return DespachoResponse.de(despachoRepository.save(despacho));
    }

    /** RF-05: al despachar es obligatorio registrar un numero de seguimiento (tracking). */
    @Transactional
    public DespachoResponse despachar(Long ordenId) {
        Despacho despacho = buscarPorOrden(ordenId);
        if (despacho.getEstado() != EstadoDespacho.EMPACADO) {
            throw new ReglaNegocioException("Solo se puede despachar una orden empacada; estado actual: "
                    + despacho.getEstado());
        }
        if (despacho.getTransportista() == null || despacho.getTransportista().isBlank()) {
            throw new ReglaNegocioException("Asigna un transportista antes de despachar la orden " + ordenId);
        }
        despacho.setTrackingNumber(generarTracking());
        despacho.setEstado(EstadoDespacho.DESPACHADO);
        despacho.setFechaDespacho(LocalDateTime.now());
        return DespachoResponse.de(despachoRepository.save(despacho));
    }

    private Despacho buscarPorOrden(Long ordenId) {
        return despachoRepository.findByOrdenId(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe registro de picking para la orden " + ordenId));
    }

    private void validarQueNoEsteDespachado(Despacho despacho, String accion) {
        if (despacho.getEstado() == EstadoDespacho.DESPACHADO) {
            throw new ReglaNegocioException("No se puede " + accion + " el despacho de la orden "
                    + despacho.getOrdenId() + " porque ya salio de la bodega");
        }
    }

    private String generarTracking() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
