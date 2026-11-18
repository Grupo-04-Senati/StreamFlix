package streamflix.datos;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import streamflix.modelo.BaseDatos;
import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * CARGA: lee los archivos CSV (usuarios, peliculas, historial) y los convierte
 * en objetos Java guardados en una {@link BaseDatos}.
 */
public class CargadorDatos {

    public static BaseDatos cargar(String carpeta) throws IOException {
        BaseDatos bd = new BaseDatos();

        // 1) Usuarios: id,nombre,pais,edad,genero
        for (String[] f : leerCsv(Paths.get(carpeta, "usuarios.csv"), 5)) {
            try {
                bd.agregar(new Usuario(f[0], f[1], f[2], Integer.parseInt(f[3]), f[4]));
            } catch (NumberFormatException e) {
                System.out.println("  [aviso] Fila de usuario ignorada (edad inválida): " + String.join(",", f));
            }
        }

        // 2) Contenido: id,titulo,tipo,genero,anio,duracion_min,idioma_original,director,pais,valoracion_promedio
        for (String[] f : leerCsv(Paths.get(carpeta, "peliculas.csv"), 10)) {
            try {
                bd.agregar(new Contenido(f[0], f[1], f[2], f[3], Integer.parseInt(f[4]),
                        Integer.parseInt(f[5]), f[6], f[7], f[8], Double.parseDouble(f[9])));
            } catch (NumberFormatException e) {
                System.out.println("  [aviso] Fila de contenido ignorada (número inválido): " + String.join(",", f));
            }
        }

        // 3) Historial: usuario_id,contenido_id,minutos_vistos,termino,valoracion,favorito,le_gusto
        for (String[] f : leerCsv(Paths.get(carpeta, "historial.csv"), 7)) {
            Usuario u = bd.getUsuario(f[0]);
            Contenido c = bd.getContenido(f[1]);
            if (u == null || c == null) {
                System.out.println("  [aviso] Fila de historial ignorada (usuario o contenido no existe): " + String.join(",", f));
                continue;
            }
            try {
                bd.agregar(new Historial(u, c, Integer.parseInt(f[2]), f[3].equals("1"),
                        Integer.parseInt(f[4]), f[5].equals("1"), f[6].equals("1")));
            } catch (NumberFormatException e) {
                System.out.println("  [aviso] Fila de historial ignorada (número inválido): " + String.join(",", f));
            }
        }
        return bd;
    }

    /** Lee un CSV (con encabezado) y devuelve sus filas ya separadas por coma. */
    private static List<String[]> leerCsv(Path ruta, int columnasEsperadas) throws IOException {
        if (!Files.exists(ruta)) {
            throw new FileNotFoundException("No se encontró el archivo: " + ruta.toAbsolutePath()
                    + "\n  -> La carpeta 'datos' debe estar en la raíz del proyecto de NetBeans.");
        }
        List<String[]> filas = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            String linea;
            boolean encabezado = true;
            while ((linea = br.readLine()) != null) {
                if (encabezado) { encabezado = false; continue; }   // saltar la primera línea
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                String[] campos = linea.split(",", -1);
                if (campos.length != columnasEsperadas) {
                    System.out.println("  [aviso] Fila ignorada en " + ruta.getFileName() + ": " + linea);
                    continue;
                }
                for (int i = 0; i < campos.length; i++) campos[i] = campos[i].trim();
                filas.add(campos);
            }
        }
        return filas;
    }
}
