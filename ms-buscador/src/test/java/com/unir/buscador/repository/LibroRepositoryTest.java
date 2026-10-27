package com.unir.buscador.repository;

import com.unir.buscador.model.Libro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class LibroRepositoryTest {

    @Autowired
    private LibroRepository libroRepository;

    @Test
    void buscarAplicaSoloLosFiltrosInformadosIgnorandoMayusculas() {
        guardarLibro("Cien años de soledad", "Gabriel García Márquez", 1967, true);
        guardarLibro("El amor en los tiempos del cólera", "Gabriel García Márquez", 1985, false);
        guardarLibro("1984", "George Orwell", 1949, true);

        List<Libro> resultado = libroRepository.buscar("SOLEDAD", "garcía", null, true);

        assertThat(resultado).extracting(Libro::getTitulo)
                .containsExactly("Cien años de soledad");
    }

    @Test
    void buscarSinFiltrosDevuelveTodoElCatalogo() {
        guardarLibro("Libro uno", "Autora uno", 2001, true);
        guardarLibro("Libro dos", "Autor dos", 2002, false);

        assertThat(libroRepository.buscar(null, null, null, null)).hasSize(2);
    }

    @Test
    void reservarSiDisponibleSoloPermiteUnaReserva() {
        Libro libro = guardarLibro("Libro disponible", "Autora", 2024, true);

        assertThat(libroRepository.reservarSiDisponible(libro.getId())).isEqualTo(1);
        assertThat(libroRepository.reservarSiDisponible(libro.getId())).isZero();
        assertThat(libroRepository.findById(libro.getId()))
                .get()
                .extracting(Libro::isDisponible)
                .isEqualTo(false);
    }

    private Libro guardarLibro(String titulo, String autor, int anio, boolean disponible) {
        Libro libro = new Libro();
        libro.setTitulo(titulo);
        libro.setAutor(autor);
        libro.setAnioPublicacion(anio);
        libro.setDisponible(disponible);
        return libroRepository.save(libro);
    }
}
