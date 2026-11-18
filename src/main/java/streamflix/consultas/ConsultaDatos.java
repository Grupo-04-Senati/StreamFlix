package streamflix.consultas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import streamflix.modelo.BaseDatos;
import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * CONSULTA: métodos que RECORREN las listas cargadas y responden preguntas
 * sobre el dataset (filtros, conteos, promedios y rankings).
 */
public class ConsultaDatos {
    private final BaseDatos bd;

    public ConsultaDatos(BaseDatos bd) {
        this.bd = bd;
    }

    /** Todo lo que vio un usuario (búsqueda por nombre). */
    public List<Historial> historialDe(String nombre) {
        List<Historial> resultado = new ArrayList<>();
        for (Historial h : bd.getHistorial()) {
            if (h.getUsuario().getNombre().equalsIgnoreCase(nombre.trim())) resultado.add(h);
        }
        return resultado;
    }

    /** Contenido que le gustó a un usuario (le_gusto = 1). */
    public List<Contenido> contenidoQueLeGustoA(String nombre) {
        List<Contenido> resultado = new ArrayList<>();
        for (Historial h : historialDe(nombre)) {
            if (h.isLeGusto()) resultado.add(h.getContenido());
        }
        return resultado;
    }

    /** Género con más "me gusta" de un usuario (null si no tiene historial). */
    public String generoFavoritoDe(String nombre) {
        Map<String, Integer> likesPorGenero = new HashMap<>();
        for (Historial h : historialDe(nombre)) {
            if (h.isLeGusto()) {
                String g = h.getContenido().getGenero();
                likesPorGenero.put(g, likesPorGenero.getOrDefault(g, 0) + 1);
            }
        }
        String mejor = null;
        int max = 0;
        for (Map.Entry<String, Integer> e : likesPorGenero.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); mejor = e.getKey(); }
        }
        return mejor;
    }

    /** Promedio de valoración (1-5) que da un usuario. */
    public double promedioValoracionDe(String nombre) {
        double suma = 0;
        int n = 0;
        for (Historial h : historialDe(nombre)) {
            suma += h.getValoracion();
            n++;
        }
        return n == 0 ? 0 : suma / n;
    }

    /** Minutos totales que vio un usuario. */
    public int minutosTotalesDe(String nombre) {
        int total = 0;
        for (Historial h : historialDe(nombre)) total += h.getMinutosVistos();
        return total;
    }

    /** Cantidad de usuarios por país. */
    public Map<String, Integer> usuariosPorPais() {
        Map<String, Integer> conteo = new TreeMap<>();
        for (Usuario u : bd.getUsuarios()) {
            conteo.put(u.getPais(), conteo.getOrDefault(u.getPais(), 0) + 1);
        }
        return conteo;
    }

    /** Contenido de un género. */
    public List<Contenido> contenidoPorGenero(String genero) {
        List<Contenido> r = new ArrayList<>();
        for (Contenido c : bd.getContenidos()) {
            if (c.getGenero().equalsIgnoreCase(genero.trim())) r.add(c);
        }
        return r;
    }

    /** ¿Cuántos usuarios distintos vieron al menos un contenido de ese género? */
    public int usuariosQueVieronGenero(String genero) {
        List<String> ids = new ArrayList<>();
        for (Historial h : bd.getHistorial()) {
            if (h.getContenido().getGenero().equalsIgnoreCase(genero.trim())
                    && !ids.contains(h.getUsuario().getId())) {
                ids.add(h.getUsuario().getId());
            }
        }
        return ids.size();
    }

    /** Interacciones donde se vio más de X minutos. */
    public List<Historial> vistosMasDe(int minutos) {
        List<Historial> r = new ArrayList<>();
        for (Historial h : bd.getHistorial()) {
            if (h.getMinutosVistos() > minutos) r.add(h);
        }
        return r;
    }

    /** Porcentaje de "me gusta" por género (tasa de aprobación). */
    public Map<String, Double> tasaDeLikePorGenero() {
        Map<String, int[]> acum = new TreeMap<>();   // [likes, total]
        for (Historial h : bd.getHistorial()) {
            int[] a = acum.computeIfAbsent(h.getContenido().getGenero(), k -> new int[2]);
            if (h.isLeGusto()) a[0]++;
            a[1]++;
        }
        Map<String, Double> tasas = new TreeMap<>();
        for (Map.Entry<String, int[]> e : acum.entrySet()) {
            tasas.put(e.getKey(), 100.0 * e.getValue()[0] / e.getValue()[1]);
        }
        return tasas;
    }

    /** Minutos totales vistos por género. */
    public Map<String, Integer> minutosPorGenero() {
        Map<String, Integer> m = new TreeMap<>();
        for (Historial h : bd.getHistorial()) {
            String g = h.getContenido().getGenero();
            m.put(g, m.getOrDefault(g, 0) + h.getMinutosVistos());
        }
        return m;
    }

    /** Top N de contenido más visto (por minutos acumulados). */
    public List<Map.Entry<Contenido, Integer>> masVistos(int n) {
        Map<Contenido, Integer> minutos = new HashMap<>();
        for (Historial h : bd.getHistorial()) {
            minutos.put(h.getContenido(), minutos.getOrDefault(h.getContenido(), 0) + h.getMinutosVistos());
        }
        List<Map.Entry<Contenido, Integer>> lista = new ArrayList<>(minutos.entrySet());
        Collections.sort(lista, (a, b) -> b.getValue() - a.getValue());
        return lista.subList(0, Math.min(n, lista.size()));
    }

    /** Usuarios dentro de un rango de edad. */
    public List<Usuario> usuariosPorEdad(int min, int max) {
        List<Usuario> r = new ArrayList<>();
        for (Usuario u : bd.getUsuarios()) {
            if (u.getEdad() >= min && u.getEdad() <= max) r.add(u);
        }
        return r;
    }
}
