package com.bodeganube.picking.controller;

import com.bodeganube.picking.dto.ActualizarDespachoRequest;
import com.bodeganube.picking.dto.CrearDespachoRequest;
import com.bodeganube.picking.dto.DespachoResponse;
import com.bodeganube.picking.model.EstadoDespacho;
import com.bodeganube.picking.service.DespachoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * RF-03: el operario consulta y gestiona los despachos de las ordenes listas para picking.
 * RF-05: registro obligatorio de un numero de seguimiento (tracking) al despachar.
 * Los despachos se identifican por el id de la orden de ms-ordenes.
 */
@RestController
@RequestMapping("/api/picking")
public class PickingController {

    private final DespachoService despachoService;

    public PickingController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @GetMapping
    public List<DespachoResponse> listar(@RequestParam(required = false) EstadoDespacho estado) {
        return despachoService.listar(estado);
    }

    @GetMapping("/{ordenId}")
    public DespachoResponse obtener(@PathVariable Long ordenId) {
        return despachoService.obtener(ordenId);
    }

    @PostMapping
    public ResponseEntity<DespachoResponse> crear(@Valid @RequestBody CrearDespachoRequest request) {
        DespachoResponse creado = despachoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{ordenId}")
                .buildAndExpand(creado.ordenId())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{ordenId}")
    public DespachoResponse actualizar(@PathVariable Long ordenId,
                                       @Valid @RequestBody ActualizarDespachoRequest request) {
        return despachoService.actualizar(ordenId, request);
    }

    @DeleteMapping("/{ordenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long ordenId) {
        despachoService.eliminar(ordenId);
    }

    @PostMapping("/{ordenId}/confirmar-empaque")
    public DespachoResponse confirmarEmpaque(@PathVariable Long ordenId) {
        return despachoService.confirmarEmpaque(ordenId);
    }

    @PostMapping("/{ordenId}/despachar")
    public DespachoResponse despachar(@PathVariable Long ordenId) {
        return despachoService.despachar(ordenId);
    }
}
