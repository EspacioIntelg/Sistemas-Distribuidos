package com.unir.buscador.repository;

import com.unir.buscador.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    @Query("""
        SELECT l FROM Libro l
        WHERE (:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
        AND (:autor IS NULL OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :autor, '%')))
        AND (:anio IS NULL OR l.anioPublicacion = :anio)
        AND (:disponible IS NULL OR l.disponible = :disponible)
    """)
    List<Libro> buscar(
            @Param("titulo") String titulo,
            @Param("autor") String autor,
            @Param("anio") Integer anio,
            @Param("disponible") Boolean disponible
    );
}