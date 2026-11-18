package streamflix.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * Modelo basado en el árbol de decisión.
 * Para evitar "trampa" (data leakage) las afinidades del entrenamiento se calculan
 * con un perfil que NO incluye la fila que se está describiendo (validación interna por bloques).
 */
public class ArbolModelo implements Modelo {
    private final boolean comportamiento;
    private final PerfilUsuarios perfil;
    private final ArbolDecision arbol;

    public ArbolModelo(List<Historial> entrenamiento, boolean comportamiento,
                       int maxProfundidad, int minMuestras, long semilla) {
        this.comportamiento = comportamiento;
        this.perfil = new PerfilUsuarios(entrenamiento);
        String[] nombres = comportamiento ? Caracteristicas.NOMBRES_COMPORTAMIENTO
                                          : Caracteristicas.NOMBRES_PREVIAS;
        this.arbol = new ArbolDecision(nombres, maxProfundidad, minMuestras);

        // Características de entrenamiento: cada bloque se describe con un perfil construido SIN ese bloque
        List<Historial> mezclado = new ArrayList<>(entrenamiento);
        Collections.shuffle(mezclado, new Random(semilla));
        int bloques = 5;
        List<double[]> x = new ArrayList<>();
        List<Boolean> y = new ArrayList<>();
        for (int b = 0; b < bloques; b++) {
            List<Historial> fuera = new ArrayList<>();
            List<Historial> dentro = new ArrayList<>();
            for (int i = 0; i < mezclado.size(); i++) {
                if (i % bloques == b) fuera.add(mezclado.get(i)); else dentro.add(mezclado.get(i));
            }
            PerfilUsuarios perfilInterno = new PerfilUsuarios(dentro);
            for (Historial h : fuera) {
                x.add(comportamiento ? Caracteristicas.conComportamiento(h, perfilInterno)
                                     : Caracteristicas.previas(h.getUsuario(), h.getContenido(), perfilInterno));
                y.add(h.isLeGusto());
            }
        }
        arbol.entrenar(x, y);
    }

    public ArbolDecision getArbol() {
        return arbol;
    }

    @Override
    public String nombre() {
        return comportamiento ? "Árbol (con comportamiento)" : "Árbol de decisión";
    }

    @Override
    public boolean usaComportamiento() {
        return comportamiento;
    }

    @Override
    public double puntaje(Usuario u, Contenido c) {
        if (comportamiento) {
            throw new UnsupportedOperationException("Este árbol necesita datos de visualización (usar puntaje(Historial)).");
        }
        return arbol.probabilidad(Caracteristicas.previas(u, c, perfil));
    }

    @Override
    public double puntaje(Historial h) {
        if (comportamiento) return arbol.probabilidad(Caracteristicas.conComportamiento(h, perfil));
        return puntaje(h.getUsuario(), h.getContenido());
    }
}
