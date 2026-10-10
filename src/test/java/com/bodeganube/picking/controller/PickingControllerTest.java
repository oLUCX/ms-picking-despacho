package com.bodeganube.picking.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeganube.picking.dto.CrearDespachoRequest;
import com.bodeganube.picking.dto.DespachoResponse;
import com.bodeganube.picking.exception.RecursoNoEncontradoException;
import com.bodeganube.picking.exception.ReglaNegocioException;
import com.bodeganube.picking.model.EstadoDespacho;
import com.bodeganube.picking.service.DespachoService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba la capa web aislada: codigos HTTP, validaciones y formato de error. */
@WebMvcTest(PickingController.class)
class PickingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DespachoService despachoService;

    @Test
    void crearDespachoValidoDevuelve201ConLocation() throws Exception {
        when(despachoService.crear(any(CrearDespachoRequest.class))).thenReturn(respuesta(EstadoDespacho.EN_PREPARACION));

        mockMvc.perform(post("/api/picking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordenId\":10,\"transportista\":\"Starken\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/picking/10")))
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
    }

    @Test
    void crearDespachoSinOrdenDevuelve400() throws Exception {
        mockMvc.perform(post("/api/picking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transportista\":\"Starken\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.ordenId").exists());
    }

    @Test
    void obtenerDespachoInexistenteDevuelve404() throws Exception {
        when(despachoService.obtener(99L))
                .thenThrow(new RecursoNoEncontradoException("No existe registro de picking para la orden 99"));

        mockMvc.perform(get("/api/picking/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe registro de picking para la orden 99"));
    }

    @Test
    void listarConEstadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/picking").param("estado", "PERDIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void despacharSinEmpacarDevuelve409() throws Exception {
        when(despachoService.despachar(10L))
                .thenThrow(new ReglaNegocioException("Solo se puede despachar una orden empacada"));

        mockMvc.perform(post("/api/picking/10/despachar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/picking/10"))
                .andExpect(status().isNoContent());

        verify(despachoService).eliminar(10L);
    }

    private static DespachoResponse respuesta(EstadoDespacho estado) {
        return new DespachoResponse(1L, 10L, "Starken", null, estado, LocalDateTime.now(), null);
    }
}
