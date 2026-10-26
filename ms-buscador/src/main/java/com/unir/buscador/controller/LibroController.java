package com.unir.buscador.controller;

import com.unir.buscador.dto.DisponibilidadRequest;
import com.unir.buscador.model.Libro;
import com.unir.buscador.repository.LibroRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/libros")
public class LibroController {

    private final LibroRepository libroRepository;

    public LibroController(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    // GET /libros
    // Permite buscar por título, autor, año y disponibilidad.
    ///Todos los parámetros son opcionales.
    @GetMapping
    public List<Libro> buscar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Boolean disponible) {

        return libroRepository.buscar(titulo, autor, anio, disponible);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Libro> obtener(@PathVariable Long id) {
        return libroRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Libro> crear(@RequestBody Libro libro,
                                        UriComponentsBuilder uriBuilder) {
        // Validación básica: un libro sin título o autor no tiene sentido en el catálogo.
        if (libro.getTitulo() == null || libro.getTitulo().isBlank()
                || libro.getAutor() == null || libro.getAutor().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // El id lo genera la base de datos (@GeneratedValue): nos aseguramos de que
        // no llegue uno desde el cliente, para no pisar un registro existente.
        libro.setId(null);

        Libro guardado = libroRepository.save(libro);

        URI location = uriBuilder
                .path("/libros/{id}")
                .buildAndExpand(guardado.getId())
                .toUri();

        return ResponseEntity.created(location).body(guardado);
    }

    // ms-operador llama aquí para marcar un libro como prestado/devuelto.
    // Es la única fuente de verdad sobre disponibilidad.
    @PutMapping("/{id}/disponibilidad")
    public ResponseEntity<Libro> actualizarDisponibilidad(@PathVariable Long id,
                                                           @RequestBody DisponibilidadRequest request) {
        return libroRepository.findById(id)
                .map(libro -> {
                    libro.setDisponible(request.isDisponible());
                    return ResponseEntity.ok(libroRepository.save(libro));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
