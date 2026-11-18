package streamflix.ml;

import java.util.List;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * SISTEMA ACTUAL de StreamFlix (línea base): recomienda por géneros populares y ranking
 * general, igual para todos los usuarios. Sirve para comparar contra el sistema propuesto.
 */
public class SistemaActual implements Modelo {
    private final PerfilUsuarios perfil;
    private final double umbral;

    public SistemaActual(List<Historial> entrenamiento) {
        this.perfil = new PerfilUsuarios(entrenamiento);
        double suma = 0;
        for (Historial h : entrenamiento) suma += puntajeBase(h.getContenido());
        this.umbral = entrenamiento.isEmpty() ? 0.5 : suma / entrenamiento.size();
    }

    private double puntajeBase(Contenido c) {
        return 0.5 * perfil.tasaGenero(c.getGenero()) + 0.5 * (c.getValoracionPromedio() / 5.0);
    }

    @Override
    public String nombre() {
        return "Sistema actual (popularidad)";
    }

    @Override
    public double puntaje(Usuario u, Contenido c) {
        return puntajeBase(c);   // no depende del usuario
    }

    @Override
    public double umbral() {
        return umbral;
    }
}
