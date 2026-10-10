package com.bodeganube.picking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeganube.picking.dto.CrearDespachoRequest;
import com.bodeganube.picking.dto.DespachoResponse;
import com.bodeganube.picking.exception.RecursoNoEncontradoException;
import com.bodeganube.picking.exception.ReglaNegocioException;
import com.bodeganube.picking.model.Despacho;
import com.bodeganube.picking.model.EstadoDespacho;
import com.bodeganube.picking.repository.DespachoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Pruebas unitarias del ciclo EN_PREPARACION -> EMPACADO -> DESPACHADO con el repositorio simulado. */
@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    @Mock
    private DespachoRepository despachoRepository;

    @InjectMocks
    private DespachoService despachoService;

    @Test
    void crearDespachoDuplicadoLanzaReglaNegocio() {
        when(despachoRepository.existsByOrdenId(10L)).thenReturn(true);

        assertThatThrownBy(() -> despachoService.crear(new CrearDespachoRequest(10L, "Starken")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe");
    }

    @Test
    void crearDespachoNuevoQuedaEnPreparacion() {
        when(despachoRepository.existsByOrdenId(10L)).thenReturn(false);
        when(despachoRepository.save(any(Despacho.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        DespachoResponse creado = despachoService.crear(new CrearDespachoRequest(10L, "Starken"));

        assertThat(creado.estado()).isEqualTo(EstadoDespacho.EN_PREPARACION);
        assertThat(creado.transportista()).isEqualTo("Starken");
    }

    @Test
    void confirmarEmpaqueDeOrdenInexistenteLanzaRecursoNoEncontrado() {
        when(despachoRepository.findByOrdenId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoService.confirmarEmpaque(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void despacharSinEmpacarLanzaReglaNegocio() {
        when(despachoRepository.findByOrdenId(10L))
                .thenReturn(Optional.of(despacho(10L, EstadoDespacho.EN_PREPARACION, "Starken")));

        assertThatThrownBy(() -> despachoService.despachar(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("empacada");
        verify(despachoRepository, never()).save(any());
    }

    @Test
    void despacharSinTransportistaLanzaReglaNegocio() {
        when(despachoRepository.findByOrdenId(10L))
                .thenReturn(Optional.of(despacho(10L, EstadoDespacho.EMPACADO, null)));

        assertThatThrownBy(() -> despachoService.despachar(10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("transportista");
    }

    @Test
    void despacharOrdenEmpacadaGeneraTrackingYFecha() {
        when(despachoRepository.findByOrdenId(10L))
                .thenReturn(Optional.of(despacho(10L, EstadoDespacho.EMPACADO, "Starken")));
        when(despachoRepository.save(any(Despacho.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        DespachoResponse despachado = despachoService.despachar(10L);

        assertThat(despachado.estado()).isEqualTo(EstadoDespacho.DESPACHADO);
        assertThat(despachado.trackingNumber()).startsWith("TRK-").hasSize(12);
        assertThat(despachado.fechaDespacho()).isNotNull();
    }

    @Test
    void eliminarDespachoYaDespachadoLanzaReglaNegocio() {
        when(despachoRepository.findByOrdenId(10L))
                .thenReturn(Optional.of(despacho(10L, EstadoDespacho.DESPACHADO, "Starken")));

        assertThatThrownBy(() -> despachoService.eliminar(10L))
                .isInstanceOf(ReglaNegocioException.class);
        verify(despachoRepository, never()).delete(any());
    }

    private static Despacho despacho(Long ordenId, EstadoDespacho estado, String transportista) {
        Despacho despacho = new Despacho();
        despacho.setId(1L);
        despacho.setOrdenId(ordenId);
        despacho.setEstado(estado);
        despacho.setTransportista(transportista);
        return despacho;
    }
}
