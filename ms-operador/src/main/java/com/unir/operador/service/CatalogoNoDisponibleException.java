package com.unir.operador.service;

import org.springframework.web.client.RestClientException;

public class CatalogoNoDisponibleException extends RestClientException {

    public CatalogoNoDisponibleException(Throwable cause) {
        super("El servicio de catálogo no está disponible temporalmente.", cause);
    }
}
