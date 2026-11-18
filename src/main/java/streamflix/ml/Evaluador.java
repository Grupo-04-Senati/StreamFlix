package streamflix.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import streamflix.modelo.Historial;

/**
 * VALIDACIÓN CRUZADA (k-fold) y métricas: exactitud, precisión, recall, F1, y además
 * satisfacción y tiempo de visualización de las recomendaciones top-K (comparación con el sistema actual).
 */
public class Evaluador {

    public static class Resultado {
        public final String nombre;
        public final boolean usaComportamiento;
        public int tp, fp, tn, fn;
        public final double[] exactitudPorFold;
        public double sumaSatisfaccion, sumaMinutos;
        public int usuariosTopK;

        Resultado(String nombre, boolean usaComportamiento, int folds) {
            this.nombre = nombre;
            this.usaComportamiento = usaComportamiento;
            this.exactitudPorFold = new double[folds];
        }

        public double exactitud() { return div(tp + tn, tp + tn + fp + fn); }
        public double precision() { return div(tp, tp + fp); }
        public double recall() { return div(tp, tp + fn); }
        public double f1() {
            double p = precision(), r = recall();
            return (p + r) == 0 ? 0 : 2 * p * r / (p + r);
        }
        public double satisfaccion() { return div(sumaSatisfaccion, usuariosTopK); }
        public double minutosTopK() { return div(sumaMinutos, usuariosTopK); }

        public double desviacionExactitud() {
            double media = 0;
            for (double v : exactitudPorFold) media += v;
            media /= exactitudPorFold.length;
            double s = 0;
            for (double v : exactitudPorFold) s += (v - media) * (v - media);
            return Math.sqrt(s / exactitudPorFold.length);
        }

        private static double div(double a, double b) { return b == 0 ? 0 : a / b; }
    }

    /**
     * @param fabricas cada una recibe el conjunto de entrenamiento y devuelve un modelo ya entrenado
     */
    public static List<Resultado> validacionCruzada(List<Historial> datos, int k, long semilla, int topK,
                                                    List<Function<List<Historial>, Modelo>> fabricas) {
        List<Historial> mezclados = new ArrayList<>(datos);
        Collections.shuffle(mezclados, new Random(semilla));

        Map<String, Resultado> resultados = new LinkedHashMap<>();
        for (int fold = 0; fold < k; fold++) {
            List<Historial> entrenamiento = new ArrayList<>();
            List<Historial> prueba = new ArrayList<>();
            for (int i = 0; i < mezclados.size(); i++) {
                if (i % k == fold) prueba.add(mezclados.get(i)); else entrenamiento.add(mezclados.get(i));
            }
            for (Function<List<Historial>, Modelo> fabrica : fabricas) {
                Modelo modelo = fabrica.apply(entrenamiento);
                final int f = fold;
                Resultado r = resultados.computeIfAbsent(modelo.nombre(),
                        n -> new Resultado(n, modelo.usaComportamiento(), k));

                int aciertos = 0;
                for (Historial h : prueba) {
                    boolean prediccion = modelo.puntaje(h) >= modelo.umbral();
                    boolean real = h.isLeGusto();
                    if (prediccion && real) r.tp++;
                    else if (prediccion) r.fp++;
                    else if (real) r.fn++;
                    else r.tn++;
                    if (prediccion == real) aciertos++;
                }
                r.exactitudPorFold[f] = aciertos / (double) prueba.size();

                if (!modelo.usaComportamiento()) acumularTopK(modelo, prueba, topK, r);
            }
        }
        return new ArrayList<>(resultados.values());
    }

    /** Por cada usuario ordena sus contenidos de prueba por puntaje y mide el top-K real. */
    private static void acumularTopK(Modelo modelo, List<Historial> prueba, int topK, Resultado r) {
        Map<String, List<Historial>> porUsuario = new LinkedHashMap<>();
        for (Historial h : prueba) {
            porUsuario.computeIfAbsent(h.getUsuario().getId(), x -> new ArrayList<>()).add(h);
        }
        for (List<Historial> lista : porUsuario.values()) {
            if (lista.size() <= topK) continue;     // con pocos casos no hay "selección" que evaluar
            List<Historial> ordenada = new ArrayList<>(lista);
            ordenada.sort((a, b) -> Double.compare(modelo.puntaje(b), modelo.puntaje(a)));
            double likes = 0, minutos = 0;
            for (int i = 0; i < topK; i++) {
                likes += ordenada.get(i).isLeGusto() ? 1 : 0;
                minutos += ordenada.get(i).getMinutosVistos();
            }
            r.sumaSatisfaccion += likes / topK;
            r.sumaMinutos += minutos / topK;
            r.usuariosTopK++;
        }
    }

    public static void imprimir(List<Resultado> resultados, int k, int topK) {
        System.out.println("Validación cruzada de " + k + " bloques (cada dato se usa para probar exactamente una vez)");
        System.out.println();
        System.out.printf("%-30s %-14s %-10s %-8s %-6s%n", "Modelo", "Exactitud", "Precisión", "Recall", "F1");
        System.out.println("-------------------------------------------------------------------------------");
        for (Resultado r : resultados) {
            System.out.printf("%-30s %5.1f%% ±%4.1f   %5.1f%%    %5.1f%%   %.2f%n",
                    r.nombre, r.exactitud() * 100, r.desviacionExactitud() * 100,
                    r.precision() * 100, r.recall() * 100, r.f1());
        }
        System.out.println();
        System.out.println("Calidad de las recomendaciones (top-" + topK + " por usuario, sobre datos de prueba)");
        System.out.println();
        System.out.printf("%-30s %-24s %-22s%n", "Modelo", "Satisfacción (% gustó)", "Minutos vistos (prom.)");
        System.out.println("-------------------------------------------------------------------------------");
        for (Resultado r : resultados) {
            if (r.usaComportamiento) continue;
            System.out.printf("%-30s %8.1f%%                %6.1f min%n",
                    r.nombre, r.satisfaccion() * 100, r.minutosTopK());
        }
    }
}
