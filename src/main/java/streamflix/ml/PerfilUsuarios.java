package streamflix.ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * Resume el historial de entrenamiento en conteos [me gusta, total]:
 * por usuario y característica (género, director, idioma, tipo), por usuario global,
 * por contenido y por género. Sirve para medir la "afinidad" de un usuario.
 */
public class PerfilUsuarios {
    public static final String[] DIMENSIONES = {"genero", "director", "idioma", "tipo"};

    private final Map<String, int[]> porUsuarioYCaracteristica = new HashMap<>();
    private final Map<String, int[]> porUsuario = new HashMap<>();
    private final Map<String, int[]> porContenido = new HashMap<>();
    private final Map<String, int[]> porGenero = new HashMap<>();
    private int likes = 0;
    private int total = 0;

    public PerfilUsuarios(List<Historial> entrenamiento) {
        for (Historial h : entrenamiento) {
            boolean like = h.isLeGusto();
            Usuario u = h.getUsuario();
            Contenido c = h.getContenido();
            for (String dim : DIMENSIONES) {
                sumar(porUsuarioYCaracteristica, u.getId() + "|" + clave(c, dim), like);
            }
            sumar(porUsuario, u.getId(), like);
            sumar(porContenido, c.getId(), like);
            sumar(porGenero, c.getGenero(), like);
            total++;
            if (like) likes++;
        }
    }

    private static void sumar(Map<String, int[]> mapa, String clave, boolean like) {
        int[] a = mapa.computeIfAbsent(clave, k -> new int[2]);
        if (like) a[0]++;
        a[1]++;
    }

    private static String clave(Contenido c, String dim) {
        switch (dim) {
            case "genero":   return c.getGenero();
            case "director": return c.getDirector();
            case "idioma":   return c.getIdiomaOriginal();
            case "tipo":     return c.getTipo();
            default: throw new IllegalArgumentException("Dimensión desconocida: " + dim);
        }
    }

    /** Tasa suavizada (Laplace) de me gusta: (likes + 1) / (total + 2). */
    private static double tasa(int[] a) {
        int l = a == null ? 0 : a[0];
        int t = a == null ? 0 : a[1];
        return (l + 1.0) / (t + 2.0);
    }

    /** Afinidad del usuario con una característica del contenido (género, director, idioma, tipo). */
    public double afinidad(Usuario u, Contenido c, String dimension) {
        return tasa(porUsuarioYCaracteristica.get(u.getId() + "|" + clave(c, dimension)));
    }

    /** Tasa de me gusta global del usuario. */
    public double afinidadUsuario(Usuario u) {
        return tasa(porUsuario.get(u.getId()));
    }

    /** Tasa de me gusta de un contenido entre todos los usuarios (popularidad). */
    public double tasaContenido(Contenido c) {
        return tasa(porContenido.get(c.getId()));
    }

    /** Tasa de me gusta de un género entre todos los usuarios. */
    public double tasaGenero(String genero) {
        return tasa(porGenero.get(genero));
    }

    public double tasaGlobal() {
        return total == 0 ? 0.5 : likes / (double) total;
    }
}
