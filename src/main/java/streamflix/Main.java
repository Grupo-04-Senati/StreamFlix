package streamflix;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

import streamflix.consultas.ConsultaDatos;
import streamflix.datos.CargadorDatos;
import streamflix.ml.ArbolDecision;
import streamflix.ml.ArbolModelo;
import streamflix.ml.BasadoEnContenido;
import streamflix.ml.Evaluador;
import streamflix.ml.FiltradoColaborativo;
import streamflix.ml.Hibrido;
import streamflix.ml.Modelo;
import streamflix.ml.PerfilUsuarios;
import streamflix.ml.SistemaActual;
import streamflix.modelo.BaseDatos;
import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Recomendacion;
import streamflix.modelo.Usuario;
import streamflix.recursividad.NodoCategoria;
import streamflix.recursividad.Recursividad;

/**
 * StreamFlix - Sistema de recomendación de películas (Java puro, sin librerías).
 * Menú de consola que muestra: carga, recorrido y consulta de datos (punto 7),
 * árbol de decisión, recursividad, recomendaciones y validación cruzada.
 */
public class Main {
    private static final String CARPETA_DATOS = "datos";
    private static final long SEMILLA = 456;

    private static BaseDatos bd;
    private static ConsultaDatos consultas;
    private static Scanner teclado;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));

        // Interfaz gráfica del punto 7: java ... Main --gui
        for (String a : args) {
            if (a.equals("--gui")) { streamflix.gui.VentanaPrincipal.main(args); return; }
        }

        String carpeta = args.length > 0 && !args[0].startsWith("--") ? args[0] : CARPETA_DATOS;
        try {
            bd = CargadorDatos.cargar(carpeta);
        } catch (IOException e) {
            System.out.println("ERROR al cargar los datos: " + e.getMessage());
            return;
        }
        consultas = new ConsultaDatos(bd);

        System.out.println("=== StreamFlix: sistema de recomendación ===");
        System.out.println("Datos cargados: " + bd.getUsuarios().size() + " usuarios, "
                + bd.getContenidos().size() + " títulos, " + bd.getHistorial().size() + " interacciones.");

        // Modo no interactivo: java ... Main --todo
        for (String a : args) {
            if (a.equals("--todo")) { ejecutarTodo(); return; }
        }

        teclado = new Scanner(System.in, "UTF-8");
        try {
            menu();
        } catch (NoSuchElementException e) {
            // entrada cerrada: salir en silencio
        }
    }

    // ------------------------------------------------------------------ menú
    private static void menu() {
        while (true) {
            System.out.println();
            System.out.println("-------------------- MENÚ --------------------");
            System.out.println(" 1. Recorrer datos cargados (usuarios, catálogo, historial)");
            System.out.println(" 2. Consultas sobre los datos");
            System.out.println(" 3. Árbol de decisión (diseño y reglas)");
            System.out.println(" 4. Recursividad (funciones y árbol de categorías)");
            System.out.println(" 5. Recomendaciones para un usuario");
            System.out.println(" 6. Validación cruzada y comparación con el sistema actual");
            System.out.println(" 7. Ejecutar todo (demostración completa)");
            System.out.println(" 0. Salir");
            System.out.print("Elige una opción: ");
            String op = teclado.nextLine().trim();
            switch (op) {
                case "1": recorrerDatos(); break;
                case "2": consultar(pedirNombre()); break;
                case "3": arbolDecision(); break;
                case "4": recursividad(); break;
                case "5": recomendar(pedirNombre()); break;
                case "6": validacion(); break;
                case "7": ejecutarTodo(); break;
                case "0": System.out.println("¡Hasta luego!"); return;
                default: System.out.println("Opción no válida.");
            }
        }
    }

    private static String pedirNombre() {
        System.out.print("Nombre del usuario (Enter = Juan): ");
        String n = teclado.nextLine().trim();
        return n.isEmpty() ? "Juan" : n;
    }

    private static void ejecutarTodo() {
        recorrerDatos();
        consultar("Juan");
        arbolDecision();
        recursividad();
        recomendar("Juan");
        validacion();
    }

    private static void titulo(String t) {
        System.out.println();
        System.out.println("==================================================================");
        System.out.println(" " + t);
        System.out.println("==================================================================");
    }

    // ------------------------------------------------------------------ 1. recorrer
    private static void recorrerDatos() {
        titulo("1. RECORRER LOS DATOS CARGADOS");
        System.out.println("-- Primeros 5 usuarios --");
        for (int i = 0; i < 5 && i < bd.getUsuarios().size(); i++) {
            System.out.println("  " + bd.getUsuarios().get(i));
        }
        System.out.println("-- Primeros 5 títulos del catálogo --");
        int mostrados = 0;
        for (Contenido c : bd.getContenidos()) {
            if (mostrados++ == 5) break;
            System.out.println("  " + c);
        }
        System.out.println("-- Primeras 5 interacciones del historial --");
        for (int i = 0; i < 5 && i < bd.getHistorial().size(); i++) {
            System.out.println("  " + bd.getHistorial().get(i));
        }
        int likes = 0;
        for (Historial h : bd.getHistorial()) if (h.isLeGusto()) likes++;
        System.out.printf("Variable objetivo 'Le gustó': %d de %d interacciones (%.1f%%)%n",
                likes, bd.getHistorial().size(), 100.0 * likes / bd.getHistorial().size());
    }

    // ------------------------------------------------------------------ 2. consultas
    private static void consultar(String nombre) {
        titulo("2. CONSULTAS SOBRE LOS DATOS");
        Usuario u = bd.buscarUsuarioPorNombre(nombre);
        if (u == null) {
            System.out.println("No existe el usuario '" + nombre + "'. Usuarios disponibles:");
            for (Usuario x : bd.getUsuarios()) System.out.print(x.getNombre() + "  ");
            System.out.println();
            return;
        }
        System.out.println("Usuario consultado: " + u);

        List<Historial> hist = consultas.historialDe(nombre);
        System.out.println("\n[a] Contenido que vio " + u.getNombre() + ": " + hist.size() + " títulos, "
                + consultas.minutosTotalesDe(nombre) + " min en total");
        for (int i = 0; i < Math.min(5, hist.size()); i++) System.out.println("    " + hist.get(i));

        List<Contenido> gustados = consultas.contenidoQueLeGustoA(nombre);
        System.out.println("\n[b] Le gustaron " + gustados.size() + " títulos. Algunos:");
        for (int i = 0; i < Math.min(5, gustados.size()); i++) System.out.println("    " + gustados.get(i));

        System.out.println("\n[c] Género favorito de " + u.getNombre() + ": " + consultas.generoFavoritoDe(nombre));
        System.out.printf("    Valoración promedio que da: %.2f / 5%n", consultas.promedioValoracionDe(nombre));

        System.out.println("\n[d] Usuarios por país: " + consultas.usuariosPorPais());

        System.out.println("\n[e] Tasa de 'me gusta' por género:");
        for (Map.Entry<String, Double> e : consultas.tasaDeLikePorGenero().entrySet()) {
            System.out.printf("    %-16s %5.1f%%%n", e.getKey(), e.getValue());
        }

        System.out.println("\n[f] Top 5 contenido más visto (minutos acumulados):");
        for (Map.Entry<Contenido, Integer> e : consultas.masVistos(5)) {
            System.out.printf("    %-28s %6d min%n", e.getKey().getTitulo(), e.getValue());
        }

        System.out.println("\n[g] Usuarios que vieron Acción: " + consultas.usuariosQueVieronGenero("Acción"));
        System.out.println("    Interacciones con más de 100 min vistos: " + consultas.vistosMasDe(100).size());
        System.out.println("    Usuarios entre 18 y 25 años: " + consultas.usuariosPorEdad(18, 25).size());
    }

    // ------------------------------------------------------------------ 3. árbol
    private static void arbolDecision() {
        titulo("3. ÁRBOL DE DECISIÓN");
        System.out.println("Variable objetivo: Le gustó (1 = sí, 0 = no)\n");

        ArbolModelo conComp = new ArbolModelo(bd.getHistorial(), true, 3, 15, SEMILLA);
        System.out.println("A) Árbol con comportamiento de visualización (tiempo visto, favoritos):\n");
        conComp.getArbol().imprimir();
        imprimirImportancias(conComp.getArbol());

        ArbolModelo previo = new ArbolModelo(bd.getHistorial(), false, 3, 15, SEMILLA);
        System.out.println("\nB) Árbol para recomendar (solo datos disponibles ANTES de ver el título):\n");
        previo.getArbol().imprimir();
        imprimirImportancias(previo.getArbol());
        System.out.println("\nNodos: " + previo.getArbol().contarNodos() + " | Profundidad: " + previo.getArbol().profundidad());
    }

    private static void imprimirImportancias(ArbolDecision arbol) {
        System.out.println("\nCaracterísticas más relevantes:");
        double[] imp = arbol.importancias();
        String[] nombres = arbol.getNombres();
        boolean[] usado = new boolean[imp.length];
        for (int rank = 0; rank < 4; rank++) {
            int mejor = -1;
            for (int i = 0; i < imp.length; i++) {
                if (!usado[i] && (mejor == -1 || imp[i] > imp[mejor])) mejor = i;
            }
            if (mejor == -1 || imp[mejor] <= 0) break;
            usado[mejor] = true;
            System.out.printf("   %d. %-20s %5.1f%%%n", rank + 1, nombres[mejor], imp[mejor] * 100);
        }
    }

    // ------------------------------------------------------------------ 4. recursividad
    private static void recursividad() {
        titulo("4. RECURSIVIDAD");
        List<Historial> todo = bd.getHistorial();
        System.out.println("Minutos totales vistos (sumarMinutos recursivo): " + Recursividad.sumarMinutos(todo, 0));
        System.out.println("Interacciones con 'le gustó' = 1 (contarLeGusto recursivo): " + Recursividad.contarLeGusto(todo, 0));
        System.out.printf("Valoración promedio global (recursivo): %.2f%n", Recursividad.promedioValoracion(todo));
        Contenido c = Recursividad.buscarContenido(bd.getContenidos(), "C010", 0);
        System.out.println("Búsqueda recursiva de C010: " + (c == null ? "no existe" : c.getTitulo()));

        System.out.println("\nÁrbol de categorías del catálogo (recorrido recursivo):");
        NodoCategoria raiz = NodoCategoria.construir(bd.getContenidos());
        raiz.imprimir(0);
        System.out.println("Total de títulos: " + raiz.contarTitulos() + " | Niveles: " + raiz.profundidad());

        Usuario u = bd.buscarUsuarioPorNombre("Juan");
        if (u != null) {
            PerfilUsuarios perfil = new PerfilUsuarios(bd.getHistorial());
            NodoCategoria.Candidato mejor = raiz.mejorCandidato(u, perfil, bd.idsVistosPor(u));
            if (mejor != null) {
                System.out.printf("Mejor título aún no visto para %s según el árbol de categorías: %s (%s)%n",
                        u.getNombre(), mejor.contenido.getTitulo(), mejor.contenido.getGenero());
            }
        }
    }

    // ------------------------------------------------------------------ 5. recomendaciones
    private static void recomendar(String nombre) {
        titulo("5. RECOMENDACIONES PARA UN USUARIO");
        Usuario u = bd.buscarUsuarioPorNombre(nombre);
        if (u == null) {
            System.out.println("No existe el usuario '" + nombre + "'.");
            return;
        }
        System.out.println("Usuario: " + u);
        System.out.println("Género favorito según su historial: " + consultas.generoFavoritoDe(nombre));

        Modelo actual = new SistemaActual(bd.getHistorial());
        PerfilUsuarios perfil = new PerfilUsuarios(bd.getHistorial());
        Modelo colab = new FiltradoColaborativo(bd.getHistorial(), perfil, 5);
        Modelo contenido = new BasadoEnContenido(perfil);
        Modelo hibrido = new Hibrido(bd.getHistorial(), SEMILLA);

        imprimirTop(actual, u, 5);
        imprimirTop(colab, u, 5);
        imprimirTop(contenido, u, 5);
        imprimirTop(hibrido, u, 5);
    }

    private static void imprimirTop(Modelo m, Usuario u, int n) {
        System.out.println("\n>> " + m.nombre() + " - Top " + n + ":");
        int i = 1;
        for (Recomendacion r : m.recomendar(u, bd, n)) {
            System.out.println("   " + (i++) + ". " + r);
        }
    }

    // ------------------------------------------------------------------ 6. validación
    private static void validacion() {
        titulo("6. VALIDACIÓN CRUZADA Y COMPARACIÓN");
        final int k = 5;
        final int topK = 2;
        List<Function<List<Historial>, Modelo>> fabricas = new ArrayList<>();
        fabricas.add(entrenamiento -> new SistemaActual(entrenamiento));
        fabricas.add(entrenamiento -> {
            PerfilUsuarios p = new PerfilUsuarios(entrenamiento);
            return new FiltradoColaborativo(entrenamiento, p, 5);
        });
        fabricas.add(entrenamiento -> new BasadoEnContenido(new PerfilUsuarios(entrenamiento)));
        fabricas.add(entrenamiento -> new ArbolModelo(entrenamiento, false, 4, 10, SEMILLA));
        fabricas.add(entrenamiento -> new Hibrido(entrenamiento, SEMILLA));
        fabricas.add(entrenamiento -> new ArbolModelo(entrenamiento, true, 4, 10, SEMILLA));

        List<Evaluador.Resultado> res = Evaluador.validacionCruzada(bd.getHistorial(), k, SEMILLA, topK, fabricas);
        Evaluador.imprimir(res, k, topK);

        Evaluador.Resultado base = null;
        Evaluador.Resultado propuesto = null;
        for (Evaluador.Resultado r : res) {
            if (r.nombre.startsWith("Sistema actual")) base = r;
            if (r.nombre.startsWith("Híbrido")) propuesto = r;
        }
        if (base != null && propuesto != null) {
            System.out.println();
            System.out.println("Comparación Híbrido vs Sistema actual:");
            System.out.printf("   Exactitud:      %+.1f puntos porcentuales%n",
                    (propuesto.exactitud() - base.exactitud()) * 100);
            System.out.printf("   Satisfacción:   %+.1f puntos porcentuales%n",
                    (propuesto.satisfaccion() - base.satisfaccion()) * 100);
            System.out.printf("   Tiempo visto:   %+.1f minutos por título recomendado%n",
                    propuesto.minutosTopK() - base.minutosTopK());
        }
        System.out.println();
        System.out.println("Nota: el árbol 'con comportamiento' usa minutos vistos y favoritos (datos que solo existen");
        System.out.println("después de ver el título), por eso no sirve para recomendar y no entra en la tabla de calidad.");
    }
}
