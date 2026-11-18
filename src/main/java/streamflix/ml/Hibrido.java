package streamflix.ml;

import java.util.List;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * MODELO PROPUESTO: combina filtrado colaborativo + análisis de contenido + árbol de decisión.
 * Si el filtrado colaborativo no tiene vecinos, usa la tendencia general del usuario.
 */
public class Hibrido implements Modelo {
    private static final double PESO_COLABORATIVO = 0.40;
    private static final double PESO_CONTENIDO = 0.30;
    private static final double PESO_ARBOL = 0.30;

    private final FiltradoColaborativo colaborativo;
    private final BasadoEnContenido contenido;
    private final ArbolModelo arbol;

    public Hibrido(List<Historial> entrenamiento, long semilla) {
        PerfilUsuarios perfil = new PerfilUsuarios(entrenamiento);
        this.colaborativo = new FiltradoColaborativo(entrenamiento, perfil, 5);
        this.contenido = new BasadoEnContenido(perfil);
        this.arbol = new ArbolModelo(entrenamiento, false, 4, 10, semilla);
    }

    @Override
    public String nombre() {
        return "Híbrido (propuesto)";
    }

    @Override
    public double puntaje(Usuario u, Contenido c) {
        return PESO_COLABORATIVO * colaborativo.puntaje(u, c)
             + PESO_CONTENIDO * contenido.puntaje(u, c)
             + PESO_ARBOL * arbol.puntaje(u, c);
    }
}
