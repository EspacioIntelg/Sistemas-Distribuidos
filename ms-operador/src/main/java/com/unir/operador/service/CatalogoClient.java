package com.unir.operador.service;

import com.unir.operador.dto.DisponibilidadRequest;
import com.unir.operador.dto.LibroDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class CatalogoClient {

    private static final String URL_LIBRO = "http://ms-buscador/libros/{id}";
    private static final String URL_DISPONIBILIDAD = "http://ms-buscador/libros/{id}/disponibilidad";

    private final RestTemplate restTemplate;

    public CatalogoClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "catalogo", fallbackMethod = "obtenerLibroFallback")
    public LibroDto obtenerLibro(Long libroId) {
        return restTemplate.getForObject(URL_LIBRO, LibroDto.class, libroId);
    }

    @CircuitBreaker(name = "catalogo", fallbackMethod = "actualizarDisponibilidadFallback")
    public void actualizarDisponibilidad(Long libroId, boolean disponible) {
        restTemplate.put(URL_DISPONIBILIDAD, new DisponibilidadRequest(disponible), libroId);
    }

    public LibroDto obtenerLibroFallback(Long libroId, Throwable error) {
        if (error instanceof HttpClientErrorException clientError) {
            throw clientError;
        }
        throw new CatalogoNoDisponibleException(error);
    }

    public void actualizarDisponibilidadFallback(Long libroId, boolean disponible, Throwable error) {
        if (error instanceof HttpClientErrorException clientError) {
            throw clientError;
        }
        throw new CatalogoNoDisponibleException(error);
    }
}
