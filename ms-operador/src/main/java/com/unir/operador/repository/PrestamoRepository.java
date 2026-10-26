package com.unir.operador.repository;

import com.unir.operador.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
}
