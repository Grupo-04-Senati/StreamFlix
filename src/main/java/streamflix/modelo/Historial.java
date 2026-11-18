package streamflix.modelo;

/**
 * Clase HISTORIAL (interacción usuario-contenido).
 * La variable objetivo del proyecto es {@link #isLeGusto()} ("Le gusta" = 1 / 0).
 */
public class Historial {
    private final Usuario usuario;
    private final Contenido contenido;
    private final int minutosVistos;
    private final boolean termino;      // ¿terminó de verla?
    private final int valoracion;       // 1 a 5
    private final boolean favorito;     // ¿la guardó en favoritos?
    private final boolean leGusto;      // VARIABLE OBJETIVO

    public Historial(Usuario usuario, Contenido contenido, int minutosVistos,
                     boolean termino, int valoracion, boolean favorito, boolean leGusto) {
        this.usuario = usuario;
        this.contenido = contenido;
        this.minutosVistos = minutosVistos;
        this.termino = termino;
        this.valoracion = valoracion;
        this.favorito = favorito;
        this.leGusto = leGusto;
    }

    public Usuario getUsuario() { return usuario; }
    public Contenido getContenido() { return contenido; }
    public int getMinutosVistos() { return minutosVistos; }
    public boolean isTermino() { return termino; }
    public int getValoracion() { return valoracion; }
    public boolean isFavorito() { return favorito; }
    public boolean isLeGusto() { return leGusto; }

    /** Fracción vista del contenido (0.0 a 1.0). */
    public double getPorcentajeVisto() {
        if (contenido.getDuracionMin() <= 0) return 0;
        return Math.min(1.0, minutosVistos / (double) contenido.getDuracionMin());
    }

    @Override
    public String toString() {
        return String.format("%-10s -> %-28s | %-15s | %3d min | val %d | fav %d | le gustó %d",
                usuario.getNombre(), contenido.getTitulo(), contenido.getGenero(),
                minutosVistos, valoracion, favorito ? 1 : 0, leGusto ? 1 : 0);
    }
}
