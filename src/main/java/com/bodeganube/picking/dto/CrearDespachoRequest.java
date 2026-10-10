package com.bodeganube.picking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Abre el registro de picking de una orden. El transportista se puede asignar ahora o despues. */
public record CrearDespachoRequest(
        @NotNull(message = "El id de la orden es obligatorio")
        @Positive(message = "El id de la orden debe ser positivo")
        Long ordenId,

        @Size(max = 60, message = "El transportista no puede superar los 60 caracteres")
        String transportista
) {
}
