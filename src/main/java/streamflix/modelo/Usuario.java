package streamflix.modelo;

/**
 * Clase USUARIO del diagrama de clases: nombre, país, edad y género.
 */
public class Usuario {
    private final String id;
    private final String nombre;
    private final String pais;
    private final int edad;
    private final String genero;   // "M" o "F"

    public Usuario(String id, String nombre, String pais, int edad, String genero) {
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
        this.edad = edad;
        this.genero = genero;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getPais() { return pais; }
    public int getEdad() { return edad; }
    public String getGenero() { return genero; }

    @Override
    public String toString() {
        return id + " | " + nombre + " | " + pais + " | " + edad + " años | " + genero;
    }
}
