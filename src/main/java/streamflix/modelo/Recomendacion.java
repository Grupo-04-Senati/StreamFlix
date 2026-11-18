package streamflix.modelo;

import java.time.LocalDate;

/**
 * Clase RECOMENDACIÓN: qué contenido se le recomienda a qué usuario, con qué puntaje
 * y con qué método (Sistema actual, Colaborativo, Híbrido, etc.).
 */
public class Recomendacion {
    private final LocalDate fecha;
    private final Usuario usuario;
    private final Contenido contenido;
    private final double puntaje;     // 0.0 a 1.0 (probabilidad estimada de que le guste)
    private final String metodo;

    public Recomendacion(LocalDate fecha, Usuario usuario, Contenido contenido,
                         double puntaje, String metodo) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.contenido = contenido;
        this.puntaje = puntaje;
        this.metodo = metodo;
    }

    public LocalDate getFecha() { return fecha; }
    public Usuario getUsuario() { return usuario; }
    public Contenido getContenido() { return contenido; }
    public double getPuntaje() { return puntaje; }
    public String getMetodo() { return metodo; }

    @Override
    public String toString() {
        return String.format("%-28s | %-15s | prob. de gustar: %5.1f%%",
                contenido.getTitulo(), contenido.getGenero(), puntaje * 100);
    }
}
