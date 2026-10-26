package com.unir.operador.service;

import com.unir.operador.dto.DisponibilidadRequest;
import com.unir.operador.dto.LibroDto;
import com.unir.operador.dto.NuevoPrestamoRequest;
import com.unir.operador.model.Prestamo;
import com.unir.operador.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
        LibroDto libro = obtenerLibro(request.getLibroId());

        if (!libro.isDisponible()) {
            throw new IllegalStateException("El libro ya esta prestado: " + request.getLibroId());
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setLibroId(libro.getId());
        prestamo.setTituloLibro(libro.getTitulo());
        prestamo.setUsuario(request.getUsuario());
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setEstado("ACTIVO");

        Prestamo guardado = prestamoRepository.save(prestamo);
        actualizarDisponibilidad(libro.getId(), false);
        return guardado;
    }

    public Prestamo devolver(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Préstamo no encontrado: " + id));

        prestamo.setEstado("DEVUELTO");
        prestamo.setFechaDevolucion(LocalDate.now());
        Prestamo guardado = prestamoRepository.save(prestamo);

        actualizarDisponibilidad(guardado.getLibroId(), true);
        return guardado;
    }

    @SuppressWarnings("unused")
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
        restTemplate.put(URL_DISPONIBILIDAD, new DisponibilidadRequest(disponible), libroId);
    }
}
