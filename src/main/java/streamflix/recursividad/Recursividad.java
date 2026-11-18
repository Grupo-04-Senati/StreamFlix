package streamflix.recursividad;

import java.util.List;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;

/**
 * Funciones RECURSIVAS sobre las listas de datos (caso base + caso recursivo).
 */
public class Recursividad {

    /** Suma los minutos vistos desde la posición i hasta el final de la lista. */
    public static int sumarMinutos(List<Historial> lista, int i) {
        if (i >= lista.size()) return 0;                                   // caso base
        return lista.get(i).getMinutosVistos() + sumarMinutos(lista, i + 1); // caso recursivo
    }

    /** Cuenta cuántas interacciones tienen le_gusto = 1 (variable objetivo). */
    public static int contarLeGusto(List<Historial> lista, int i) {
        if (i >= lista.size()) return 0;
        return (lista.get(i).isLeGusto() ? 1 : 0) + contarLeGusto(lista, i + 1);
    }

    /** Búsqueda recursiva de un contenido por id. Devuelve null si no existe. */
    public static Contenido buscarContenido(List<Contenido> lista, String id, int i) {
        if (i >= lista.size()) return null;
        if (lista.get(i).getId().equals(id)) return lista.get(i);
        return buscarContenido(lista, id, i + 1);
    }

    /** Promedio de valoración calculado recursivamente (suma / cantidad). */
    public static double promedioValoracion(List<Historial> lista) {
        if (lista.isEmpty()) return 0;
        return sumarValoracion(lista, 0) / (double) lista.size();
    }

    private static int sumarValoracion(List<Historial> lista, int i) {
        if (i >= lista.size()) return 0;
        return lista.get(i).getValoracion() + sumarValoracion(lista, i + 1);
    }
}
