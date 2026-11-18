package streamflix.ml;

import java.util.Arrays;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * Convierte un par (usuario, contenido) en un vector numérico de características
 * (los "datos predictores" del punto 2) que consume el árbol de decisión.
 */
public class Caracteristicas {

    /** Características que se conocen ANTES de ver el contenido (sirven para recomendar). */
    public static final String[] NOMBRES_PREVIAS = {
        "afinidadGenero", "afinidadDirector", "afinidadIdioma", "afinidadUsuario",
        "valoracionPromedio", "esSerie", "duracionMin", "edad", "mismoPais"
    };

    /** Previas + comportamiento durante la visualización. */
    public static final String[] NOMBRES_COMPORTAMIENTO = {
        "afinidadGenero", "afinidadDirector", "afinidadIdioma", "afinidadUsuario",
        "valoracionPromedio", "esSerie", "duracionMin", "edad", "mismoPais",
        "porcentajeVisto", "guardoFavoritos"
    };

    public static double[] previas(Usuario u, Contenido c, PerfilUsuarios p) {
        return new double[] {
            p.afinidad(u, c, "genero"),
            p.afinidad(u, c, "director"),
            p.afinidad(u, c, "idioma"),
            p.afinidadUsuario(u),
            c.getValoracionPromedio(),
            c.esSerie() ? 1 : 0,
            c.getDuracionMin(),
            u.getEdad(),
            u.getPais().equalsIgnoreCase(c.getPais()) ? 1 : 0
        };
    }

    public static double[] conComportamiento(Historial h, PerfilUsuarios p) {
        double[] base = previas(h.getUsuario(), h.getContenido(), p);
        double[] v = Arrays.copyOf(base, base.length + 2);
        v[base.length] = h.getPorcentajeVisto();
        v[base.length + 1] = h.isFavorito() ? 1 : 0;
        return v;
    }
}
