package com.bodeganube.picking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Asigna o cambia el transportista mientras el despacho no haya salido de la bodega. */
public record ActualizarDespachoRequest(
        @NotBlank(message = "El transportista es obligatorio")
        @Size(max = 60, message = "El transportista no puede superar los 60 caracteres")
        String transportista
) {
}
