package streamflix.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * ÁRBOL DE DECISIÓN PROPUESTO EN LA DIAPOSITIVA (reglas del negocio, no aprendido).
 *
 * <pre>
 * ¿Coincide con el género preferido en su historial?
 *   SÍ -> ¿El tiempo de visualización promedio supera el 70%?
 *           SÍ -> ¿Usuarios similares valoraron la película con >= 4 estrellas?
 *                   SÍ -> Recomendar (Alta Prioridad)
 *                   NO -> ¿Coincide director o idioma original preferido?
 *                           SÍ -> Recomendar (Por Contenido)
 *                           NO -> No recomendar
 *           NO -> No recomendar
 *   NO -> ¿Está en el Top Tendencias / Ranking global?
 *           SÍ -> ¿Es acorde a la edad y país del usuario?
 *                   SÍ -> Recomendar (Exploración)
 *                   NO -> No recomendar
 *           NO -> No recomendar
 * </pre>
 *
 * La clase devuelve la categoría, la ruta de decisión (para que el administrador
 * pueda verificarla) y una confianza. El porcentaje que se muestra al administrador
 * es el del modelo híbrido; este árbol aporta el patrón y la etiqueta.
 */
public class ArbolReglas {

    private static final int TOP_TENDENCIAS = 10;
    private static final double UMBRAL_TIEMPO = 0.70;
    private static final double UMBRAL_ESTRELLAS = 4.0;
    private static final int TOLERANCIA_EDAD = 15;

    /** Resultado de evaluar un par (usuario, contenido) con las reglas. */
    public static class Resultado {
        public final boolean recomendar;
        public final String categoria;    // Alta Prioridad / Por Contenido / Exploración / No recomendar
        public final double confianza;    // 0.0 a 1.0 (fuerza de la regla)
        public final List<String> ruta;   // pasos con los valores reales

        Resultado(boolean recomendar, String categoria, double confianza, List<String> ruta) {
            this.recomendar = recomendar;
            this.categoria = categoria;
            this.confianza = confianza;
            this.ruta = ruta;
        }
    }

    /** Datos del usuario que usa el árbol para responder las preguntas. */
    public static class Resumen {
        public final Usuario usuario;
        public final int interacciones;
        public final int meGusta;
        public final String generoFavorito;
        public final double porcentajeGeneroFavorito;   // 0.0 a 1.0 (de sus "me gusta")
        public final double porcentajeVisto;            // 0.0 a 1.0 (promedio de todo su historial)
        public final double porcentajeVistoGenero;      // 0.0 a 1.0 (promedio en su género preferido)
        public final String directorPreferido;
        public final String idiomaPreferido;
        public final int vecinosSimilares;
        public final int totalTendencias;

        Resumen(Usuario usuario, int interacciones, int meGusta, String generoFavorito,
                double porcentajeGeneroFavorito, double porcentajeVisto, double porcentajeVistoGenero,
                String directorPreferido, String idiomaPreferido, int vecinosSimilares,
                int totalTendencias) {
            this.usuario = usuario;
            this.interacciones = interacciones;
            this.meGusta = meGusta;
            this.generoFavorito = generoFavorito;
            this.porcentajeGeneroFavorito = porcentajeGeneroFavorito;
            this.porcentajeVisto = porcentajeVisto;
            this.porcentajeVistoGenero = porcentajeVistoGenero;
            this.directorPreferido = directorPreferido;
            this.idiomaPreferido = idiomaPreferido;
            this.vecinosSimilares = vecinosSimilares;
            this.totalTendencias = totalTendencias;
        }
    }

    private final List<Historial> historial;
    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    private final Map<String, Resumen> cacheResumen = new HashMap<>();
    private final PerfilUsuarios perfil;
    private final FiltradoColaborativo colaborativo;
    private final Set<String> tendencias = new HashSet<>();

    public ArbolReglas(List<Historial> historial) {
        this.historial = historial;
        this.perfil = new PerfilUsuarios(historial);
        this.colaborativo = new FiltradoColaborativo(historial, perfil, 5);
        for (Historial h : historial) usuarios.put(h.getUsuario().getId(), h.getUsuario());
        calcularTendencias();
    }

    // ---------------------------------------------------------------- tendencias
    private void calcularTendencias() {
        Map<String, Integer> minutos = new HashMap<>();
        for (Historial h : historial) {
            minutos.merge(h.getContenido().getId(), h.getMinutosVistos(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> orden = new ArrayList<>(minutos.entrySet());
        orden.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(TOP_TENDENCIAS, orden.size()); i++) {
            tendencias.add(orden.get(i).getKey());
        }
    }

    // ---------------------------------------------------------------- resumen
    public Resumen resumen(Usuario u) {
        Resumen cache = cacheResumen.get(u.getId());
        if (cache != null) return cache;

        List<Historial> hs = historialDe(u);
        int likes = 0;
        double sumaVisto = 0;
        Map<String, Integer> porGenero = new LinkedHashMap<>();
        Map<String, Integer> minutosPorGenero = new LinkedHashMap<>();
        Map<String, double[]> vistoPorGenero = new LinkedHashMap<>();
        Map<String, Integer> porDirector = new LinkedHashMap<>();
        Map<String, Integer> porIdioma = new LinkedHashMap<>();

        for (Historial h : hs) {
            double visto = h.getPorcentajeVisto();
            sumaVisto += visto;
            minutosPorGenero.merge(h.getContenido().getGenero(), h.getMinutosVistos(), Integer::sum);
            double[] acumulado = vistoPorGenero.computeIfAbsent(
                    h.getContenido().getGenero(), k -> new double[2]);
            acumulado[0] += visto;
            acumulado[1]++;
            if (h.isLeGusto()) {
                likes++;
                sumar(porGenero, h.getContenido().getGenero());
                sumar(porDirector, h.getContenido().getDirector());
                sumar(porIdioma, h.getContenido().getIdiomaOriginal());
            }
        }

        String genero = masFrecuente(porGenero);
        if (genero == null) genero = masFrecuente(minutosPorGenero);   // sin "me gusta": el más visto
        double pctGenero = (genero == null || likes == 0) ? 0 : porGenero.get(genero) / (double) likes;
        double pctVisto = hs.isEmpty() ? 0 : sumaVisto / hs.size();
        double pctVistoGenero = pctVisto;
        if (genero != null && vistoPorGenero.containsKey(genero)) {
            double[] a = vistoPorGenero.get(genero);
            pctVistoGenero = a[0] / a[1];
        }

        int vecinos = 0;
        for (Usuario otro : usuarios.values()) {
            if (otro.getId().equals(u.getId())) continue;
            if (colaborativo.similitud(u.getId(), otro.getId()) > 0) vecinos++;
        }

        return new Resumen(u, hs.size(), likes, genero, pctGenero, pctVisto, pctVistoGenero,
                masFrecuente(porDirector), masFrecuente(porIdioma), vecinos, tendencias.size());
    }

    // ---------------------------------------------------------------- evaluación
    public Resultado evaluar(Usuario u, Contenido c) {
        Resumen r = resumen(u);
        List<String> ruta = new ArrayList<>();

        boolean generoOk = r.generoFavorito != null
                && c.getGenero().equalsIgnoreCase(r.generoFavorito);
        ruta.add("¿Coincide con el género preferido ("
                + (r.generoFavorito == null ? "sin historial" : r.generoFavorito) + ")? → "
                + (generoOk ? "SÍ" : "NO"));

        if (generoOk) {
            boolean tiempoOk = r.porcentajeVistoGenero > UMBRAL_TIEMPO;
            ruta.add(String.format("¿El tiempo de visualización promedio en su género preferido (%.1f%%) supera el 70%%? → %s",
                    r.porcentajeVistoGenero * 100, tiempoOk ? "SÍ" : "NO"));
            if (tiempoOk) {
                double valoracion = valoracionSimilares(u, c);
                boolean estrellas = valoracion >= UMBRAL_ESTRELLAS;
                ruta.add(String.format("¿Usuarios similares valoraron la película con %.1f★ (>= 4★)? → %s",
                        valoracion, estrellas ? "SÍ" : "NO"));
                if (estrellas) {
                    return new Resultado(true, "Alta Prioridad", 0.90, ruta);
                }
                boolean afinidad = coincideDirectorOIdioma(r, c);
                ruta.add("¿Coincide el director (" + texto(r.directorPreferido)
                        + ") o el idioma original (" + texto(r.idiomaPreferido) + ")? → "
                        + (afinidad ? "SÍ" : "NO"));
                if (afinidad) {
                    return new Resultado(true, "Por Contenido", 0.75, ruta);
                }
                return new Resultado(false, "No recomendar", 0.25, ruta);
            }
            return new Resultado(false, "No recomendar", 0.20, ruta);
        }

        boolean tendencia = tendencias.contains(c.getId());
        ruta.add("¿Está en el Top " + TOP_TENDENCIAS + " Tendencias / Ranking global? → "
                + (tendencia ? "SÍ" : "NO"));
        if (!tendencia) {
            return new Resultado(false, "No recomendar", 0.20, ruta);
        }

        boolean acorde = acordeEdadYPais(u, c);
        ruta.add("¿Es acorde a la edad y país del usuario? → " + (acorde ? "SÍ" : "NO"));
        if (acorde) {
            return new Resultado(true, "Exploración", 0.60, ruta);
        }
        return new Resultado(false, "No recomendar", 0.30, ruta);
    }

    // ---------------------------------------------------------------- reglas auxiliares
    /** Valoración media (ponderada por similitud) de los usuarios parecidos que vieron el título. */
    public double valoracionSimilares(Usuario u, Contenido c) {
        double suma = 0;
        double peso = 0;
        for (Historial h : historial) {
            if (!h.getContenido().getId().equals(c.getId())) continue;
            if (h.getUsuario().getId().equals(u.getId())) continue;
            double s = colaborativo.similitud(u.getId(), h.getUsuario().getId());
            if (s <= 0) continue;
            suma += s * h.getValoracion();
            peso += s;
        }
        return peso == 0 ? c.getValoracionPromedio() : suma / peso;
    }

    private static boolean coincideDirectorOIdioma(Resumen r, Contenido c) {
        return (r.directorPreferido != null && r.directorPreferido.equalsIgnoreCase(c.getDirector()))
                || (r.idiomaPreferido != null && r.idiomaPreferido.equalsIgnoreCase(c.getIdiomaOriginal()));
    }

    /**
     * "Acorde a la edad y país" con los datos disponibles (el catálogo no trae clasificación por edad):
     *  - edad: la audiencia que le dio "me gusta" al título (o a su género) tiene una edad media
     *    que no se aleja más de 15 años de la edad del usuario;
     *  - país: entre los usuarios del país del usuario que vieron el título, al menos la mitad le dio "me gusta"
     *    (si nadie del país lo vio, no se descarta).
     */
    public boolean acordeEdadYPais(Usuario u, Contenido c) {
        double sumaEdad = 0;
        int n = 0;
        for (Historial h : historial) {
            if (h.getContenido().getId().equals(c.getId()) && h.isLeGusto()) {
                sumaEdad += h.getUsuario().getEdad();
                n++;
            }
        }
        if (n == 0) {
            for (Historial h : historial) {
                if (h.getContenido().getGenero().equalsIgnoreCase(c.getGenero()) && h.isLeGusto()) {
                    sumaEdad += h.getUsuario().getEdad();
                    n++;
                }
            }
        }
        double edadMedia = n == 0 ? 30 : sumaEdad / n;
        boolean edadOk = Math.abs(u.getEdad() - edadMedia) <= TOLERANCIA_EDAD;

        int delPais = 0;
        int gustaronPais = 0;
        for (Historial h : historial) {
            if (!h.getContenido().getId().equals(c.getId())) continue;
            if (!h.getUsuario().getPais().equalsIgnoreCase(u.getPais())) continue;
            delPais++;
            if (h.isLeGusto()) gustaronPais++;
        }
        boolean paisOk = delPais == 0 || gustaronPais * 2 >= delPais;

        return edadOk && paisOk;
    }

    /** Detalle numérico de la regla de edad/país, para mostrarlo al administrador. */
    public String detalleEdadYPais(Usuario u, Contenido c) {
        double sumaEdad = 0;
        int n = 0;
        for (Historial h : historial) {
            if (h.getContenido().getId().equals(c.getId()) && h.isLeGusto()) {
                sumaEdad += h.getUsuario().getEdad();
                n++;
            }
        }
        if (n == 0) {
            for (Historial h : historial) {
                if (h.getContenido().getGenero().equalsIgnoreCase(c.getGenero()) && h.isLeGusto()) {
                    sumaEdad += h.getUsuario().getEdad();
                    n++;
                }
            }
        }
        double edadMedia = n == 0 ? 30 : sumaEdad / n;

        int delPais = 0;
        int gustaronPais = 0;
        for (Historial h : historial) {
            if (!h.getContenido().getId().equals(c.getId())) continue;
            if (!h.getUsuario().getPais().equalsIgnoreCase(u.getPais())) continue;
            delPais++;
            if (h.isLeGusto()) gustaronPais++;
        }
        return String.format("   (edad media de la audiencia: %.0f años; tu edad: %d; "
                        + "aceptación en tu país: %s)",
                edadMedia, u.getEdad(),
                delPais == 0 ? "sin datos" : gustaronPais + "/" + delPais + " (" 
                        + Math.round(100.0 * gustaronPais / delPais) + "%)");
    }

    public boolean esTendencia(Contenido c) {
        return tendencias.contains(c.getId());
    }

    public Set<String> getTendencias() {
        return tendencias;
    }

    // ---------------------------------------------------------------- utilidades
    private List<Historial> historialDe(Usuario u) {
        List<Historial> r = new ArrayList<>();
        for (Historial h : historial) {
            if (h.getUsuario().getId().equals(u.getId())) r.add(h);
        }
        return r;
    }

    private static void sumar(Map<String, Integer> mapa, String clave) {
        mapa.put(clave, mapa.getOrDefault(clave, 0) + 1);
    }

    private static String masFrecuente(Map<String, Integer> mapa) {
        String mejor = null;
        int max = 0;
        for (Map.Entry<String, Integer> e : mapa.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                mejor = e.getKey();
            }
        }
        return mejor;
    }

    private static String texto(String s) {
        return s == null ? "sin dato" : s;
    }
}
