package com.bodeganube.picking.dto;

import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.model.EstadoDespacho;
import java.time.LocalDateTime;

/** Lo que la API devuelve de un despacho. Desacopla el JSON de la entidad JPA. */
public record DespachoResponse(
        Long id,
        Long ordenId,
        String transportista,
        String trackingNumber,
        EstadoDespacho estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaDespacho
) {
    public static DespachoResponse de(Despacho despacho) {
        return new DespachoResponse(despacho.getId(), despacho.getOrdenId(), despacho.getTransportista(),
                despacho.getTrackingNumber(), despacho.getEstado(), despacho.getFechaCreacion(),
                despacho.getFechaDespacho());
    }
}
