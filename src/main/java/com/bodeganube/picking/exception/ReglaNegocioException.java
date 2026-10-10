package com.bodeganube.picking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando la peticion esta bien formada pero choca con una regla del negocio
 * (despachar sin empacar, modificar un despacho ya enviado...).
 * Se traduce a HTTP 409.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
