package streamflix.modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contenedor en memoria de todo el dataset (usuarios, contenido e historial).
 */
public class BaseDatos {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Contenido> contenidos = new ArrayList<>();
    private final List<Historial> historial = new ArrayList<>();
    private final Map<String, Usuario> usuarioPorId = new HashMap<>();
    private final Map<String, Contenido> contenidoPorId = new HashMap<>();

    public void agregar(Usuario u) {
        usuarios.add(u);
        usuarioPorId.put(u.getId(), u);
    }

    public void agregar(Contenido c) {
        contenidos.add(c);
        contenidoPorId.put(c.getId(), c);
    }

    public void agregar(Historial h) {
        historial.add(h);
    }

    public List<Usuario> getUsuarios() { return usuarios; }
    public List<Contenido> getContenidos() { return contenidos; }
    public List<Historial> getHistorial() { return historial; }

    public Usuario getUsuario(String id) { return usuarioPorId.get(id); }
    public Contenido getContenido(String id) { return contenidoPorId.get(id); }

    /** Busca un usuario por nombre (sin distinguir mayúsculas). Devuelve null si no existe. */
    public Usuario buscarUsuarioPorNombre(String nombre) {
        for (Usuario u : usuarios) {
            if (u.getNombre().equalsIgnoreCase(nombre.trim())) return u;
        }
        return null;
    }

    public List<Historial> historialDe(Usuario u) {
        List<Historial> r = new ArrayList<>();
        for (Historial h : historial) {
            if (h.getUsuario().getId().equals(u.getId())) r.add(h);
        }
        return r;
    }

    public Set<String> idsVistosPor(Usuario u) {
        Set<String> ids = new HashSet<>();
        for (Historial h : historial) {
            if (h.getUsuario().getId().equals(u.getId())) ids.add(h.getContenido().getId());
        }
        return ids;
    }
}
