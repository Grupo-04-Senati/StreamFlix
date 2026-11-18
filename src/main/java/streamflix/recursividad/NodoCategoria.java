package streamflix.recursividad;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import streamflix.ml.PerfilUsuarios;
import streamflix.modelo.Contenido;
import streamflix.modelo.Usuario;

/**
 * Estructura de ÁRBOL del catálogo: StreamFlix -> Tipo (Película/Serie) -> Género -> Títulos.
 * Todos los métodos principales son recursivos porque el árbol tiene niveles anidados.
 */
public class NodoCategoria {

    /** Resultado de buscar el mejor título dentro de un subárbol. */
    public static class Candidato {
        public final Contenido contenido;
        public final double puntaje;

        public Candidato(Contenido contenido, double puntaje) {
            this.contenido = contenido;
            this.puntaje = puntaje;
        }
    }

    private final String nombre;
    private final List<NodoCategoria> hijos = new ArrayList<>();
    private final List<Contenido> titulos = new ArrayList<>();

    private NodoCategoria(String nombre) {
        this.nombre = nombre;
    }

    /** Construye el árbol de categorías a partir del catálogo. */
    public static NodoCategoria construir(List<Contenido> catalogo) {
        NodoCategoria raiz = new NodoCategoria("StreamFlix");
        for (Contenido c : catalogo) {
            NodoCategoria tipo = raiz.obtenerHijo(c.getTipo());
            NodoCategoria genero = tipo.obtenerHijo(c.getGenero());
            genero.titulos.add(c);
        }
        return raiz;
    }

    private NodoCategoria obtenerHijo(String nombreHijo) {
        for (NodoCategoria h : hijos) {
            if (h.nombre.equals(nombreHijo)) return h;
        }
        NodoCategoria nuevo = new NodoCategoria(nombreHijo);
        hijos.add(nuevo);
        return nuevo;
    }

    /** Cuenta los títulos de este nodo y de todos sus descendientes. */
    public int contarTitulos() {
        int total = titulos.size();
        for (NodoCategoria h : hijos) total += h.contarTitulos();
        return total;
    }

    /** Altura del árbol (cantidad de niveles). */
    public int profundidad() {
        int max = 0;
        for (NodoCategoria h : hijos) max = Math.max(max, h.profundidad());
        return 1 + max;
    }

    /** Imprime el árbol con sangría según el nivel. */
    public void imprimir(int nivel) {
        StringBuilder sangria = new StringBuilder();
        for (int i = 0; i < nivel; i++) sangria.append("    ");
        System.out.println(sangria + "└─ " + nombre + " (" + contarTitulos() + ")");
        for (NodoCategoria h : hijos) h.imprimir(nivel + 1);
    }

    /**
     * Recorre TODO el subárbol y devuelve el título aún no visto con mayor puntaje
     * (afinidad del usuario con el género x valoración promedio del título).
     */
    public Candidato mejorCandidato(Usuario u, PerfilUsuarios perfil, Set<String> vistos) {
        Candidato mejor = null;
        for (Contenido c : titulos) {
            if (vistos.contains(c.getId())) continue;
            double p = perfil.afinidad(u, c, "genero") * (c.getValoracionPromedio() / 5.0);
            if (mejor == null || p > mejor.puntaje) mejor = new Candidato(c, p);
        }
        for (NodoCategoria h : hijos) {
            Candidato cand = h.mejorCandidato(u, perfil, vistos);   // llamada recursiva
            if (cand != null && (mejor == null || cand.puntaje > mejor.puntaje)) mejor = cand;
        }
        return mejor;
    }
}
