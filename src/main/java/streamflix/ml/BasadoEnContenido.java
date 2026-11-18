package streamflix.ml;

import streamflix.modelo.Contenido;
import streamflix.modelo.Usuario;

/**
 * ANÁLISIS DE CONTENIDO: usa las características de la película (género, director, idioma)
 * y cuánto le han gustado antes al usuario contenidos con esas mismas características.
 */
public class BasadoEnContenido implements Modelo {
    private final PerfilUsuarios perfil;

    public BasadoEnContenido(PerfilUsuarios perfil) {
        this.perfil = perfil;
    }

    @Override
    public String nombre() {
        return "Basado en contenido";
    }

    @Override
    public double puntaje(Usuario u, Contenido c) {
        return 0.60 * perfil.afinidad(u, c, "genero")
             + 0.20 * perfil.afinidad(u, c, "director")
             + 0.20 * perfil.afinidad(u, c, "idioma");
    }
}
