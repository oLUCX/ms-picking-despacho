package com.bodeganube.picking.controller;

import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.service.DespachoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF-03/RF-04: el operario consulta ordenes disponibles (expuestas por ms-ordenes) y confirma
 * empaque/despacho.
 * RF-05: registro obligatorio de un numero de seguimiento (tracking) al confirmar el despacho.
 */
@RestController
@RequestMapping("/api/picking")
public class PickingController {

    private final DespachoService despachoService;

    public PickingController(DespachoService despachoService) {
        this.despachoService = despachoService;
    }

    @PostMapping("/{ordenId}/confirmar-empaque")
    public Despacho confirmarEmpaque(@PathVariable Long ordenId) {
        return despachoService.confirmarEmpaque(ordenId);
    }

    @PostMapping("/{ordenId}/despachar")
    public Despacho despachar(@PathVariable Long ordenId) {
        return despachoService.despachar(ordenId);
    }
}
