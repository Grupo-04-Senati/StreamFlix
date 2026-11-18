package streamflix.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ÁRBOL DE DECISIÓN propio (tipo CART con índice de Gini) implementado con RECURSIVIDAD:
 * construir(), probabilidad(), imprimir(), contarNodos() y profundidad() se llaman a sí mismos.
 * No usa librerías externas.
 */
public class ArbolDecision {

    public static class Nodo {
        int caracteristica = -1;     // índice de la característica con la que pregunta
        double umbral;               // pregunta: valor <= umbral ?
        double probabilidad;         // en hojas: probabilidad de "le gusta"
        int muestras;
        Nodo menorIgual;             // rama "Sí"
        Nodo mayor;                  // rama "No"

        boolean esHoja() { return menorIgual == null; }
    }

    private static final int MIN_MUESTRAS_HOJA = 3;

    private final String[] nombres;
    private final int maxProfundidad;
    private final int minMuestras;
    private Nodo raiz;
    private double[] importancia;

    public ArbolDecision(String[] nombresCaracteristicas, int maxProfundidad, int minMuestras) {
        this.nombres = nombresCaracteristicas;
        this.maxProfundidad = maxProfundidad;
        this.minMuestras = minMuestras;
    }

    public void entrenar(List<double[]> x, List<Boolean> y) {
        importancia = new double[nombres.length];
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < x.size(); i++) indices.add(i);
        raiz = construir(x, y, indices, 0);
    }

    // ---------- construcción recursiva ----------
    private Nodo construir(List<double[]> x, List<Boolean> y, List<Integer> idx, int prof) {
        int n = idx.size();
        int pos = 0;
        for (int i : idx) if (y.get(i)) pos++;

        Nodo nodo = new Nodo();
        nodo.muestras = n;
        nodo.probabilidad = (pos + 1.0) / (n + 2.0);

        // CASOS BASE: profundidad máxima, pocas muestras o nodo puro
        if (prof >= maxProfundidad || n < minMuestras || pos == 0 || pos == n) return nodo;

        double giniPadre = gini(pos, n);
        double mejorGini = giniPadre;
        int mejorF = -1;
        double mejorU = 0;

        for (int f = 0; f < nombres.length; f++) {
            final int ff = f;
            List<Integer> ordenado = new ArrayList<>(idx);
            Collections.sort(ordenado, (a, b) -> Double.compare(x.get(a)[ff], x.get(b)[ff]));
            int posIzq = 0;
            for (int k = 0; k < n - 1; k++) {
                int i = ordenado.get(k);
                if (y.get(i)) posIzq++;
                double v = x.get(i)[f];
                double vSig = x.get(ordenado.get(k + 1))[f];
                if (v == vSig) continue;
                int nIzq = k + 1;
                int nDer = n - nIzq;
                if (nIzq < MIN_MUESTRAS_HOJA || nDer < MIN_MUESTRAS_HOJA) continue;
                double g = (nIzq * gini(posIzq, nIzq) + nDer * gini(pos - posIzq, nDer)) / n;
                if (g < mejorGini - 1e-12) {
                    mejorGini = g;
                    mejorF = f;
                    mejorU = (v + vSig) / 2.0;
                }
            }
        }
        if (mejorF < 0) return nodo;   // ninguna pregunta mejora -> hoja

        importancia[mejorF] += n * (giniPadre - mejorGini);

        List<Integer> izq = new ArrayList<>();
        List<Integer> der = new ArrayList<>();
        for (int i : idx) {
            if (x.get(i)[mejorF] <= mejorU) izq.add(i); else der.add(i);
        }
        nodo.caracteristica = mejorF;
        nodo.umbral = mejorU;
        nodo.menorIgual = construir(x, y, izq, prof + 1);    // llamada recursiva
        nodo.mayor = construir(x, y, der, prof + 1);         // llamada recursiva
        return nodo;
    }

    private static double gini(int pos, int n) {
        double p = pos / (double) n;
        return 2 * p * (1 - p);
    }

    // ---------- predicción recursiva ----------
    public double probabilidad(double[] x) {
        return probabilidad(raiz, x);
    }

    private double probabilidad(Nodo nodo, double[] x) {
        if (nodo.esHoja()) return nodo.probabilidad;
        return x[nodo.caracteristica] <= nodo.umbral
                ? probabilidad(nodo.menorIgual, x)
                : probabilidad(nodo.mayor, x);
    }

    // ---------- impresión recursiva ----------
    public void imprimir() {
        imprimir(raiz, "", "");
    }

    private void imprimir(Nodo nodo, String sangria, String etiqueta) {
        if (nodo.esHoja()) {
            String decision = nodo.probabilidad >= 0.5 ? "RECOMENDAR" : "NO RECOMENDAR";
            System.out.printf("%s%s→ %s  (prob. de gustar %.0f%%, %d casos)%n",
                    sangria, etiqueta, decision, nodo.probabilidad * 100, nodo.muestras);
            return;
        }
        System.out.printf("%s%s¿%s <= %.2f ?%n", sangria, etiqueta, nombres[nodo.caracteristica], nodo.umbral);
        imprimir(nodo.menorIgual, sangria + "    ", "Sí: ");
        imprimir(nodo.mayor, sangria + "    ", "No: ");
    }

    // ---------- utilidades recursivas ----------
    public int contarNodos() {
        return contarNodos(raiz);
    }

    private int contarNodos(Nodo n) {
        if (n == null) return 0;
        return 1 + contarNodos(n.menorIgual) + contarNodos(n.mayor);
    }

    public int profundidad() {
        return profundidad(raiz);
    }

    private int profundidad(Nodo n) {
        if (n == null || n.esHoja()) return 0;
        return 1 + Math.max(profundidad(n.menorIgual), profundidad(n.mayor));
    }

    /** Importancia relativa (suma 1.0) de cada característica en las decisiones del árbol. */
    public double[] importancias() {
        double suma = 0;
        for (double v : importancia) suma += v;
        double[] r = new double[importancia.length];
        for (int i = 0; i < r.length; i++) r[i] = suma == 0 ? 0 : importancia[i] / suma;
        return r;
    }

    public String[] getNombres() {
        return nombres;
    }
}
