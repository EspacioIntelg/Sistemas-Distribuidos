package com.unir.operador.service;

import com.unir.operador.dto.DisponibilidadRequest;
import com.unir.operador.dto.LibroDto;
import com.unir.operador.dto.NuevoPrestamoRequest;
import com.unir.operador.model.Prestamo;
import com.unir.operador.repository.PrestamoRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PrestamoService {

    // "ms-buscador" es el nombre con el que ese microservicio se registra en Eureka.
    // No usamos IP ni puerto: el LoadBalancer los resuelve a partir del registro.
    private static final String URL_LIBRO = "http://ms-buscador/libros/{id}";
    private static final String URL_DISPONIBILIDAD = "http://ms-buscador/libros/{id}/disponibilidad";

    private final PrestamoRepository prestamoRepository;
    private final RestTemplate restTemplate;

    public PrestamoService(PrestamoRepository prestamoRepository, RestTemplate restTemplate) {
        this.prestamoRepository = prestamoRepository;
        this.restTemplate = restTemplate;
    }

    public List<Prestamo> listar() {
        return prestamoRepository.findAll();
    }

    public Optional<Prestamo> obtener(Long id) {
        return prestamoRepository.findById(id);
    }

    public Prestamo crear(NuevoPrestamoRequest request) {
        if (request == null || request.getLibroId() == null) {
            throw new IllegalArgumentException("El identificador del libro es obligatorio");
        }
        if (request.getUsuario() == null || request.getUsuario().isBlank()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }

        LibroDto libro = obtenerLibro(request.getLibroId());

        if (!libro.isDisponible()) {
            throw new IllegalStateException("El libro ya esta prestado: " + request.getLibroId());
        }

        actualizarDisponibilidad(libro.getId(), false);

        Prestamo prestamo = new Prestamo();
        prestamo.setLibroId(libro.getId());
        prestamo.setTituloLibro(libro.getTitulo());
        prestamo.setUsuario(request.getUsuario());
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setEstado("ACTIVO");

        try {
            return prestamoRepository.save(prestamo);
        } catch (DataAccessException saveError) {
            try {
                actualizarDisponibilidad(libro.getId(), true);
            } catch (RestClientException | IllegalStateException rollbackError) {
                saveError.addSuppressed(rollbackError);
            }
            throw saveError;
        }
    }

    public Prestamo devolver(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Préstamo no encontrado: " + id));

        if (!"ACTIVO".equals(prestamo.getEstado())) {
            throw new IllegalStateException("El préstamo ya fue devuelto: " + id);
        }

        actualizarDisponibilidad(prestamo.getLibroId(), true);
        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaDevolucion(LocalDate.now());

        try {
            return prestamoRepository.save(prestamo);
        } catch (DataAccessException saveError) {
            try {
                actualizarDisponibilidad(prestamo.getLibroId(), false);
            } catch (RestClientException | IllegalStateException rollbackError) {
                saveError.addSuppressed(rollbackError);
            }
            throw saveError;
        }
    }

    private LibroDto obtenerLibro(Long libroId) {
        try {
            LibroDto libro = restTemplate.getForObject(URL_LIBRO, LibroDto.class, libroId);
            if (libro == null) {
                throw new NoSuchElementException("Libro no encontrado: " + libroId);
            }
            return libro;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NoSuchElementException("Libro no encontrado: " + libroId);
        }
    }

    private void actualizarDisponibilidad(Long libroId, boolean disponible) {
        try {
            restTemplate.put(URL_DISPONIBILIDAD, new DisponibilidadRequest(disponible), libroId);
        } catch (HttpClientErrorException.Conflict ex) {
            throw new IllegalStateException("El libro ya no esta disponible: " + libroId, ex);
        }
    }
}
