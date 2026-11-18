package streamflix.ml;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import streamflix.modelo.BaseDatos;
import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Recomendacion;
import streamflix.modelo.Usuario;

/**
 * Contrato común de todos los modelos de recomendación.
 * puntaje() devuelve una estimación entre 0 y 1 de que a un usuario le guste un contenido.
 */
public interface Modelo {

    String nombre();

    /** Puntaje de un par (usuario, contenido) que el usuario aún no ha visto. */
    double puntaje(Usuario u, Contenido c);

    /** Puntaje usando una interacción del historial (los modelos "de comportamiento" lo sobrescriben). */
    default double puntaje(Historial h) {
        return puntaje(h.getUsuario(), h.getContenido());
    }

    /** A partir de este valor se predice "le gusta". */
    default double umbral() {
        return 0.5;
    }

    /** true si el modelo necesita datos de la propia visualización (minutos vistos, favoritos). */
    default boolean usaComportamiento() {
        return false;
    }

    /** Top N de contenido NO visto con mayor puntaje para el usuario. */
    default List<Recomendacion> recomendar(Usuario u, BaseDatos bd, int n) {
        Set<String> vistos = bd.idsVistosPor(u);
        List<Recomendacion> lista = new ArrayList<>();
        for (Contenido c : bd.getContenidos()) {
            if (!vistos.contains(c.getId())) {
                lista.add(new Recomendacion(LocalDate.now(), u, c, puntaje(u, c), nombre()));
            }
        }
        lista.sort((a, b) -> Double.compare(b.getPuntaje(), a.getPuntaje()));
        return lista.subList(0, Math.min(n, lista.size()));
    }
}
