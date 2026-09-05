package com.bodeganube.picking.controller;

import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.model.EstadoDespacho;
import com.bodeganube.picking.repository.DespachoRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * RF-03/RF-04: el operario consulta ordenes disponibles (expuestas por ms-ordenes) y confirma
 * empaque/despacho.
 * RF-05: registro obligatorio de un numero de seguimiento (tracking) al confirmar el despacho.
 *
 * NOTA (esqueleto minimo): en la version funcional completa, este servicio consultaria a
 * ms-ordenes via HTTP para validar que la orden esta LISTA_PARA_PICKING antes de aceptar el
 * empaque, y notificaria de vuelta el cambio de estado tras el despacho.
 */
@RestController
@RequestMapping("/api/picking")
public class PickingController {

    private final DespachoRepository despachoRepository;

    public PickingController(DespachoRepository despachoRepository) {
        this.despachoRepository = despachoRepository;
    }

    @PostMapping("/{ordenId}/confirmar-empaque")
    public ResponseEntity<Despacho> confirmarEmpaque(@PathVariable Long ordenId) {
        Despacho despacho = despachoRepository.findByOrdenId(ordenId)
                .orElseGet(() -> {
                    Despacho nuevo = new Despacho();
                    nuevo.setOrdenId(ordenId);
                    return nuevo;
                });
        despacho.setEstado(EstadoDespacho.EMPACADO);
        return ResponseEntity.ok(despachoRepository.save(despacho));
    }

    @PostMapping("/{ordenId}/despachar")
    public ResponseEntity<Despacho> despachar(@PathVariable Long ordenId) {
        Despacho despacho = despachoRepository.findByOrdenId(ordenId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe registro de picking para esta orden"));

        despacho.setTrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        despacho.setEstado(EstadoDespacho.DESPACHADO);
        despacho.setFechaDespacho(LocalDateTime.now());

        return ResponseEntity.ok(despachoRepository.save(despacho));
    }
}
