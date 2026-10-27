package com.unir.operador.service;

import com.unir.operador.dto.LibroDto;
import com.unir.operador.dto.NuevoPrestamoRequest;
import com.unir.operador.model.Prestamo;
import com.unir.operador.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private CatalogoClient catalogoClient;

    @InjectMocks
    private PrestamoService prestamoService;

    private LibroDto libro;

    @BeforeEach
    void setUp() {
        libro = new LibroDto();
        libro.setId(7L);
        libro.setTitulo("Libro de prueba");
        libro.setDisponible(true);
    }

    @Test
    void crearReservaLibroYGuardaPrestamo() {
        when(catalogoClient.obtenerLibro(7L)).thenReturn(libro);
        when(prestamoRepository.save(any(Prestamo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Prestamo resultado = prestamoService.crear(nuevoPrestamo(7L, "Ana"));

        assertThat(resultado.getLibroId()).isEqualTo(7L);
        assertThat(resultado.getTituloLibro()).isEqualTo("Libro de prueba");
        assertThat(resultado.getUsuario()).isEqualTo("Ana");
        assertThat(resultado.getEstado()).isEqualTo("ACTIVO");
        assertThat(resultado.getFechaPrestamo()).isNotNull();
        verify(catalogoClient).actualizarDisponibilidad(7L, false);
        verify(prestamoRepository).save(any(Prestamo.class));
    }

    @Test
    void crearNoPermiteLibroYaPrestado() {
        libro.setDisponible(false);
        when(catalogoClient.obtenerLibro(7L)).thenReturn(libro);

        assertThatThrownBy(() -> prestamoService.crear(nuevoPrestamo(7L, "Ana")))
                .isInstanceOf(IllegalStateException.class);

        verify(catalogoClient, never()).actualizarDisponibilidad(7L, false);
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void crearDeshaceReservaSiNoPuedeGuardarPrestamo() {
        when(catalogoClient.obtenerLibro(7L)).thenReturn(libro);
        when(prestamoRepository.save(any(Prestamo.class)))
                .thenThrow(new DataIntegrityViolationException("error de prueba"));

        assertThatThrownBy(() -> prestamoService.crear(nuevoPrestamo(7L, "Ana")))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(catalogoClient).actualizarDisponibilidad(7L, false);
        verify(catalogoClient).actualizarDisponibilidad(7L, true);
    }

    @Test
    void devolverActualizaEstadoYDisponibilidad() {
        Prestamo prestamo = prestamoActivo();
        when(prestamoRepository.findById(3L)).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Prestamo resultado = prestamoService.devolver(3L);

        assertThat(resultado.getEstado()).isEqualTo("DEVUELTO");
        assertThat(resultado.getFechaDevolucion()).isNotNull();
        verify(catalogoClient).actualizarDisponibilidad(7L, true);
    }

    @Test
    void devolverNoPermiteDevolverDosVecesElMismoPrestamo() {
        Prestamo prestamo = prestamoActivo();
        prestamo.setEstado("DEVUELTO");
        when(prestamoRepository.findById(3L)).thenReturn(Optional.of(prestamo));

        assertThatThrownBy(() -> prestamoService.devolver(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue devuelto");

        verify(catalogoClient, never()).actualizarDisponibilidad(7L, true);
        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    @Test
    void crearDevuelveConflictoSiOtroPrestamoReservaAntesElLibro() {
        when(catalogoClient.obtenerLibro(7L)).thenReturn(libro);
        doThrow(HttpClientErrorException.create(
                        HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8))
                .when(catalogoClient).actualizarDisponibilidad(7L, false);

        assertThatThrownBy(() -> prestamoService.crear(nuevoPrestamo(7L, "Ana")))
                .isInstanceOf(IllegalStateException.class);

        verify(prestamoRepository, never()).save(any(Prestamo.class));
    }

    private NuevoPrestamoRequest nuevoPrestamo(Long libroId, String usuario) {
        NuevoPrestamoRequest request = new NuevoPrestamoRequest();
        request.setLibroId(libroId);
        request.setUsuario(usuario);
        return request;
    }

    private Prestamo prestamoActivo() {
        Prestamo prestamo = new Prestamo();
        prestamo.setId(3L);
        prestamo.setLibroId(7L);
        prestamo.setTituloLibro("Libro de prueba");
        prestamo.setUsuario("Ana");
        prestamo.setEstado("ACTIVO");
        return prestamo;
    }
}
