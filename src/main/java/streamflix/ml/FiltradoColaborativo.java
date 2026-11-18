package streamflix.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * FILTRADO COLABORATIVO usuario-usuario:
 * "a usuarios parecidos a ti les gustó esto, por lo tanto probablemente a ti también".
 * La similitud entre dos usuarios compara sus gustos por género (qué géneros les gustan o no).
 */
public class FiltradoColaborativo implements Modelo {
    private final Map<String, Map<String, Integer>> votos = new HashMap<>();          // usuario -> (contenido -> +1/-1)
    private final Map<String, List<String>> votantesPorContenido = new HashMap<>();  // contenido -> usuarios
    private final Map<String, Double> cacheSimilitud = new HashMap<>();
    private final Map<String, Map<String, double[]>> gustoPorGenero = new HashMap<>();   // usuario -> (género -> [likes, total])
    private final PerfilUsuarios perfil;
    private final int vecinos;

    public FiltradoColaborativo(List<Historial> entrenamiento, PerfilUsuarios perfil, int vecinos) {
        this.perfil = perfil;
        this.vecinos = vecinos;
        for (Historial h : entrenamiento) {
            String u = h.getUsuario().getId();
            String c = h.getContenido().getId();
            votos.computeIfAbsent(u, k -> new HashMap<>()).put(c, h.isLeGusto() ? 1 : -1);
            votantesPorContenido.computeIfAbsent(c, k -> new ArrayList<>()).add(u);
            double[] a = gustoPorGenero.computeIfAbsent(u, k -> new HashMap<>())
                    .computeIfAbsent(h.getContenido().getGenero(), k -> new double[2]);
            if (h.isLeGusto()) a[0]++;
            a[1]++;
        }
    }

    @Override
    public String nombre() {
        return "Filtrado colaborativo";
    }

    /**
     * Similitud entre dos usuarios en [-1, 1]: coseno entre sus "perfiles de gusto por género"
     * (tasa de me gusta por género, centrada en 0.5). Usuarios que aman/odian los mismos géneros se parecen.
     */
    public double similitud(String a, String b) {
        String clave = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
        Double guardada = cacheSimilitud.get(clave);
        if (guardada != null) return guardada;

        Map<String, double[]> ga = gustoPorGenero.get(a);
        Map<String, double[]> gb = gustoPorGenero.get(b);
        double resultado = 0;
        if (ga != null && gb != null) {
            double dot = 0, na = 0, nb = 0;
            for (Map.Entry<String, double[]> e : ga.entrySet()) {
                double[] otro = gb.get(e.getKey());
                if (otro == null) continue;
                double va = (e.getValue()[0] + 1) / (e.getValue()[1] + 2) - 0.5;
                double vb = (otro[0] + 1) / (otro[1] + 2) - 0.5;
                dot += va * vb;
                na += va * va;
                nb += vb * vb;
            }
            if (na > 0 && nb > 0) resultado = dot / Math.sqrt(na * nb);
        }
        cacheSimilitud.put(clave, resultado);
        return resultado;
    }

    @Override
    public double puntaje(Usuario u, Contenido c) {
        List<String> votantes = votantesPorContenido.get(c.getId());
        if (votantes == null) return perfil.afinidadUsuario(u);    // contenido nuevo: usar tendencia del usuario

        List<double[]> candidatos = new ArrayList<>();   // {similitud, voto 0/1}
        for (String otro : votantes) {
            if (otro.equals(u.getId())) continue;
            double s = similitud(u.getId(), otro);
            if (s > 0) candidatos.add(new double[] {s, votos.get(otro).get(c.getId()) > 0 ? 1 : 0});
        }
        if (candidatos.isEmpty()) return perfil.afinidadUsuario(u);

        Collections.sort(candidatos, (p, q) -> Double.compare(q[0], p[0]));
        double num = 0;
        double den = 0;
        for (int i = 0; i < Math.min(vecinos, candidatos.size()); i++) {
            num += candidatos.get(i)[0] * candidatos.get(i)[1];
            den += candidatos.get(i)[0];
        }
        return num / den;
    }
}
