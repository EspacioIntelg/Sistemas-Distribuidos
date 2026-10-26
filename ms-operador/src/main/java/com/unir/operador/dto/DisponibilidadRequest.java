package com.unir.operador.dto;

public class DisponibilidadRequest {

    private boolean disponible;

    public DisponibilidadRequest() {
    }

    public DisponibilidadRequest(boolean disponible) {
        this.disponible = disponible;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}
