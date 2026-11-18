package streamflix.modelo;

/**
 * Clase CONTENIDO del diagrama de clases: película o serie del catálogo.
 */
public class Contenido {
    private final String id;
    private final String titulo;
    private final String tipo;            // "Película" o "Serie"
    private final String genero;
    private final int anio;
    private final int duracionMin;
    private final String idiomaOriginal;
    private final String director;
    private final String pais;
    private final double valoracionPromedio;   // 1.0 a 5.0

    public Contenido(String id, String titulo, String tipo, String genero, int anio,
                     int duracionMin, String idiomaOriginal, String director,
                     String pais, double valoracionPromedio) {
        this.id = id;
        this.titulo = titulo;
        this.tipo = tipo;
        this.genero = genero;
        this.anio = anio;
        this.duracionMin = duracionMin;
        this.idiomaOriginal = idiomaOriginal;
        this.director = director;
        this.pais = pais;
        this.valoracionPromedio = valoracionPromedio;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getTipo() { return tipo; }
    public String getGenero() { return genero; }
    public int getAnio() { return anio; }
    public int getDuracionMin() { return duracionMin; }
    public String getIdiomaOriginal() { return idiomaOriginal; }
    public String getDirector() { return director; }
    public String getPais() { return pais; }
    public double getValoracionPromedio() { return valoracionPromedio; }

    public boolean esSerie() { return "Serie".equalsIgnoreCase(tipo); }

    @Override
    public String toString() {
        return String.format("%s | %-28s | %-8s | %-15s | %d | %3d min | %.1f★",
                id, titulo, tipo, genero, anio, duracionMin, valoracionPromedio);
    }
}
