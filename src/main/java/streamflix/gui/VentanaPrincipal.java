package streamflix.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import streamflix.consultas.ConsultaDatos;
import streamflix.datos.CargadorDatos;
import streamflix.ml.ArbolReglas;
import streamflix.ml.Hibrido;
import streamflix.ml.Modelo;
import streamflix.modelo.BaseDatos;
import streamflix.modelo.Contenido;
import streamflix.modelo.Historial;
import streamflix.modelo.Usuario;

/**
 * INTERFAZ GRÁFICA (Swing) del PUNTO 7: "Clases Java que cargan, recorren y consultan los datos",
 * más el flujo del ADMINISTRADOR (datos de prueba, patrón con árbol de decisión y publicación).
 *
 * Usa directamente las clases del proyecto:
 *   - {@link CargadorDatos}   -> pestaña 1 (CARGA)
 *   - {@link BaseDatos}       -> pestaña 2 (RECORRER las listas)
 *   - {@link ConsultaDatos}   -> pestaña 3 (CONSULTAR)
 *   - CSV de datos            -> pestaña 4 (AGREGAR usuarios, películas e interacciones)
 *   - {@link ArbolReglas}     -> pestaña 5 (PATRÓN y recomendaciones del administrador)
 *   - código fuente           -> pestaña 6
 *
 * Se abre con:  Main --gui    o directamente  streamflix.gui.VentanaPrincipal
 */
public class VentanaPrincipal extends JFrame {

    // ---- paleta fija del proyecto: gris/negro + naranja de marca (#E68200)
    private static final Color BG = new Color(0x1b1b1b);
    private static final Color PANEL = new Color(0x212121);
    private static final Color CARD = new Color(0x292929);
    private static final Color BORDE = new Color(0x333333);
    private static final Color ORANGE = new Color(0xe68200);
    private static final Color ORANGE_CLARO = new Color(0xf59b1f);
    private static final Color AMBAR = new Color(0xf7b356);
    private static final Color CREMA = new Color(0xf9cc8f);
    private static final Color COBRE = new Color(0xc06d00);
    private static final Color ORANGE_OSCURO = new Color(0x7a4400);
    private static final Color TEXT = new Color(0xffffff);
    private static final Color MUTED = new Color(0x808080);
    private static final Color ALT = new Color(0x232323);
    private static final Color SELECCION = new Color(0x3a3a3a);
    private static final Color[] PALETA_GRAFICO = {ORANGE, ORANGE_CLARO, AMBAR, CREMA, COBRE, ORANGE_OSCURO};

    /** Explica qué significa cada columna del administrador (tooltip del encabezado). */
    private static final Map<String, String> AYUDA_COLUMNAS = new LinkedHashMap<>();
    static {
        AYUDA_COLUMNAS.put("% modelo", "Probabilidad de gustar según el modelo híbrido "
                + "(contenido + usuarios similares). Es una señal, no la decisión final.");
        AYUDA_COLUMNAS.put("Patrón (árbol)", "Categoría que asignó el árbol de reglas: "
                + "Alta Prioridad, Por Contenido o Exploración.");
        AYUDA_COLUMNAS.put("Confianza", "Confianza de la regla del árbol que clasificó este título.");
        AYUDA_COLUMNAS.put("Decisión", "Decisión final del árbol de reglas. Puede ser «Sí» aunque "
                + "el % del modelo sea bajo (p. ej. títulos de Exploración).");
    }

    private static final String[] METODOS = {
        "historialDe(nombre)",
        "contenidoQueLeGustoA(nombre)",
        "perfilDe(nombre)",
        "usuariosPorPais()",
        "contenidoPorGenero(genero)",
        "usuariosQueVieronGenero(genero)",
        "vistosMasDe(minutos)",
        "tasaDeLikePorGenero()",
        "minutosPorGenero()",
        "masVistos(n)",
        "usuariosPorEdad(min, max)"
    };
    private static final String[] AYUDA = {
        "Recorre el historial y devuelve todo lo que vio el usuario.",
        "Contenido del historial con le_gusto = 1.",
        "Género favorito, promedio de valoración y minutos totales del usuario.",
        "Cantidad de usuarios por país.",
        "Títulos del catálogo de un género.",
        "Usuarios distintos que vieron al menos un título del género.",
        "Interacciones con más de X minutos vistos.",
        "% de «me gusta» por género (tasa de aprobación).",
        "Minutos totales vistos por género.",
        "Top N del contenido más visto (minutos acumulados).",
        "Usuarios dentro de un rango de edad."
    };

    static {
        UIManager.put("TabbedPaneUI", TabsEstilo.class.getName());
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.contentAreaColor", BG);
        UIManager.put("TabbedPane.focus", BG);
        UIManager.put("ScrollBarUI", ScrollEstilo.class.getName());
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.background", PANEL);
        UIManager.put("ScrollBar.foreground", TEXT);
        UIManager.put("ScrollBar.thumb", new Color(0x333333));
        UIManager.put("ScrollBar.track", PANEL);
        UIManager.put("ScrollPane.background", PANEL);
        UIManager.put("Viewport.background", PANEL);
        UIManager.put("TextArea.selectionColor", SELECCION);
        UIManager.put("TextArea.selectedTextColor", Color.WHITE);
        UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("TextField.selectionColor", SELECCION);
        UIManager.put("TextField.selectedTextColor", Color.WHITE);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("ComboBox.disabledBackground", PANEL);
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("focusColor", ORANGE_CLARO);
        UIManager.put("ToolTip.background", new Color(0x292929));
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDE));
        UIManager.put("Table.showGrid", Boolean.FALSE);
        UIManager.put("ComboBox.background", PANEL);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", SELECCION);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("Spinner.background", PANEL);
        UIManager.put("Spinner.foreground", TEXT);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("CheckBox.icon", new IconoCasilla());
        UIManager.put("CheckBox.background", BG);
        UIManager.put("CheckBox.foreground", TEXT);
    }

    private final String carpetaInicial;
    private JTabbedPane pestanas;
    private JTextField campoBuscar;
    private BaseDatos bd;
    private ConsultaDatos consultas;

    // pestaña 1
    private JTextField campoCarpeta;
    private final JLabel[] tarjetas = new JLabel[4];
    private JTextArea registro;
    // pestaña 2
    private TablaPanel tUsuarios, tContenidos, tHistorial;
    // pestaña 3
    private JList<String> listaMetodos;
    private JComboBox<String> cbUsuario, cbGenero;
    private JSpinner spMinutos, spTopN, spEdadMin, spEdadMax;
    private JTextArea lblAyuda;
    private JLabel lblResumen, lblEstado;
    private JPanel zonaResultado;
    private TablaPanel tablaResultado;
    private GraficoBarras grafico;
    // pestaña 4
    private JComboBox<String> cbArchivo;
    private JTextArea areaCodigo;
    // pestaña 5: datos de prueba
    private JTextField tfUsuId, tfUsuNombre, tfUsuPais;
    private JSpinner spUsuEdad;
    private JComboBox<String> cbUsuGenero;
    private JTextField tfPeliId, tfPeliTitulo, tfPeliDirector;
    private JComboBox<String> cbPeliTipo, cbPeliGenero, cbPeliIdioma, cbPeliPais;
    private JSpinner spPeliAnio, spPeliDuracion, spPeliValoracion;
    private JComboBox<String> cbInterUsuario, cbInterPelicula;
    private JSpinner spInterMinutos, spInterValoracion;
    private JCheckBox chkInterTermino, chkInterFavorito, chkInterGusto;
    private JLabel lblDatosUsuarios, lblDatosPeliculas, lblDatosInteracciones;
    private TablaPanel tUltimosUsuarios, tUltimosContenidos, tUltimosHistorial;
    // pestaña 6: administrador
    private JComboBox<String> cbAdminUsuario;
    private JSpinner spAdminTop;
    private JCheckBox chkAdminTodas;
    private JLabel lblAdminResumen;
    private JTextArea areaPatron, areaRuta;
    private TablaPanel tablaAdmin, tablaGustos, tablaPublicadas;
    private JTabbedPane subAdmin;
    private final List<Usuario> usuariosAdmin = new ArrayList<>();
    private List<FilaAdmin> todasAdmin = new ArrayList<>();
    private List<FilaAdmin> filasAdmin = new ArrayList<>();
    private ArbolReglas arbolReglas;
    private Modelo hibridoAdmin;
    private boolean actualizandoAdmin;

    /** Una fila de la tabla del administrador. */
    private static class FilaAdmin {
        final Contenido contenido;
        final double probabilidad;
        final ArbolReglas.Resultado resultado;

        FilaAdmin(Contenido contenido, double probabilidad, ArbolReglas.Resultado resultado) {
            this.contenido = contenido;
            this.probabilidad = probabilidad;
            this.resultado = resultado;
        }
    }

    public VentanaPrincipal(String carpetaDatos) {
        super("StreamFlix · Punto 7 + Administrador — cargar, recorrer, consultar, recomendar y publicar");
        this.carpetaInicial = carpetaDatos;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 840);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JPanel cab = new Cabecera();
        cab.setLayout(new BorderLayout(18, 0));
        // Logo tipo bloque: "STREAM" en blanco + "FLIX" negro sobre naranja.
        JLabel t = new JLabel("STREAM");
        t.setForeground(Color.WHITE);
        t.setFont(new Font("SansSerif", Font.BOLD, 26));
        JLabel bloque = new JLabel("FLIX");
        bloque.setOpaque(true);
        bloque.setBackground(ORANGE);
        bloque.setForeground(new Color(0x1b1b1b));
        bloque.setFont(new Font("SansSerif", Font.BOLD, 26));
        bloque.setBorder(BorderFactory.createEmptyBorder(0, 7, 2, 7));
        JLabel st = new JLabel("Punto 7 · Cargar · Recorrer · Consultar · Administrador");
        st.setForeground(MUTED);
        st.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel izqCab = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        izqCab.setOpaque(false);
        izqCab.add(t);
        izqCab.add(bloque);
        izqCab.add(javax.swing.Box.createHorizontalStrut(14));
        izqCab.add(st);
        cab.add(izqCab, BorderLayout.WEST);
        // Buscador central y acciones de la derecha, como la barra del sitio.
        JPanel buscar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buscar.setOpaque(false);
        campoBuscar = new JTextField(26);
        estilizarCampo(campoBuscar);
        campoBuscar.setPreferredSize(new Dimension(340, 34));
        campoBuscar.setToolTipText("Escribe y pulsa Enter: filtra el catálogo en la pestaña Recorrer");
        campoBuscar.addActionListener(e -> buscar());
        JButton lupa = new BotonIcono(new IconoBuscar());
        lupa.setToolTipText("Buscar en el catálogo");
        lupa.addActionListener(e -> buscar());
        buscar.add(campoBuscar);
        buscar.add(lupa);
        cab.add(buscar, BorderLayout.CENTER);
        JLabel subir = new JLabel("SUBIR");
        subir.setOpaque(true);
        subir.setBackground(ORANGE);
        subir.setForeground(new Color(0x1b1b1b));
        subir.setFont(new Font("SansSerif", Font.BOLD, 11));
        subir.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        JLabel sesion = new JLabel("INICIAR SESIÓN");
        sesion.setOpaque(true);
        sesion.setBackground(new Color(0x333333));
        sesion.setForeground(TEXT);
        sesion.setFont(new Font("SansSerif", Font.BOLD, 11));
        sesion.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        JPanel derCab = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derCab.setOpaque(false);
        derCab.add(subir);
        derCab.add(sesion);
        cab.add(derCab, BorderLayout.EAST);
        cab.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        add(cab, BorderLayout.NORTH);

        pestanas = new JTabbedPane();
        pestanas.setBackground(BG);
        pestanas.setForeground(TEXT);
        pestanas.setFont(new Font("SansSerif", Font.BOLD, 12));
        pestanas.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        pestanas.addTab("INICIO", crearPestanaCarga());
        pestanas.addTab("RECORRER", crearPestanaRecorrer());
        pestanas.addTab("CONSULTAS", crearPestanaConsultas());
        pestanas.addTab("DATOS DE PRUEBA", crearPestanaDatosPrueba());
        pestanas.addTab("ADMINISTRADOR", crearPestanaAdmin());
        pestanas.addTab("CÓDIGO JAVA", crearPestanaCodigo());
        add(pestanas, BorderLayout.CENTER);

        lblEstado = new JLabel("Sin datos cargados.", new IconoPunto(MUTED), JLabel.LEFT);
        lblEstado.setIconTextGap(8);
        lblEstado.setForeground(MUTED);
        lblEstado.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(PANEL);
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDE),
                BorderFactory.createEmptyBorder(6, 14, 7, 14)));
        pie.add(lblEstado, BorderLayout.WEST);
        // Firma discreta de la app (apenas visible sobre el fondo).
        JLabel firma = new JLabel("badillo hub");
        firma.setForeground(new Color(0x3d3d3d));
        firma.setFont(new Font("SansSerif", Font.PLAIN, 10));
        firma.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        pie.add(firma, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::cargar);
        temarControles(getContentPane());
    }

    /** Filtra el catálogo desde la barra de búsqueda de la cabecera. */
    private void buscar() {
        String q = campoBuscar == null ? "" : campoBuscar.getText().trim();
        if (tContenidos != null) tContenidos.filtrar(q);
        if (pestanas != null) pestanas.setSelectedIndex(1);
    }

    /** Recorre el árbol de componentes y deja spinners y combos con el tema oscuro. */
    private static void temarControles(Container c) {
        for (Component h : c.getComponents()) {
            if (h instanceof JScrollPane) {
                JScrollPane sc = (JScrollPane) h;
                sc.setBackground(PANEL);
                sc.getViewport().setBackground(PANEL);
            } else if (h instanceof JSpinner) {
                JSpinner s = (JSpinner) h;
                s.setBackground(PANEL);
                s.setBorder(BorderFactory.createLineBorder(BORDE));
                if (s.getEditor() instanceof JSpinner.DefaultEditor) {
                    JTextField f = ((JSpinner.DefaultEditor) s.getEditor()).getTextField();
                    f.setBackground(PANEL);
                    f.setForeground(TEXT);
                    f.setDisabledTextColor(MUTED);
                    f.setCaretColor(TEXT);
                    f.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                }
                for (Component b : s.getComponents()) {
                    b.setBackground(new Color(0x333333));
                    b.setForeground(TEXT);
                }
            } else if (h instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) h;
                cb.setBackground(PANEL);
                cb.setForeground(TEXT);
                cb.setBorder(BorderFactory.createLineBorder(BORDE));
                cb.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public java.awt.Component getListCellRendererComponent(
                            JList<?> l, Object v, int i, boolean sel, boolean foc) {
                        JLabel r = (JLabel) super.getListCellRendererComponent(l, v, i, sel, foc);
                        r.setOpaque(true);
                        r.setBackground(sel ? SELECCION : PANEL);
                        r.setForeground(sel ? Color.WHITE : TEXT);
                        r.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                        return r;
                    }
                });
                if (cb.isEditable()) {
                    java.awt.Component ed = cb.getEditor().getEditorComponent();
                    if (ed instanceof JTextField) {
                        JTextField f = (JTextField) ed;
                        f.setBackground(PANEL);
                        f.setForeground(TEXT);
                        f.setCaretColor(TEXT);
                    }
                }
                for (Component b : cb.getComponents()) {
                    b.setBackground(new Color(0x333333));
                    b.setForeground(TEXT);
                }
            }
            if (h instanceof Container) temarControles((Container) h);
        }
    }

    /** Divisor del tema: se arrastra para repartir espacio y sus flechas pliegan/restauran
     *  un lado guardando la posición anterior, para poder volver atrás con un clic. */
    private static void divisorOscuro(JSplitPane sp) {
        boolean vertical = sp.getOrientation() == JSplitPane.VERTICAL_SPLIT;
        sp.setDividerSize(14);
        sp.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new DivisorTema(this, vertical);
            }
        });
        sp.setBackground(BG);
        sp.setBorder(null);
        sp.setToolTipText("Arrastra el divisor para repartir el espacio · clic en sus flechas para plegar un lado y volver atrás");
        javax.swing.plaf.basic.BasicSplitPaneDivider div =
                ((javax.swing.plaf.basic.BasicSplitPaneUI) sp.getUI()).getDivider();
        div.setBorder(null);
        // Sin mínimos, un lado puede plegarse de verdad; el divisor queda encima de los
        // paneles para que sus flechas sigan alcanzables aunque un lado quede en 0.
        if (sp.getLeftComponent() != null) sp.getLeftComponent().setMinimumSize(new Dimension(0, 0));
        if (sp.getRightComponent() != null) sp.getRightComponent().setMinimumSize(new Dimension(0, 0));
        sp.setComponentZOrder(div, 0);
    }

    private static class DivisorTema extends javax.swing.plaf.basic.BasicSplitPaneDivider {
        private final boolean vertical;
        private int guardadaPrimero = -1;
        private int guardadaSegundo = -1;
        private int zonaHover;
        private int pressX, pressY;
        private int locPress;
        private boolean movido;

        DivisorTema(javax.swing.plaf.basic.BasicSplitPaneUI ui, boolean vertical) {
            super(ui);
            this.vertical = vertical;
            setBackground(BG);
            setBorder(null);
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    pressX = e.getX();
                    pressY = e.getY();
                    movido = false;
                    locPress = splitPane == null ? -1 : splitPane.getDividerLocation();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    if (Math.abs(e.getX() - pressX) < 4 && Math.abs(e.getY() - pressY) < 4) {
                        // El MouseHandler de Swing completa su "arrastre" después de este
                        // listener y pisaría la posición: plegar/restaurar tras él.
                        final int x = e.getX(), y = e.getY();
                        SwingUtilities.invokeLater(() -> clicZona(x, y));
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    zonaHover = 0;
                    repaint();
                }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int z = zona(e.getX(), e.getY());
                    if (z != zonaHover) {
                        zonaHover = z;
                        repaint();
                    }
                }

                @Override
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (Math.abs(e.getX() - pressX) >= 4 || Math.abs(e.getY() - pressY) >= 4) movido = true;
                }
            });
        }

        /** En un clic sin arrastre, Swing "completa" el arrastre soltando en el punto del
         *  clic y lo recorta a los mínimos de los paneles, pisando el plegado: se completa
         *  en la posición previa al clic para que las flechas puedan plegar y restaurar. */
        @Override
        protected void finishDraggingTo(int location) {
            super.finishDraggingTo(movido ? location : locPress);
        }

        @Override
        protected JButton createLeftOneTouchButton() { return null; }

        @Override
        protected JButton createRightOneTouchButton() { return null; }

        /** 1 = flecha que pliega el primer lado, 2 = la que pliega el segundo, 0 = resto. */
        private int zona(int x, int y) {
            int w = getWidth(), h = getHeight();
            if (vertical) {
                if (y >= h / 2 - 16 && y <= h / 2 - 2) return 1;
                if (y >= h / 2 + 2 && y <= h / 2 + 16) return 2;
            } else {
                if (x >= w / 2 - 16 && x <= w / 2 - 2) return 1;
                if (x >= w / 2 + 2 && x <= w / 2 + 16) return 2;
            }
            return 0;
        }

        private void clicZona(int x, int y) {
            int z = zona(x, y);
            if (z == 0 || splitPane == null) return;
            int max = (vertical ? splitPane.getHeight() : splitPane.getWidth())
                    - splitPane.getDividerSize();
            if (max <= 0) return;
            int loc = splitPane.getDividerLocation();
            if (z == 1) {
                if (loc <= 0) {
                    if (guardadaPrimero > 0) splitPane.setDividerLocation(Math.min(guardadaPrimero, max));
                } else {
                    guardadaPrimero = loc;
                    splitPane.setDividerLocation(0);
                }
            } else {
                if (loc >= max) {
                    if (guardadaSegundo > 0) splitPane.setDividerLocation(Math.min(guardadaSegundo, max));
                } else {
                    guardadaSegundo = loc;
                    splitPane.setDividerLocation(max);
                }
            }
        }

        @Override
        public void paint(Graphics g0) {
            g0.setColor(getBackground());
            g0.fillRect(0, 0, getWidth(), getHeight());
            super.paint(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setColor(BORDE);
            if (vertical) g.fillRect(6, h / 2 - 1, w - 12, 2);
            else g.fillRect(w / 2 - 1, 6, 2, h - 12);
            g.setColor(zonaHover == 1 ? ORANGE_CLARO : MUTED);
            if (vertical) {
                int x = w / 2 - 9;
                g.fillPolygon(new int[]{x, x - 4, x + 4}, new int[]{h / 2 - 3, h / 2 + 3, h / 2 + 3}, 3);
            } else {
                int y = h / 2 - 9;
                g.fillPolygon(new int[]{w / 2 - 3, w / 2 + 3, w / 2 + 3},
                        new int[]{y, y - 4, y + 4}, 3);
            }
            g.setColor(zonaHover == 2 ? ORANGE_CLARO : MUTED);
            if (vertical) {
                int x = w / 2 + 9;
                g.fillPolygon(new int[]{x, x - 4, x + 4}, new int[]{h / 2 + 3, h / 2 - 3, h / 2 - 3}, 3);
            } else {
                int y = h / 2 + 9;
                g.fillPolygon(new int[]{w / 2 + 3, w / 2 - 3, w / 2 - 3},
                        new int[]{y, y - 4, y + 4}, 3);
            }
            g.dispose();
        }
    }

    // =========================================================== 1. CARGA
    private JComponent crearPestanaCarga() {
        JPanel p = panelBase(new BorderLayout(0, 10));

        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        JLabel l = etiqueta("Carpeta de datos:");
        campoCarpeta = new JTextField(carpetaInicial);
        estilizarCampo(campoCarpeta);
        JButton bExaminar = boton("Examinar…");
        JButton bCargar = boton("Cargar datos");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botones.setOpaque(false);
        botones.add(bExaminar);
        botones.add(bCargar);
        fila.add(l, BorderLayout.WEST);
        fila.add(campoCarpeta, BorderLayout.CENTER);
        fila.add(botones, BorderLayout.EAST);
        bExaminar.addActionListener(e -> examinar());
        bCargar.addActionListener(e -> cargar());

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        JPanel cards = new JPanel(new java.awt.GridLayout(1, 4, 12, 0));
        cards.setOpaque(false);
        String[] titulos = {"USUARIOS", "CONTENIDOS", "INTERACCIONES", "LE GUSTÓ = 1"};
        Color[] acentos = {ORANGE, ORANGE_CLARO, AMBAR, COBRE};
        for (int i = 0; i < 4; i++) {
            JPanel c = new TarjetaStat(acentos[i]);
            c.setLayout(new BorderLayout(0, 2));
            c.setBorder(BorderFactory.createEmptyBorder(104, 18, 12, 14));
            tarjetas[i] = new JLabel("–");
            tarjetas[i].setForeground(acentos[i]);
            tarjetas[i].setFont(new Font("SansSerif", Font.BOLD, 30));
            JLabel lt = new JLabel(titulos[i]);
            lt.setForeground(MUTED);
            lt.setFont(new Font("SansSerif", Font.BOLD, 11));
            c.add(tarjetas[i], BorderLayout.CENTER);
            c.add(lt, BorderLayout.SOUTH);
            cards.add(c);
        }
        centro.add(cards, BorderLayout.NORTH);

        registro = new JTextArea();
        registro.setEditable(false);
        registro.setBackground(PANEL);
        registro.setForeground(TEXT);
        registro.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        registro.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(registro);
        sp.setBorder(BorderFactory.createLineBorder(ALT));
        JPanel logPanel = new JPanel(new BorderLayout(0, 4));
        logPanel.setOpaque(false);
        logPanel.add(etiqueta("Registro de carga (CargadorDatos.cargar):"), BorderLayout.NORTH);
        logPanel.add(sp, BorderLayout.CENTER);
        centro.add(logPanel, BorderLayout.CENTER);

        p.add(fila, BorderLayout.NORTH);
        p.add(centro, BorderLayout.CENTER);
        return p;
    }

    private void examinar() {
        JFileChooser fc = new JFileChooser(campoCarpeta.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Selecciona la carpeta 'datos'");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoCarpeta.setText(fc.getSelectedFile().getAbsolutePath());
            cargar();
        }
    }

    /** Llama a la clase REAL CargadorDatos y refresca toda la interfaz. */
    private void cargar() {
        String carpeta = campoCarpeta.getText().trim();
        registro.setText("");
        log("Leyendo CSV desde: " + Paths.get(carpeta).toAbsolutePath());
        try {
            bd = CargadorDatos.cargar(carpeta);
        } catch (IOException ex) {
            log("ERROR al cargar los datos: " + ex.getMessage());
            lblEstado.setIcon(new IconoPunto(MUTED));
            lblEstado.setText("Error al cargar. Revisa la carpeta de datos.");
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudieron cargar los datos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        consultas = new ConsultaDatos(bd);
        arbolReglas = new ArbolReglas(bd.getHistorial());
        hibridoAdmin = new Hibrido(bd.getHistorial(), 456);
        int likes = 0;
        for (Historial h : bd.getHistorial()) if (h.isLeGusto()) likes++;
        log("usuarios.csv  -> " + bd.getUsuarios().size() + " objetos Usuario");
        log("peliculas.csv -> " + bd.getContenidos().size() + " objetos Contenido");
        log("historial.csv -> " + bd.getHistorial().size() + " objetos Historial");
        log("(los avisos de filas ignoradas, si los hay, salen en la consola de NetBeans)");
        tarjetas[0].setText(String.valueOf(bd.getUsuarios().size()));
        tarjetas[1].setText(String.valueOf(bd.getContenidos().size()));
        tarjetas[2].setText(String.valueOf(bd.getHistorial().size()));
        tarjetas[3].setText(String.valueOf(likes));
        animarConteo(tarjetas[0], bd.getUsuarios().size());
        animarConteo(tarjetas[1], bd.getContenidos().size());
        animarConteo(tarjetas[2], bd.getHistorial().size());
        animarConteo(tarjetas[3], likes);
        lblEstado.setIcon(new IconoPunto(ORANGE));
        lblEstado.setText("Datos cargados: " + bd.getUsuarios().size() + " usuarios, "
                + bd.getContenidos().size() + " títulos, " + bd.getHistorial().size() + " interacciones.");
        llenarRecorrer();
        llenarCombos();
        refrescarDatosPrueba();
        refrescarComboAdmin();
        mostrarPublicaciones();
    }

    private void log(String s) {
        registro.append(s + "\n");
    }

    // =========================================================== 2. RECORRER
    private JComponent crearPestanaRecorrer() {
        JTabbedPane sub = new JTabbedPane();
        sub.setBackground(BG);
        sub.setFont(new Font("SansSerif", Font.BOLD, 12));
        tUsuarios = new TablaPanel(new String[]{"ID", "Nombre", "País", "Edad", "Género"});
        tContenidos = new TablaPanel(new String[]{"ID", "Título", "Tipo", "Género", "Año", "Duración",
            "Idioma", "Director", "País", "Valoración"});
        tHistorial = new TablaPanel(new String[]{"Usuario", "Título", "Género", "Min. vistos", "Terminó",
            "Valoración", "Favorito", "Le gustó"});
        sub.addTab("Usuarios", tUsuarios);
        sub.addTab("Catálogo (Contenido)", tContenidos);
        sub.addTab("Historial (interacciones)", tHistorial);
        JPanel p = panelBase(new BorderLayout());
        p.add(sub, BorderLayout.CENTER);
        return p;
    }

    private void llenarRecorrer() {
        List<Object[]> u = new ArrayList<>();
        for (Usuario x : bd.getUsuarios()) {
            u.add(new Object[]{x.getId(), x.getNombre(), x.getPais(), x.getEdad(), x.getGenero()});
        }
        tUsuarios.setFilas(u);
        List<Object[]> c = new ArrayList<>();
        for (Contenido x : bd.getContenidos()) {
            c.add(new Object[]{x.getId(), x.getTitulo(), x.getTipo(), x.getGenero(), x.getAnio(),
                x.getDuracionMin(), x.getIdiomaOriginal(), x.getDirector(), x.getPais(),
                String.format(Locale.US, "%.1f", x.getValoracionPromedio())});
        }
        tContenidos.setFilas(c);
        tHistorial.setFilas(filasHistorial(bd.getHistorial()));
    }

    private static List<Object[]> filasHistorial(List<Historial> hs) {
        List<Object[]> r = new ArrayList<>();
        for (Historial h : hs) {
            r.add(new Object[]{h.getUsuario().getNombre(), h.getContenido().getTitulo(),
                h.getContenido().getGenero(), h.getMinutosVistos(), h.isTermino() ? "Sí" : "No",
                h.getValoracion(), h.isFavorito() ? "Sí" : "No", h.isLeGusto() ? "1" : "0"});
        }
        return r;
    }

    // =========================================================== 3. CONSULTAS
    private JComponent crearPestanaConsultas() {
        JPanel izq = new JPanel(new BorderLayout(0, 8));
        izq.setBackground(BG);
        izq.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        listaMetodos = new JList<>(METODOS);
        listaMetodos.setBackground(PANEL);
        listaMetodos.setForeground(TEXT);
        listaMetodos.setSelectionBackground(ORANGE);
        listaMetodos.setSelectionForeground(Color.WHITE);
        listaMetodos.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        listaMetodos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaMetodos.setSelectedIndex(0);
        listaMetodos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, s, f);
                c.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                return c;
            }
        });
        listaMetodos.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) actualizarParametros(); });
        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.setOpaque(false);
        arriba.add(etiqueta("Método a ejecutar:"), BorderLayout.NORTH);
        arriba.add(listaMetodos, BorderLayout.CENTER);
        izq.add(arriba, BorderLayout.NORTH);

        cbUsuario = new JComboBox<>();
        cbGenero = new JComboBox<>();
        spMinutos = new JSpinner(new SpinnerNumberModel(100, 0, 500, 10));
        spTopN = new JSpinner(new SpinnerNumberModel(5, 1, 40, 1));
        spEdadMin = new JSpinner(new SpinnerNumberModel(18, 1, 100, 1));
        spEdadMax = new JSpinner(new SpinnerNumberModel(25, 1, 100, 1));
        JPanel params = new JPanel(new GridBagLayout());
        params.setOpaque(false);
        JComponent[] ws = {cbUsuario, cbGenero, spMinutos, spTopN, spEdadMin, spEdadMax};
        String[] nombres = {"Usuario", "Género", "Minutos >", "Top N", "Edad mín.", "Edad máx."};
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 0, 3, 8);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < ws.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0; g.fill = GridBagConstraints.NONE;
            params.add(etiqueta(nombres[i]), g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            params.add(ws[i], g);
        }
        JButton ejecutar = boton("▶  Ejecutar consulta");
        ejecutar.addActionListener(e -> ejecutar());
        lblAyuda = new JTextArea(3, 20);
        lblAyuda.setEditable(false);
        lblAyuda.setLineWrap(true);
        lblAyuda.setWrapStyleWord(true);
        lblAyuda.setOpaque(false);
        lblAyuda.setForeground(MUTED);

        JPanel medio = new JPanel(new BorderLayout(0, 8));
        medio.setOpaque(false);
        medio.add(etiqueta("Parámetros:"), BorderLayout.NORTH);
        JPanel contenedorParams = new JPanel(new BorderLayout());
        contenedorParams.setOpaque(false);
        contenedorParams.add(params, BorderLayout.NORTH);
        medio.add(contenedorParams, BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(0, 8));
        abajo.setOpaque(false);
        abajo.add(ejecutar, BorderLayout.NORTH);
        abajo.add(lblAyuda, BorderLayout.CENTER);
        medio.add(abajo, BorderLayout.SOUTH);
        izq.add(medio, BorderLayout.CENTER);
        izq.setPreferredSize(new Dimension(300, 100));

        lblResumen = new JLabel("Elige un método y pulsa «Ejecutar consulta».");
        lblResumen.setForeground(AMBAR);
        lblResumen.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblResumen.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        tablaResultado = new TablaPanel(new String[]{"—"});
        grafico = new GraficoBarras();
        grafico.setPreferredSize(new Dimension(100, 210));
        zonaResultado = new JPanel(new BorderLayout());
        zonaResultado.setOpaque(false);
        zonaResultado.add(tablaResultado, BorderLayout.CENTER);
        JPanel der = new JPanel(new BorderLayout());
        der.setBackground(BG);
        der.add(lblResumen, BorderLayout.NORTH);
        der.add(zonaResultado, BorderLayout.CENTER);
        der.add(grafico, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setBorder(null);
        split.setBackground(BG);
        divisorOscuro(split);
        split.setDividerLocation(300);
        JPanel p = panelBase(new BorderLayout());
        p.add(split, BorderLayout.CENTER);
        actualizarParametros();
        return p;
    }

    private void llenarCombos() {
        TreeSet<String> nombres = new TreeSet<>();
        for (Usuario u : bd.getUsuarios()) nombres.add(u.getNombre());
        TreeSet<String> generos = new TreeSet<>();
        for (Contenido c : bd.getContenidos()) generos.add(c.getGenero());
        cbUsuario.setModel(new javax.swing.DefaultComboBoxModel<>(nombres.toArray(new String[0])));
        cbGenero.setModel(new javax.swing.DefaultComboBoxModel<>(generos.toArray(new String[0])));
    }

    private void actualizarParametros() {
        int i = Math.max(0, listaMetodos.getSelectedIndex());
        cbUsuario.setEnabled(i <= 2);
        cbGenero.setEnabled(i == 4 || i == 5);
        spMinutos.setEnabled(i == 6);
        spTopN.setEnabled(i == 9);
        spEdadMin.setEnabled(i == 10);
        spEdadMax.setEnabled(i == 10);
        lblAyuda.setText(AYUDA[i]);
    }

    /** Ejecuta el método de la clase REAL ConsultaDatos que eligió el usuario. */
    private void ejecutar() {
        if (consultas == null) {
            JOptionPane.showMessageDialog(this, "Primero carga los datos en la pestaña 1.");
            return;
        }
        int i = listaMetodos.getSelectedIndex();
        String nombre = (String) cbUsuario.getSelectedItem();
        String genero = (String) cbGenero.getSelectedItem();
        String[] cols;
        List<Object[]> filas = new ArrayList<>();
        String resumen;
        Map<String, Double> graf = null;
        String sufijo = "";
        String[] HC = {"Usuario", "Título", "Género", "Min. vistos", "Terminó", "Valoración", "Favorito", "Le gustó"};
        String[] CC = {"ID", "Título", "Tipo", "Género", "Año", "Duración", "Valoración"};

        switch (i) {
            case 0: {
                List<Historial> hs = consultas.historialDe(nombre);
                cols = HC; filas = filasHistorial(hs);
                resumen = nombre + " vio " + hs.size() + " títulos.";
                break;
            }
            case 1: {
                List<Contenido> cs = consultas.contenidoQueLeGustoA(nombre);
                cols = CC; filas = filasContenido(cs);
                resumen = "A " + nombre + " le gustaron " + cs.size() + " títulos.";
                break;
            }
            case 2: {
                String fav = consultas.generoFavoritoDe(nombre);
                cols = new String[]{"Indicador", "Valor"};
                filas.add(new Object[]{"Género favorito", fav == null ? "(sin historial)" : fav});
                filas.add(new Object[]{"Promedio de valoración (1-5)",
                    String.format(Locale.US, "%.2f", consultas.promedioValoracionDe(nombre))});
                filas.add(new Object[]{"Minutos totales vistos", consultas.minutosTotalesDe(nombre)});
                filas.add(new Object[]{"Interacciones", consultas.historialDe(nombre).size()});
                resumen = "Perfil de " + nombre + ".";
                break;
            }
            case 3: {
                Map<String, Integer> m = consultas.usuariosPorPais();
                cols = new String[]{"País", "Usuarios"};
                graf = new LinkedHashMap<>();
                int total = 0;
                for (Map.Entry<String, Integer> e : m.entrySet()) {
                    filas.add(new Object[]{e.getKey(), e.getValue()});
                    graf.put(e.getKey(), e.getValue().doubleValue());
                    total += e.getValue();
                }
                resumen = m.size() + " países, " + total + " usuarios.";
                break;
            }
            case 4: {
                List<Contenido> cs = consultas.contenidoPorGenero(genero);
                cols = CC; filas = filasContenido(cs);
                resumen = cs.size() + " títulos de " + genero + ".";
                break;
            }
            case 5: {
                int n = consultas.usuariosQueVieronGenero(genero);
                cols = new String[]{"Género", "Usuarios distintos"};
                filas.add(new Object[]{genero, n});
                resumen = n + " usuarios vieron al menos un título de " + genero + ".";
                break;
            }
            case 6: {
                int min = (Integer) spMinutos.getValue();
                List<Historial> hs = consultas.vistosMasDe(min);
                cols = HC; filas = filasHistorial(hs);
                resumen = hs.size() + " interacciones con más de " + min + " min vistos.";
                break;
            }
            case 7: {
                Map<String, Double> m = consultas.tasaDeLikePorGenero();
                cols = new String[]{"Género", "% me gusta"};
                for (Map.Entry<String, Double> e : m.entrySet()) {
                    filas.add(new Object[]{e.getKey(), String.format(Locale.US, "%.1f%%", e.getValue())});
                }
                graf = m; sufijo = "%";
                resumen = "Tasa de aprobación por género.";
                break;
            }
            case 8: {
                Map<String, Integer> m = consultas.minutosPorGenero();
                cols = new String[]{"Género", "Minutos totales"};
                graf = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> e : m.entrySet()) {
                    filas.add(new Object[]{e.getKey(), e.getValue()});
                    graf.put(e.getKey(), e.getValue().doubleValue());
                }
                resumen = "Minutos vistos por género.";
                break;
            }
            case 9: {
                int n = (Integer) spTopN.getValue();
                List<Map.Entry<Contenido, Integer>> top = consultas.masVistos(n);
                cols = new String[]{"#", "Título", "Género", "Minutos acumulados"};
                graf = new LinkedHashMap<>();
                int k = 1;
                for (Map.Entry<Contenido, Integer> e : top) {
                    filas.add(new Object[]{k++, e.getKey().getTitulo(), e.getKey().getGenero(), e.getValue()});
                    graf.put(e.getKey().getTitulo(), e.getValue().doubleValue());
                }
                resumen = "Top " + top.size() + " del contenido más visto.";
                break;
            }
            default: {
                int a = (Integer) spEdadMin.getValue(), b = (Integer) spEdadMax.getValue();
                List<Usuario> us = consultas.usuariosPorEdad(a, b);
                cols = new String[]{"ID", "Nombre", "País", "Edad", "Género"};
                for (Usuario u : us) {
                    filas.add(new Object[]{u.getId(), u.getNombre(), u.getPais(), u.getEdad(), u.getGenero()});
                }
                resumen = us.size() + " usuarios entre " + a + " y " + b + " años.";
            }
        }
        zonaResultado.removeAll();
        tablaResultado = new TablaPanel(cols);
        tablaResultado.setFilas(filas);
        zonaResultado.add(tablaResultado, BorderLayout.CENTER);
        zonaResultado.revalidate();
        zonaResultado.repaint();
        lblResumen.setText(resumen);
        grafico.mostrar(graf, resumen, sufijo);
    }

    private static List<Object[]> filasContenido(List<Contenido> cs) {
        List<Object[]> r = new ArrayList<>();
        for (Contenido c : cs) {
            r.add(new Object[]{c.getId(), c.getTitulo(), c.getTipo(), c.getGenero(), c.getAnio(),
                c.getDuracionMin(), String.format(Locale.US, "%.1f", c.getValoracionPromedio())});
        }
        return r;
    }

    // =========================================================== 4. DATOS DE PRUEBA
    private JComponent crearPestanaDatosPrueba() {
        JPanel p = panelBase(new BorderLayout(0, 8));

        JPanel barra = new JPanel(new BorderLayout(8, 0));
        barra.setOpaque(false);
        JLabel ayuda = etiqueta("Agrega tus propios usuarios, películas e interacciones. "
                + "Cada fila se guarda en la carpeta de datos y se vuelve a leer desde el CSV.");
        JButton recargar = boton("Recargar y verificar lectura");
        recargar.addActionListener(e -> cargar());
        barra.add(ayuda, BorderLayout.CENTER);
        barra.add(recargar, BorderLayout.EAST);

        JTabbedPane sub = new JTabbedPane();
        sub.setBackground(BG);
        sub.setFont(new Font("SansSerif", Font.BOLD, 12));
        sub.addTab("Usuarios", crearFormUsuario());
        sub.addTab("Películas / Series", crearFormPelicula());
        sub.addTab("Interacciones (historial)", crearFormInteraccion());

        p.add(barra, BorderLayout.NORTH);
        p.add(sub, BorderLayout.CENTER);
        return p;
    }

    private JComponent crearFormUsuario() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 8);
        g.anchor = GridBagConstraints.WEST;

        tfUsuId = campo("U041");
        tfUsuNombre = campo("");
        tfUsuPais = campo("Perú");
        spUsuEdad = new JSpinner(new SpinnerNumberModel(25, 1, 120, 1));
        cbUsuGenero = combo(new String[]{"M", "F"}, false);

        int y = 0;
        filaForm(form, g, y++, "ID:", tfUsuId);
        filaForm(form, g, y++, "Nombre:", tfUsuNombre);
        filaForm(form, g, y++, "País:", tfUsuPais);
        filaForm(form, g, y++, "Edad:", spUsuEdad);
        filaForm(form, g, y++, "Género:", cbUsuGenero);

        JButton guardar = boton("Guardar usuario en usuarios.csv");
        guardar.addActionListener(e -> agregarUsuario());
        g.gridy = y;
        g.gridx = 0;
        g.gridwidth = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(guardar, g);

        panel.add(tarjeta(form, "NUEVO USUARIO · se guarda en usuarios.csv"), BorderLayout.NORTH);

        lblDatosUsuarios = new JLabel("Aún no se ha agregado ningún usuario.");
        lblDatosUsuarios.setForeground(MUTED);
        lblDatosUsuarios.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        tUltimosUsuarios = new TablaPanel(new String[]{"ID", "Nombre", "País", "Edad", "Género"});
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.setOpaque(false);
        abajo.add(lblDatosUsuarios, BorderLayout.NORTH);
        abajo.add(tUltimosUsuarios, BorderLayout.CENTER);
        panel.add(tarjeta(abajo, "ÚLTIMOS USUARIOS LEÍDOS DESDE EL CSV"), BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearFormPelicula() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 8);
        g.anchor = GridBagConstraints.WEST;

        tfPeliId = campo("C041");
        tfPeliTitulo = campo("");
        cbPeliTipo = combo(new String[]{"Película", "Serie"}, false);
        cbPeliGenero = combo(new String[0], true);
        spPeliAnio = new JSpinner(new SpinnerNumberModel(2024, 1900, 2100, 1));
        spPeliAnio.setEditor(new JSpinner.NumberEditor(spPeliAnio, "0"));
        spPeliDuracion = new JSpinner(new SpinnerNumberModel(100, 1, 600, 1));
        spPeliDuracion.setEditor(new JSpinner.NumberEditor(spPeliDuracion, "0"));
        cbPeliIdioma = combo(new String[0], true);
        tfPeliDirector = campo("");
        cbPeliPais = combo(new String[0], true);
        spPeliValoracion = new JSpinner(new SpinnerNumberModel(4.0, 0.0, 5.0, 0.1));
        spPeliValoracion.setEditor(new JSpinner.NumberEditor(spPeliValoracion, "0.0"));

        int y = 0;
        filaForm(form, g, y++, "ID:", tfPeliId);
        filaForm(form, g, y++, "Título:", tfPeliTitulo);
        filaForm(form, g, y++, "Tipo:", cbPeliTipo);
        filaForm(form, g, y++, "Género:", cbPeliGenero);
        filaForm(form, g, y++, "Año:", spPeliAnio);
        filaForm(form, g, y++, "Duración (min):", spPeliDuracion);
        filaForm(form, g, y++, "Idioma original:", cbPeliIdioma);
        filaForm(form, g, y++, "Director:", tfPeliDirector);
        filaForm(form, g, y++, "País de origen:", cbPeliPais);
        filaForm(form, g, y++, "Valoración (1-5):", spPeliValoracion);

        JButton guardar = boton("Guardar película en peliculas.csv");
        guardar.addActionListener(e -> agregarPelicula());
        g.gridy = y;
        g.gridx = 0;
        g.gridwidth = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(guardar, g);

        panel.add(tarjeta(form, "NUEVA PELÍCULA / SERIE · se guarda en peliculas.csv"), BorderLayout.NORTH);

        lblDatosPeliculas = new JLabel("Aún no se ha agregado ninguna película.");
        lblDatosPeliculas.setForeground(MUTED);
        lblDatosPeliculas.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        tUltimosContenidos = new TablaPanel(new String[]{"ID", "Título", "Tipo", "Género", "Año",
            "Duración", "Idioma", "Director", "País", "Valoración"});
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.setOpaque(false);
        abajo.add(lblDatosPeliculas, BorderLayout.NORTH);
        abajo.add(tUltimosContenidos, BorderLayout.CENTER);
        panel.add(tarjeta(abajo, "ÚLTIMOS TÍTULOS LEÍDOS DESDE EL CSV"), BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearFormInteraccion() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 8);
        g.anchor = GridBagConstraints.WEST;

        cbInterUsuario = combo(new String[0], false);
        cbInterUsuario.setPreferredSize(new Dimension(300, 26));
        cbInterPelicula = combo(new String[0], false);
        cbInterPelicula.setPreferredSize(new Dimension(300, 26));
        spInterMinutos = new JSpinner(new SpinnerNumberModel(60, 0, 600, 5));
        spInterValoracion = new JSpinner(new SpinnerNumberModel(4, 1, 5, 1));
        chkInterTermino = casilla("Terminó de verla", true);
        chkInterFavorito = casilla("La guardó en favoritos", false);
        chkInterGusto = casilla("Le gustó (1 = sí)", true);

        int y = 0;
        filaForm(form, g, y++, "Usuario:", cbInterUsuario);
        filaForm(form, g, y++, "Película / Serie:", cbInterPelicula);
        filaForm(form, g, y++, "Minutos vistos:", spInterMinutos);
        filaForm(form, g, y++, "Valoración (1-5):", spInterValoracion);
        filaForm(form, g, y++, "", chkInterTermino);
        filaForm(form, g, y++, "", chkInterFavorito);
        filaForm(form, g, y++, "", chkInterGusto);

        JButton guardar = boton("Guardar interacción en historial.csv");
        guardar.addActionListener(e -> agregarInteraccion());
        g.gridy = y;
        g.gridx = 0;
        g.gridwidth = 2;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(guardar, g);

        panel.add(tarjeta(form, "NUEVA INTERACCIÓN · se guarda en historial.csv"), BorderLayout.NORTH);

        lblDatosInteracciones = new JLabel("Una interacción es lo que vio un usuario y si le gustó.");
        lblDatosInteracciones.setForeground(MUTED);
        lblDatosInteracciones.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        tUltimosHistorial = new TablaPanel(new String[]{"Usuario", "Título", "Género", "Min. vistos",
            "Terminó", "Valoración", "Favorito", "Le gustó"});
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        abajo.setOpaque(false);
        abajo.add(lblDatosInteracciones, BorderLayout.NORTH);
        abajo.add(tUltimosHistorial, BorderLayout.CENTER);
        panel.add(tarjeta(abajo, "ÚLTIMAS INTERACCIONES LEÍDAS DESDE EL CSV"), BorderLayout.CENTER);
        return panel;
    }

    private static void filaForm(JPanel form, GridBagConstraints g, int y, String texto, JComponent campo) {
        g.gridy = y;
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        form.add(etiquetaCampo(texto), g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(campo, g);
    }

    private static void animarConteo(JLabel etiqueta, int destino) {
        Object previo = etiqueta.getClientProperty("conteo");
        if (previo instanceof javax.swing.Timer) ((javax.swing.Timer) previo).stop();
        final int[] paso = {0};
        javax.swing.Timer t = new javax.swing.Timer(16, null);
        t.addActionListener(e -> {
            paso[0]++;
            double p = Math.min(1, paso[0] / 28.0);
            double f = p * p * (3 - 2 * p);
            etiqueta.setText(String.valueOf((int) Math.round(destino * f)));
            if (paso[0] >= 28) { etiqueta.setText(String.valueOf(destino)); t.stop(); }
        });
        etiqueta.putClientProperty("conteo", t);
        t.start();
    }

    private static JTextField campo(String texto) {
        JTextField f = new JTextField(texto, 22);
        estilizarCampo(f);
        return f;
    }

    private static JComboBox<String> combo(String[] items, boolean editable) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setEditable(editable);
        cb.setPreferredSize(new Dimension(240, 26));
        return cb;
    }

    private static JCheckBox casilla(String texto, boolean seleccionada) {
        JCheckBox c = new JCheckBox(texto, seleccionada);
        c.setForeground(TEXT);
        c.setOpaque(false);
        c.setFocusPainted(false);
        return c;
    }

    private void agregarUsuario() {
        if (bd == null) {
            aviso("Primero carga los datos en la pestaña 1.");
            return;
        }
        String id = tfUsuId.getText().trim().toUpperCase(Locale.ROOT);
        String nombre = tfUsuNombre.getText().trim();
        String pais = tfUsuPais.getText().trim();
        int edad = (Integer) spUsuEdad.getValue();
        String genero = (String) cbUsuGenero.getSelectedItem();
        if (!textoValido(id, nombre, pais, genero)) {
            aviso("Completa todos los campos. No se permiten comas ni saltos de línea.");
            return;
        }
        if (bd.getUsuario(id) != null) {
            aviso("El ID " + id + " ya existe. Usa otro.");
            return;
        }
        try {
            escribirLinea("usuarios.csv", id + "," + nombre + "," + pais + "," + edad + "," + genero);
            cargar();
            Usuario leido = bd.getUsuario(id);
            if (leido == null) {
                aviso("La fila se escribió, pero no se pudo releer. Revisa usuarios.csv.");
                return;
            }
            lblDatosUsuarios.setText("✔ Leído desde usuarios.csv: " + leido);
            JOptionPane.showMessageDialog(this, "Usuario agregado y verificado:\n" + leido,
                    "Lectura correcta", JOptionPane.INFORMATION_MESSAGE);
            tfUsuId.setText(siguienteIdUsuarios());
            tfUsuNombre.setText("");
        } catch (IOException ex) {
            aviso("No se pudo escribir usuarios.csv:\n" + ex.getMessage());
        }
    }

    private void agregarPelicula() {
        if (bd == null) {
            aviso("Primero carga los datos en la pestaña 1.");
            return;
        }
        String id = tfPeliId.getText().trim().toUpperCase(Locale.ROOT);
        String titulo = tfPeliTitulo.getText().trim();
        String tipo = (String) cbPeliTipo.getSelectedItem();
        String genero = textoCombo(cbPeliGenero);
        String idioma = textoCombo(cbPeliIdioma);
        String director = tfPeliDirector.getText().trim();
        String pais = textoCombo(cbPeliPais);
        int anio = (Integer) spPeliAnio.getValue();
        int duracion = (Integer) spPeliDuracion.getValue();
        double valoracion = (Double) spPeliValoracion.getValue();

        if (!textoValido(id, titulo, tipo, genero, idioma, director, pais)) {
            aviso("Completa todos los campos. No se permiten comas ni saltos de línea.");
            return;
        }
        if (bd.getContenido(id) != null) {
            aviso("El ID " + id + " ya existe. Usa otro.");
            return;
        }
        String linea = String.join(",", id, titulo, tipo, genero, String.valueOf(anio),
                String.valueOf(duracion), idioma, director, pais,
                String.format(Locale.US, "%.1f", valoracion));
        try {
            escribirLinea("peliculas.csv", linea);
            cargar();
            Contenido leido = bd.getContenido(id);
            if (leido == null) {
                aviso("La fila se escribió, pero no se pudo releer. Revisa peliculas.csv.");
                return;
            }
            lblDatosPeliculas.setText("✔ Leído desde peliculas.csv: " + leido);
            JOptionPane.showMessageDialog(this, "Película agregada y verificada:\n" + leido,
                    "Lectura correcta", JOptionPane.INFORMATION_MESSAGE);
            tfPeliId.setText(siguienteIdContenidos());
            tfPeliTitulo.setText("");
            tfPeliDirector.setText("");
        } catch (IOException ex) {
            aviso("No se pudo escribir peliculas.csv:\n" + ex.getMessage());
        }
    }

    private void agregarInteraccion() {
        if (bd == null) {
            aviso("Primero carga los datos en la pestaña 1.");
            return;
        }
        String usuarioId = idDeItem((String) cbInterUsuario.getSelectedItem());
        String contenidoId = idDeItem((String) cbInterPelicula.getSelectedItem());
        if (usuarioId.isEmpty() || contenidoId.isEmpty()) {
            aviso("No hay usuarios o películas cargadas.");
            return;
        }
        int minutos = (Integer) spInterMinutos.getValue();
        int valoracion = (Integer) spInterValoracion.getValue();
        boolean termino = chkInterTermino.isSelected();
        boolean favorito = chkInterFavorito.isSelected();
        boolean leGusto = chkInterGusto.isSelected();
        int antes = bd.getHistorial().size();
        String linea = String.join(",", usuarioId, contenidoId, String.valueOf(minutos),
                termino ? "1" : "0", String.valueOf(valoracion), favorito ? "1" : "0",
                leGusto ? "1" : "0");
        try {
            escribirLinea("historial.csv", linea);
            cargar();
            int despues = bd.getHistorial().size();
            if (despues != antes + 1) {
                aviso("La fila se escribió, pero el historial no aumentó. Revisa historial.csv.");
                return;
            }
            Historial ultimo = bd.getHistorial().get(despues - 1);
            lblDatosInteracciones.setText("✔ Leído desde historial.csv: " + ultimo);
            JOptionPane.showMessageDialog(this,
                    "Interacción agregada y verificada.\nSe releerán " + despues + " interacciones.",
                    "Lectura correcta", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            aviso("No se pudo escribir historial.csv:\n" + ex.getMessage());
        }
    }

    /** Agrega una línea al CSV conservando la codificación UTF-8 y el encabezado. */
    private void escribirLinea(String archivo, String linea) throws IOException {
        Path ruta = Paths.get(campoCarpeta.getText().trim(), archivo);
        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        lineas.add(linea);
        Files.write(ruta, lineas, StandardCharsets.UTF_8);
    }

    private static boolean textoValido(String... valores) {
        for (String v : valores) {
            if (v == null || v.trim().isEmpty()) return false;
            if (v.contains(",") || v.contains("\n") || v.contains("\r")) return false;
        }
        return true;
    }

    private static String textoCombo(JComboBox<String> cb) {
        Object item = cb.isEditable() ? cb.getEditor().getItem() : cb.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    private static String idDeItem(String item) {
        if (item == null) return "";
        int corte = item.indexOf(" · ");
        return corte < 0 ? item.trim() : item.substring(0, corte).trim();
    }

    private String siguienteIdUsuarios() {
        int max = 0;
        if (bd != null) for (Usuario u : bd.getUsuarios()) max = Math.max(max, numeroSufijo(u.getId()));
        return String.format("U%03d", max + 1);
    }

    private String siguienteIdContenidos() {
        int max = 0;
        if (bd != null) for (Contenido c : bd.getContenidos()) max = Math.max(max, numeroSufijo(c.getId()));
        return String.format("C%03d", max + 1);
    }

    private static int numeroSufijo(String id) {
        int i = 0;
        while (i < id.length() && !Character.isDigit(id.charAt(i))) i++;
        if (i >= id.length()) return 0;
        try {
            return Integer.parseInt(id.substring(i));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refrescarDatosPrueba() {
        if (bd == null) return;

        if (tfUsuId != null) {
            String actual = tfUsuId.getText().trim();
            if (actual.isEmpty() || bd.getUsuario(actual) != null) tfUsuId.setText(siguienteIdUsuarios());
        }
        if (tfPeliId != null) {
            String actual = tfPeliId.getText().trim();
            if (actual.isEmpty() || bd.getContenido(actual) != null) tfPeliId.setText(siguienteIdContenidos());
        }

        TreeSet<String> generos = new TreeSet<>();
        TreeSet<String> idiomas = new TreeSet<>();
        TreeSet<String> paises = new TreeSet<>();
        for (Contenido c : bd.getContenidos()) {
            generos.add(c.getGenero());
            idiomas.add(c.getIdiomaOriginal());
            paises.add(c.getPais());
        }
        if (cbPeliGenero != null) {
            cbPeliGenero.removeAllItems();
            for (String s : generos) cbPeliGenero.addItem(s);
            if (cbPeliGenero.getItemCount() > 0) cbPeliGenero.setSelectedIndex(0);
        }
        if (cbPeliIdioma != null) {
            cbPeliIdioma.removeAllItems();
            for (String s : idiomas) cbPeliIdioma.addItem(s);
            if (cbPeliIdioma.getItemCount() > 0) cbPeliIdioma.setSelectedIndex(0);
        }
        if (cbPeliPais != null) {
            cbPeliPais.removeAllItems();
            for (String s : paises) cbPeliPais.addItem(s);
            if (cbPeliPais.getItemCount() > 0) cbPeliPais.setSelectedIndex(0);
        }

        String selUsuario = (String) cbInterUsuario.getSelectedItem();
        cbInterUsuario.removeAllItems();
        for (Usuario u : bd.getUsuarios()) cbInterUsuario.addItem(u.getId() + " · " + u.getNombre());
        if (selUsuario != null) cbInterUsuario.setSelectedItem(selUsuario);

        String selContenido = (String) cbInterPelicula.getSelectedItem();
        cbInterPelicula.removeAllItems();
        for (Contenido c : bd.getContenidos()) cbInterPelicula.addItem(c.getId() + " · " + c.getTitulo());
        if (selContenido != null) cbInterPelicula.setSelectedItem(selContenido);

        List<Object[]> filasU = new ArrayList<>();
        List<Usuario> usuarios = bd.getUsuarios();
        for (int i = Math.max(0, usuarios.size() - 5); i < usuarios.size(); i++) {
            Usuario x = usuarios.get(i);
            filasU.add(new Object[]{x.getId(), x.getNombre(), x.getPais(), x.getEdad(), x.getGenero()});
        }
        tUltimosUsuarios.setFilas(filasU);

        List<Object[]> filasC = new ArrayList<>();
        List<Contenido> contenidos = bd.getContenidos();
        for (int i = Math.max(0, contenidos.size() - 5); i < contenidos.size(); i++) {
            Contenido x = contenidos.get(i);
            filasC.add(new Object[]{x.getId(), x.getTitulo(), x.getTipo(), x.getGenero(), x.getAnio(),
                x.getDuracionMin(), x.getIdiomaOriginal(), x.getDirector(), x.getPais(),
                String.format(Locale.US, "%.1f", x.getValoracionPromedio())});
        }
        tUltimosContenidos.setFilas(filasC);

        List<Historial> hs = bd.getHistorial();
        int desde = Math.max(0, hs.size() - 5);
        tUltimosHistorial.setFilas(filasHistorial(hs.subList(desde, hs.size())));
    }

    // =========================================================== 5. ADMINISTRADOR
    private JComponent crearPestanaAdmin() {
        JPanel p = panelBase(new BorderLayout(10, 10));

        JPanel barra = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 4));
        barra.setOpaque(false);
        cbAdminUsuario = new JComboBox<>();
        cbAdminUsuario.setPreferredSize(new Dimension(280, 28));
        spAdminTop = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        spAdminTop.setPreferredSize(new Dimension(64, 28));
        chkAdminTodas = casilla("Mostrar también las NO recomendadas", false);

        JButton bAnalizar = boton("▶  Analizar patrón");
        JButton bPublicar = boton("Publicar recomendaciones");
        JButton bVer = boton("Ver publicaciones");

        barra.add(etiqueta("Usuario:"));
        barra.add(cbAdminUsuario);
        barra.add(bAnalizar);
        barra.add(separadorVertical());
        barra.add(etiqueta("Publicar top:"));
        barra.add(spAdminTop);
        barra.add(bPublicar);
        barra.add(bVer);
        p.add(tarjeta(barra, "ADMINISTRADOR · elige un usuario para ver su patrón"), BorderLayout.NORTH);

        areaPatron = areaTexto();
        areaRuta = areaTexto();

        JComponent cardPatron = tarjeta(scrollTexto(areaPatron),
                "Patrón del usuario (árbol de decisiones)");
        JComponent cardRuta = tarjeta(scrollTexto(areaRuta),
                "Ruta de la película seleccionada");

        JSplitPane splitIzq = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cardPatron, cardRuta);
        splitIzq.setResizeWeight(0.38);
        splitIzq.setDividerSize(8);
        splitIzq.setBorder(null);
        splitIzq.setOpaque(false);
        divisorOscuro(splitIzq);
        JPanel izq = new JPanel(new BorderLayout());
        izq.setOpaque(false);
        izq.add(splitIzq, BorderLayout.CENTER);
        izq.setPreferredSize(new Dimension(430, 100));

        lblAdminResumen = new JLabel("Elige un usuario y pulsa «Analizar patrón».");
        lblAdminResumen.setForeground(AMBAR);
        lblAdminResumen.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblAdminResumen.setBorder(BorderFactory.createEmptyBorder(2, 4, 8, 0));

        tablaAdmin = new TablaPanel(new String[]{"#", "Título", "Género", "% modelo",
            "Patrón (árbol)", "Confianza", "Decisión"});
        tablaGustos = new TablaPanel(new String[]{"Título", "Género", "Tipo", "Valoración", "Favorito"});
        tablaPublicadas = new TablaPanel(new String[]{"Fecha", "Usuario", "Título", "Género",
            "% modelo", "Patrón", "Decisión"});
        subAdmin = new JTabbedPane();
        subAdmin.setBackground(BG);
        subAdmin.setFont(new Font("SansSerif", Font.BOLD, 12));
        subAdmin.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 0));
        subAdmin.addTab("Recomendaciones (árbol)", tablaAdmin);
        subAdmin.addTab("Le gustaron", tablaGustos);
        subAdmin.addTab("Publicadas", tablaPublicadas);

        JPanel pieDer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 6));
        pieDer.setOpaque(false);
        pieDer.add(chkAdminTodas);

        JPanel der = new JPanel(new BorderLayout(0, 4));
        der.setOpaque(false);
        der.add(lblAdminResumen, BorderLayout.NORTH);
        der.add(subAdmin, BorderLayout.CENTER);
        der.add(pieDer, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izq, der);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(10);
        divisorOscuro(split);
        split.setDividerLocation(430);
        p.add(split, BorderLayout.CENTER);

        cbAdminUsuario.addActionListener(e -> {
            if (!actualizandoAdmin) analizar();
        });
        bAnalizar.addActionListener(e -> analizar());
        chkAdminTodas.addActionListener(e -> pintarTablaAdmin());
        bPublicar.addActionListener(e -> publicar());
        bVer.addActionListener(e -> {
            mostrarPublicaciones();
            subAdmin.setSelectedIndex(2);
        });
        tablaAdmin.getTabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarRutaSeleccionada();
        });
        return p;
    }

    /** Línea vertical fina para separar grupos de controles. */
    private static JComponent separadorVertical() {
        JPanel s = new JPanel();
        s.setOpaque(false);
        s.setPreferredSize(new Dimension(1, 26));
        s.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDE));
        return s;
    }

    private static JTextArea areaTexto() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setBackground(PANEL);
        a.setForeground(TEXT);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        a.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    private static JScrollPane scrollTexto(JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createLineBorder(ALT));
        return sp;
    }

    private void refrescarComboAdmin() {
        if (cbAdminUsuario == null) return;
        actualizandoAdmin = true;
        String seleccion = (String) cbAdminUsuario.getSelectedItem();
        usuariosAdmin.clear();
        if (bd != null) usuariosAdmin.addAll(bd.getUsuarios());
        String[] items = new String[usuariosAdmin.size()];
        for (int i = 0; i < items.length; i++) {
            Usuario u = usuariosAdmin.get(i);
            items[i] = u.getId() + " · " + u.getNombre();
        }
        cbAdminUsuario.setModel(new javax.swing.DefaultComboBoxModel<>(items));
        if (seleccion != null) cbAdminUsuario.setSelectedItem(seleccion);
        actualizandoAdmin = false;
        if (!usuariosAdmin.isEmpty()) analizar();
        else limpiarAdmin();
    }

    private Usuario usuarioAdminSeleccionado() {
        if (cbAdminUsuario == null) return null;
        int i = cbAdminUsuario.getSelectedIndex();
        if (i < 0 || i >= usuariosAdmin.size()) return null;
        return usuariosAdmin.get(i);
    }

    private void analizar() {
        if (bd == null || arbolReglas == null || hibridoAdmin == null) {
            limpiarAdmin();
            return;
        }
        Usuario u = usuarioAdminSeleccionado();
        if (u == null) {
            limpiarAdmin();
            return;
        }
        ArbolReglas.Resumen res = arbolReglas.resumen(u);
        areaPatron.setText(textoPatron(res));

        todasAdmin = new ArrayList<>();
        java.util.Set<String> vistos = bd.idsVistosPor(u);
        for (Contenido c : bd.getContenidos()) {
            if (vistos.contains(c.getId())) continue;
            double probabilidad = hibridoAdmin.puntaje(u, c);
            ArbolReglas.Resultado resultado = arbolReglas.evaluar(u, c);
            todasAdmin.add(new FilaAdmin(c, probabilidad, resultado));
        }
        todasAdmin.sort((a, b) -> {
            int ca = ordenCategoria(a.resultado.categoria);
            int cb = ordenCategoria(b.resultado.categoria);
            if (ca != cb) return Integer.compare(ca, cb);
            return Double.compare(b.probabilidad, a.probabilidad);
        });

        List<Historial> gustos = new ArrayList<>();
        for (Historial h : bd.historialDe(u)) if (h.isLeGusto()) gustos.add(h);
        gustos.sort((a, b) -> Integer.compare(b.getValoracion(), a.getValoracion()));
        List<Object[]> filasGustos = new ArrayList<>();
        for (Historial h : gustos) {
            filasGustos.add(new Object[]{h.getContenido().getTitulo(), h.getContenido().getGenero(),
                h.getContenido().getTipo(), h.getValoracion(), h.isFavorito() ? "Sí" : "No"});
        }
        tablaGustos.setFilas(filasGustos);
        pintarTablaAdmin();
    }

    private void pintarTablaAdmin() {
        if (tablaAdmin == null) return;
        boolean mostrarTodas = chkAdminTodas != null && chkAdminTodas.isSelected();
        filasAdmin = new ArrayList<>();
        for (FilaAdmin f : todasAdmin) {
            if (mostrarTodas || f.resultado.recomendar) filasAdmin.add(f);
        }
        List<Object[]> filas = new ArrayList<>();
        int i = 1;
        for (FilaAdmin f : filasAdmin) {
            filas.add(new Object[]{i++, f.contenido.getTitulo(), f.contenido.getGenero(),
                String.format(Locale.US, "%.1f%%", f.probabilidad * 100),
                f.resultado.categoria,
                String.format(Locale.US, "%.0f%%", f.resultado.confianza * 100),
                f.resultado.recomendar ? "Sí" : "No"});
        }
        tablaAdmin.setFilas(filas);

        int recomendadas = 0;
        for (FilaAdmin f : todasAdmin) if (f.resultado.recomendar) recomendadas++;
        int limite = (Integer) spAdminTop.getValue();
        lblAdminResumen.setText("Recomendadas por el árbol: " + recomendadas + " de "
                + todasAdmin.size() + " candidatas  ·  se publicarán hasta " + limite + ".");

        if (!filasAdmin.isEmpty()) {
            tablaAdmin.getTabla().setRowSelectionInterval(0, 0);
        } else {
            areaRuta.setText("El árbol no recomienda ninguna película con los datos actuales.\n"
                    + "Marca «Mostrar también las NO recomendadas» para ver el motivo de cada una.");
        }
    }

    private static int ordenCategoria(String categoria) {
        if ("Alta Prioridad".equals(categoria)) return 0;
        if ("Por Contenido".equals(categoria)) return 1;
        if ("Exploración".equals(categoria)) return 2;
        return 3;
    }

    private static String textoPatron(ArbolReglas.Resumen r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usuario: ").append(r.usuario.getId()).append(" · ").append(r.usuario.getNombre())
                .append("\nEdad: ").append(r.usuario.getEdad())
                .append(" · País: ").append(r.usuario.getPais()).append("\n");
        sb.append("\nInteracciones: ").append(r.interacciones)
                .append(" · Me gusta: ").append(r.meGusta);
        if (r.interacciones > 0) {
            sb.append(String.format(Locale.US, " (%.1f%%)", 100.0 * r.meGusta / r.interacciones));
        }
        sb.append("\n\nGénero preferido: ")
                .append(r.generoFavorito == null ? "(sin historial)" : r.generoFavorito).append("\n");
        if (r.generoFavorito != null) {
            sb.append(String.format(Locale.US, "  %.1f%% de sus «me gusta»%n",
                    r.porcentajeGeneroFavorito * 100));
        }
        sb.append(String.format(Locale.US, "Tiempo visto (todo el historial): %.1f%%%n",
                r.porcentajeVisto * 100));
        sb.append(String.format(Locale.US,
                "Tiempo visto (género preferido): %.1f%%  → regla >70%%: %s%n",
                r.porcentajeVistoGenero * 100, r.porcentajeVistoGenero > 0.70 ? "SÍ" : "NO"));
        sb.append("Director preferido: ")
                .append(r.directorPreferido == null ? "sin dato" : r.directorPreferido).append("\n");
        sb.append("Idioma preferido: ")
                .append(r.idiomaPreferido == null ? "sin dato" : r.idiomaPreferido).append("\n");
        sb.append("Usuarios similares: ").append(r.vecinosSimilares).append("\n");
        sb.append("Tendencias globales (top): ").append(r.totalTendencias).append("\n");
        return sb.toString();
    }

    private void mostrarRutaSeleccionada() {
        if (tablaAdmin == null || filasAdmin.isEmpty()) return;
        int vista = tablaAdmin.getTabla().getSelectedRow();
        if (vista < 0) return;
        int modelo = tablaAdmin.getTabla().convertRowIndexToModel(vista);
        if (modelo < 0 || modelo >= filasAdmin.size()) return;
        FilaAdmin f = filasAdmin.get(modelo);
        Usuario u = usuarioAdminSeleccionado();

        StringBuilder sb = new StringBuilder();
        sb.append("Película: ").append(f.contenido.getTitulo())
                .append(" (").append(f.contenido.getGenero()).append(")\n");
        sb.append(String.format(Locale.US, "Valoración global: %.1f★ · Duración: %d min%n",
                f.contenido.getValoracionPromedio(), f.contenido.getDuracionMin()));
        sb.append(String.format(Locale.US, "Modelo híbrido: %.1f%% de probabilidad de gustar%n%n",
                f.probabilidad * 100));
        sb.append("Ruta del árbol:\n");
        for (String paso : f.resultado.ruta) {
            sb.append("  • ").append(paso).append("\n");
        }
        boolean usoEdadPais = false;
        for (String paso : f.resultado.ruta) if (paso.startsWith("¿Es acorde")) usoEdadPais = true;
        if (usoEdadPais && u != null) {
            sb.append("  ").append(arbolReglas.detalleEdadYPais(u, f.contenido)).append("\n");
        }
        sb.append("\n→ ").append(f.resultado.recomendar ? "RECOMENDAR" : "NO RECOMENDAR")
                .append(" (").append(f.resultado.categoria).append(")\n");
        sb.append(String.format(Locale.US, "Confianza de la regla: %.0f%%",
                f.resultado.confianza * 100));
        areaRuta.setText(sb.toString());
        areaRuta.setCaretPosition(0);
    }

    private void publicar() {
        if (todasAdmin.isEmpty()) {
            aviso("Primero elige un usuario en la pestaña 5 para analizar su patrón.");
            return;
        }
        Usuario u = usuarioAdminSeleccionado();
        if (u == null) return;
        int limite = (Integer) spAdminTop.getValue();
        List<FilaAdmin> publicar = new ArrayList<>();
        for (FilaAdmin f : todasAdmin) {
            if (f.resultado.recomendar && publicar.size() < limite) publicar.add(f);
        }
        if (publicar.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El árbol no recomienda ninguna película para " + u.getNombre() + ".\n\n"
                            + "Revisa el patrón: tiempo visto del género, valoración de usuarios similares,\n"
                            + "director/idioma, tendencias y edad/país.",
                    "Sin recomendaciones", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Path ruta = Paths.get(campoCarpeta.getText().trim(), "publicaciones.csv");
            List<String> lineas = Files.exists(ruta)
                    ? Files.readAllLines(ruta, StandardCharsets.UTF_8)
                    : new ArrayList<>();
            if (lineas.isEmpty()) {
                lineas.add("fecha,usuario_id,usuario,contenido_id,titulo,genero,porcentaje,patron,decision");
            }
            for (FilaAdmin f : publicar) {
                lineas.add(String.join(",",
                        LocalDate.now().toString(),
                        u.getId(),
                        limpiarCsv(u.getNombre()),
                        f.contenido.getId(),
                        limpiarCsv(f.contenido.getTitulo()),
                        limpiarCsv(f.contenido.getGenero()),
                        String.format(Locale.US, "%.1f", f.probabilidad * 100),
                        limpiarCsv(f.resultado.categoria),
                        "Sí"));
            }
            Files.write(ruta, lineas, StandardCharsets.UTF_8);
            mostrarPublicaciones();
            subAdmin.setSelectedIndex(2);
            mostrarDialogoPublicacion(u, publicar);
        } catch (IOException ex) {
            aviso("No se pudo escribir publicaciones.csv:\n" + ex.getMessage());
        }
    }

    private void mostrarDialogoPublicacion(Usuario u, List<FilaAdmin> publicadas) {
        List<Object[]> filas = new ArrayList<>();
        int i = 1;
        for (FilaAdmin f : publicadas) {
            filas.add(new Object[]{i++, f.contenido.getTitulo(), f.contenido.getGenero(),
                String.format(Locale.US, "%.1f%%", f.probabilidad * 100),
                f.resultado.categoria});
        }
        TablaPanel tabla = new TablaPanel(new String[]{"#", "Título", "Género", "% modelo", "Patrón"});
        tabla.setFilas(filas);

        JDialog d = new JDialog(this, "Publicado para " + u.getNombre(), true);
        d.setSize(760, 340);
        d.setLocationRelativeTo(this);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel titulo = new JLabel("✔ Publicado en "
                + Paths.get(campoCarpeta.getText().trim()).getFileName()
                + "/publicaciones.csv — " + filas.size()
                + " recomendaciones para " + u.getNombre());
        titulo.setForeground(AMBAR);
        p.add(titulo, BorderLayout.NORTH);
        p.add(tabla, BorderLayout.CENTER);
        JButton cerrar = boton("Cerrar");
        cerrar.addActionListener(e -> d.dispose());
        JPanel pie = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        pie.setOpaque(false);
        pie.add(cerrar);
        p.add(pie, BorderLayout.SOUTH);
        d.setContentPane(p);
        d.setVisible(true);
    }

    private void mostrarPublicaciones() {
        if (tablaPublicadas == null) return;
        Path ruta = Paths.get(campoCarpeta.getText().trim(), "publicaciones.csv");
        List<Object[]> filas = new ArrayList<>();
        if (Files.exists(ruta)) {
            try {
                List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
                for (int i = 1; i < lineas.size(); i++) {
                    String[] f = lineas.get(i).split(",", -1);
                    if (f.length < 9) continue;
                    filas.add(new Object[]{f[0], f[2], f[4], f[5], f[6] + "%", f[7], f[8]});
                }
            } catch (IOException ignorado) {
                // si no se puede leer, se muestra la tabla vacía
            }
        }
        tablaPublicadas.setFilas(filas);
    }

    private void limpiarAdmin() {
        todasAdmin = new ArrayList<>();
        filasAdmin = new ArrayList<>();
        if (tablaAdmin != null) tablaAdmin.setFilas(new ArrayList<>());
        if (tablaGustos != null) tablaGustos.setFilas(new ArrayList<>());
        if (lblAdminResumen != null) lblAdminResumen.setText("Sin datos cargados. Ve a la pestaña 1.");
        if (areaPatron != null) areaPatron.setText("");
        if (areaRuta != null) areaRuta.setText("");
    }

    private static String limpiarCsv(String s) {
        return s == null ? "" : s.replace(',', ';').replace('\n', ' ').replace('\r', ' ');
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    // =========================================================== 6. CÓDIGO
    private JComponent crearPestanaCodigo() {
        String[] archivos = {"datos/CargadorDatos.java", "consultas/ConsultaDatos.java", "modelo/BaseDatos.java",
            "modelo/Usuario.java", "modelo/Contenido.java", "modelo/Historial.java", "modelo/Recomendacion.java"};
        cbArchivo = new JComboBox<>(archivos);
        areaCodigo = new JTextArea();
        areaCodigo.setEditable(false);
        areaCodigo.setBackground(PANEL);
        areaCodigo.setForeground(TEXT);
        areaCodigo.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaCodigo.setTabSize(4);
        cbArchivo.addActionListener(e -> mostrarCodigo());
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barra.setOpaque(false);
        barra.add(etiqueta("Clase:"));
        barra.add(cbArchivo);
        JPanel p = panelBase(new BorderLayout(0, 6));
        p.add(barra, BorderLayout.NORTH);
        p.add(new JScrollPane(areaCodigo), BorderLayout.CENTER);
        mostrarCodigo();
        return p;
    }

    private void mostrarCodigo() {
        String rel = (String) cbArchivo.getSelectedItem();
        Path ruta = Paths.get("src", "main", "java", "streamflix", rel);
        try {
            areaCodigo.setText(new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8));
        } catch (IOException e) {
            areaCodigo.setText("No se encontró " + ruta.toAbsolutePath()
                    + "\n(ejecuta la aplicación desde la raíz del proyecto StreamFlix)");
        }
        areaCodigo.setCaretPosition(0);
    }

    // =========================================================== utilidades de estilo
    private static JPanel panelBase(java.awt.LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return p;
    }

    private static JLabel etiqueta(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(TEXT);
        return l;
    }

    private static JLabel etiquetaCampo(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return l;
    }

    private static JButton boton(String s) {
        return new BotonRedondeado(s);
    }

    /** Botón con esquinas redondeadas y realce al pasar el ratón (sin librerías externas). */
    private static class BotonRedondeado extends JButton {
        private boolean encima;

        BotonRedondeado(String texto) {
            super(texto.toUpperCase(Locale.getDefault()));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) { encima = true; repaint(); }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) { encima = false; repaint(); }
            });
            addChangeListener(e -> repaint());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c;
            if (!isEnabled()) c = new Color(0x333333);
            else if (getModel().isPressed()) c = COBRE;
            else if (encima) c = ORANGE_CLARO;
            else c = ORANGE;
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Panel con fondo redondeado y borde suave, con un título en la parte superior. */
    private static JPanel tarjeta(JComponent contenido, String titulo) {
        JPanel p = new PanelTarjeta();
        p.setLayout(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        JLabel l = new JLabel(titulo.toUpperCase(Locale.getDefault()));
        l.setForeground(TEXT);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        p.add(l, BorderLayout.NORTH);
        contenido.setOpaque(false);
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    private static class PanelTarjeta extends JPanel {
        PanelTarjeta() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CARD);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Cabecera plana del sitio: barra gris oscura con línea inferior. */
    private static class Cabecera extends JPanel {
        Cabecera() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(PANEL);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(BORDE);
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private static void estilizarCampo(JTextField f) {
        f.setBackground(CARD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x3a3a3a)),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)));
    }

    // =========================================================== componentes propios
    /** Tabla con filtro, orden por columna y anchos calculados según el contenido. */
    private static class TablaPanel extends JPanel {
        private final DefaultTableModel modelo;
        private final TableRowSorter<DefaultTableModel> sorter;
        private final JLabel cuenta = new JLabel();
        private JTextField filtro;
        private final JTable tabla;
        private final JScrollPane scroll;
        private final java.util.Set<Integer> columnasNumericas = new java.util.HashSet<>();
        private int filaHover = -1;

        private Color fondo(int r, boolean sel) {
            if (sel) return SELECCION;
            if (r == filaHover) return CARD;
            return PANEL;
        }

        TablaPanel(String[] columnas) {
            super(new BorderLayout(0, 6));
            setBackground(BG);
            modelo = new DefaultTableModel(columnas, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };
            tabla = new JTable(modelo) {
                @Override
                public void paintComponent(Graphics g0) {
                    super.paintComponent(g0);
                    if (getRowCount() == 0) {
                        Graphics2D g = (Graphics2D) g0.create();
                        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g.setColor(MUTED);
                        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                        String msg = modelo.getRowCount() == 0
                                ? "Todavía no hay datos para mostrar."
                                : "Ningún resultado coincide con el filtro.";
                        g.drawString(msg, 14, 26);
                        g.dispose();
                    }
                }
            };
            tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
            tabla.setBackground(PANEL);
            tabla.setForeground(TEXT);
            tabla.setRowHeight(30);
            tabla.setShowVerticalLines(false);
            tabla.setShowHorizontalLines(true);
            tabla.setGridColor(new Color(0x2e2e2e));
            tabla.setIntercellSpacing(new Dimension(0, 1));
            tabla.setSelectionBackground(SELECCION);
            tabla.setSelectionForeground(Color.WHITE);
            tabla.setFillsViewportHeight(true);
            tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            tabla.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int r = tabla.rowAtPoint(e.getPoint());
                    if (r != filaHover) { filaHover = r; tabla.repaint(); }
                }
            });
            tabla.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (filaHover != -1) { filaHover = -1; tabla.repaint(); }
                }
            });

            javax.swing.table.JTableHeader encabezado = tabla.getTableHeader();
            encabezado.setBackground(CARD);
            encabezado.setForeground(MUTED);
            encabezado.setFont(new Font("SansSerif", Font.BOLD, 11));
            encabezado.setPreferredSize(new Dimension(10, 34));
            encabezado.setReorderingAllowed(false);
            encabezado.setDefaultRenderer(new RenderEncabezado());

            tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public java.awt.Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foco, int r, int c) {
                    java.awt.Component comp = super.getTableCellRendererComponent(t, v, sel, foco, r, c);
                    comp.setBackground(fondo(r, sel));
                    comp.setForeground(sel ? Color.WHITE : TEXT);
                    setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                    setHorizontalAlignment(c == 0 ? CENTER
                            : columnasNumericas.contains(c) ? RIGHT : LEFT);
                    setToolTipText(v == null ? null : v.toString());
                    return comp;
                }
            });
            for (int c = 0; c < columnas.length; c++) {
                String n = columnas[c];
                if (n.contains("%")) {
                    tabla.getColumnModel().getColumn(c).setCellRenderer(new RenderPorcentaje(ORANGE));
                } else if (n.equals("Confianza")) {
                    tabla.getColumnModel().getColumn(c).setCellRenderer(new RenderPorcentaje(AMBAR));
                } else if (n.equals("Decisión") || n.equals("Favorito")
                        || n.equals("Terminó") || n.equals("Le gustó")) {
                    tabla.getColumnModel().getColumn(c).setCellRenderer(new RenderBadge());
                }
            }

            sorter = new TableRowSorter<>(modelo);
            java.util.Comparator<Object> cmp = (a, b) -> {
                String x = String.valueOf(a).replace("%", "").trim();
                String y = String.valueOf(b).replace("%", "").trim();
                try {
                    return Double.compare(Double.parseDouble(x), Double.parseDouble(y));
                } catch (NumberFormatException ex) {
                    return x.compareToIgnoreCase(y);
                }
            };
            for (int c = 0; c < columnas.length; c++) sorter.setComparator(c, cmp);
            tabla.setRowSorter(sorter);
            sorter.addRowSorterListener(e -> {
                tabla.getTableHeader().repaint();
                ajustarColumnas();
            });

            filtro = new JTextField(18);
            estilizarCampo(filtro);
            filtro.setToolTipText("Escribe para filtrar en cualquier columna");
            filtro.getDocument().addDocumentListener(new DocumentListener() {
                void aplicar() {
                    String q = filtro.getText().trim();
                    sorter.setRowFilter(q.isEmpty() ? null
                            : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
                    actualizarCuenta();
                }
                public void insertUpdate(DocumentEvent e) { aplicar(); }
                public void removeUpdate(DocumentEvent e) { aplicar(); }
                public void changedUpdate(DocumentEvent e) { aplicar(); }
            });

            cuenta.setForeground(MUTED);
            cuenta.setFont(new Font("SansSerif", Font.PLAIN, 11));
            JPanel barra = new JPanel(new BorderLayout(6, 0));
            barra.setOpaque(false);
            JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            busqueda.setOpaque(false);
            JLabel lFiltro = new JLabel("Buscar:");
            lFiltro.setForeground(MUTED);
            lFiltro.setFont(new Font("SansSerif", Font.PLAIN, 12));
            busqueda.add(lFiltro);
            busqueda.add(filtro);
            barra.add(cuenta, BorderLayout.WEST);
            barra.add(busqueda, BorderLayout.EAST);

            scroll = new JScrollPane(tabla);
            scroll.getViewport().setBackground(PANEL);
            scroll.setBorder(BorderFactory.createLineBorder(BORDE));
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    ajustarColumnas();
                }
            });

            add(barra, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            actualizarCuenta();
        }

        /** Aplica un texto en el buscador de la tabla (lo usa la barra de la cabecera). */
        void filtrar(String q) {
            if (filtro != null) filtro.setText(q == null ? "" : q);
        }

        void setFilas(List<Object[]> filas) {
            modelo.setRowCount(0);
            for (Object[] f : filas) modelo.addRow(f);
            actualizarCuenta();
            ajustarColumnas();
            SwingUtilities.invokeLater(this::ajustarColumnas);
        }

        JTable getTabla() {
            return tabla;
        }

        /** Calcula el ancho de cada columna con el encabezado (incluida la flecha de orden)
         *  y el contenido. Si no cabe, aplica los anchos reales y activa el scroll horizontal;
         *  si cabe, reparte el espacio sobrante para llenar el panel sin cortar textos. */
        private void ajustarColumnas() {
            javax.swing.table.TableColumnModel cm = tabla.getColumnModel();
            int columnas = cm.getColumnCount();
            if (columnas == 0) return;

            FontMetrics fmCabecera = tabla.getTableHeader().getFontMetrics(tabla.getTableHeader().getFont());
            FontMetrics fmCelda = tabla.getFontMetrics(tabla.getFont());
            int filas = Math.min(modelo.getRowCount(), 300);
            int disponible = scroll.getViewport().getWidth();
            if (disponible <= 0) disponible = Math.max(getWidth() - 40, 400);

            int[] anchos = new int[columnas];
            int total = 0;
            columnasNumericas.clear();
            for (int col = 0; col < columnas; col++) {
                String nombre = String.valueOf(tabla.getColumnName(col));
                int ancho = fmCabecera.stringWidth(nombre + "  ▼") + 30;
                ancho = Math.max(ancho, 72);
                boolean numerica = filas > 0;
                for (int r = 0; r < filas; r++) {
                    Object v = modelo.getValueAt(r, col);
                    if (v == null) continue;
                    String s = v.toString();
                    if (!s.matches("-?\\d+([.,]\\d+)?\\s*%?")) numerica = false;
                    ancho = Math.max(ancho, fmCelda.stringWidth(s) + 30);
                }
                if (numerica) columnasNumericas.add(col);
                anchos[col] = Math.min(ancho, 420);
                total += anchos[col];
            }

            boolean cabe = total <= disponible - 2;
            tabla.setAutoResizeMode(cabe ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
            for (int col = 0; col < columnas; col++) {
                javax.swing.table.TableColumn tc = cm.getColumn(col);
                tc.setMinWidth(48);
                tc.setPreferredWidth(anchos[col]);
                // En AUTO_RESIZE_OFF el ancho preferido NO se aplica solo: hay que fijarlo.
                if (!cabe) tc.setWidth(anchos[col]);
            }
            if (cabe) tabla.doLayout();
            tabla.getTableHeader().repaint();
        }

        private void actualizarCuenta() {
            int visibles = sorter.getViewRowCount();
            int total = modelo.getRowCount();
            String texto = visibles == total
                    ? total + (total == 1 ? " fila" : " filas")
                    : visibles + " de " + total + " filas";
            cuenta.setText(texto);
        }

        /** Encabezado oscuro, centrado, con una flecha dibujada según el orden activo. */
        private static class RenderEncabezado extends DefaultTableCellRenderer {
            RenderEncabezado() {
                setOpaque(true);
                setHorizontalAlignment(CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, BORDE),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            }

            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foco, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foco, r, c);
                setBackground(CARD);
                setForeground(MUTED);
                setFont(new Font("SansSerif", Font.BOLD, 11));
                String base = v == null ? "" : v.toString();
                String texto = base.toUpperCase(Locale.getDefault());
                if (t.getRowSorter() != null) {
                    for (javax.swing.RowSorter.SortKey k : t.getRowSorter().getSortKeys()) {
                        if (k.getColumn() == t.convertColumnIndexToModel(c)) {
                            texto += k.getSortOrder() == javax.swing.SortOrder.ASCENDING ? "  ▲" : "  ▼";
                        }
                    }
                }
                setText(texto);
                setToolTipText(AYUDA_COLUMNAS.getOrDefault(base, texto));
                return this;
            }
        }

        /** Celda de porcentaje con barra de progreso dibujada debajo del número. */
        private class RenderPorcentaje extends DefaultTableCellRenderer {
            private final Color barra;
            private double frac;

            RenderPorcentaje(Color barra) {
                this.barra = barra;
                setOpaque(false);
                setHorizontalAlignment(CENTER);
            }

            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foco, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foco, r, c);
                setOpaque(false);
                setBackground(fondo(r, sel));
                setForeground(sel ? Color.WHITE : TEXT);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 4, 10));
                frac = 0;
                try {
                    frac = Double.parseDouble(String.valueOf(v).replace("%", "").trim()) / 100.0;
                } catch (NumberFormatException ignorada) { /* celda sin número: barra vacía */ }
                frac = Math.max(0, Math.min(1, frac));
                setToolTipText(v == null ? null : v.toString());
                return this;
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                int pad = 10, y = getHeight() - 7, w = Math.max(0, getWidth() - pad * 2);
                g.setColor(new Color(0x333333));
                g.fillRoundRect(pad, y, w, 4, 4, 4);
                g.setColor(barra);
                g.fillRoundRect(pad, y, (int) Math.max(4, w * frac), 4, 4, 4);
                g.dispose();
                super.paintComponent(g0);
            }
        }

        /** Celda Sí/No (o 1/0) como pastilla verde/rojo centrada. */
        private class RenderBadge extends DefaultTableCellRenderer {
            private Color pastilla = new Color(0x333333);

            RenderBadge() {
                setOpaque(false);
                setHorizontalAlignment(CENTER);
            }

            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foco, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foco, r, c);
                setOpaque(false);
                setBackground(fondo(r, sel));
                String s = String.valueOf(v);
                boolean si = s.equalsIgnoreCase("Sí") || s.equals("1");
                boolean no = s.equalsIgnoreCase("No") || s.equals("0");
                Color tinta;
                if (si) { pastilla = new Color(0x4a2600); tinta = ORANGE_CLARO; }
                else if (no) { pastilla = new Color(0x202020); tinta = MUTED; }
                else { pastilla = new Color(0x333333); tinta = MUTED; }
                setForeground(sel ? Color.WHITE : tinta);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                setToolTipText(s);
                return this;
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                int w = getFontMetrics(getFont()).stringWidth(getText()) + 22;
                int h = 20;
                g.setColor(pastilla);
                g.fillRoundRect((getWidth() - w) / 2, (getHeight() - h) / 2, w, h, h, h);
                g.dispose();
                super.paintComponent(g0);
            }
        }
    }

    /** Gráfico de barras horizontales (dibujado con Graphics2D, sin librerías). */
    private static class GraficoBarras extends JPanel {
        private Map<String, Double> datos;
        private String titulo = "", sufijo = "";
        private double progreso = 1;
        private javax.swing.Timer anim;

        GraficoBarras() { setBackground(PANEL); }

        void mostrar(Map<String, Double> d, String t, String suf) {
            datos = d; titulo = t; sufijo = suf;
            if (anim != null) anim.stop();
            if (d == null || d.isEmpty()) {
                progreso = 1;
                repaint();
                return;
            }
            progreso = 0;
            anim = new javax.swing.Timer(16, null);
            anim.addActionListener(e -> {
                progreso = Math.min(1, progreso + 0.045);
                if (progreso >= 1) anim.stop();
                repaint();
            });
            anim.start();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (datos == null || datos.isEmpty()) {
                g.setColor(MUTED);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.drawString("(esta consulta no tiene gráfico)", 12, 22);
                g.dispose();
                return;
            }
            g.setColor(AMBAR);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(titulo, 12, 20);
            int top = 32, izq = 160, der = 86;
            int areaW = Math.max(50, w - izq - der);
            g.setColor(new Color(0x2e2e2e));
            for (int i = 1; i <= 4; i++) {
                int x = izq + areaW * i / 4;
                g.drawLine(x, top - 4, x, h - 8);
            }
            double mx = 0;
            for (double v : datos.values()) mx = Math.max(mx, v);
            if (mx == 0) mx = 1;
            double alto = Math.max(14, Math.min(26, (h - top - 8.0) / datos.size()));
            double f = progreso * progreso * (3 - 2 * progreso);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int i = 0;
            for (Map.Entry<String, Double> e : datos.entrySet()) {
                int y = (int) (top + i * alto);
                int ancho = (int) (areaW * (e.getValue() / mx) * f);
                String k = e.getKey().length() > 24 ? e.getKey().substring(0, 23) + "…" : e.getKey();
                g.setColor(TEXT);
                g.drawString(k, izq - 10 - g.getFontMetrics().stringWidth(k), (int) (y + alto / 2 + 4));
                int bh = Math.max(6, (int) alto - 8);
                int by = y + ((int) alto - bh) / 2;
                g.setColor(new Color(0x2e2e2e));
                g.fillRoundRect(izq, by, areaW, bh, bh, bh);
                g.setColor(PALETA_GRAFICO[i % PALETA_GRAFICO.length]);
                g.fillRoundRect(izq, by, Math.max(bh, ancho), bh, bh, bh);
                g.setColor(TEXT);
                String v = sufijo.isEmpty() && e.getValue() == Math.rint(e.getValue())
                        ? String.valueOf(e.getValue().intValue())
                        : String.format(Locale.US, "%.1f", e.getValue()) + sufijo;
                g.drawString(v, izq + Math.max(bh, ancho) + 8, (int) (y + alto / 2 + 4));
                i++;
            }
            g.dispose();
        }
    }

    // =========================================================== componentes de estilo
    /** Barras de scroll del propio tema: pista oscura y pulgar redondeado que se enciende al tocarlo. */
    public static class ScrollEstilo extends javax.swing.plaf.basic.BasicScrollBarUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent c) {
            return new ScrollEstilo();
        }

        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(0x333333);
            thumbDarkShadowColor = PANEL;
            thumbHighlightColor = PANEL;
            thumbLightShadowColor = PANEL;
            trackColor = PANEL;
            trackHighlightColor = PANEL;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return botonInvisible(); }

        @Override
        protected JButton createIncreaseButton(int orientation) { return botonInvisible(); }

        private static JButton botonInvisible() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            b.setOpaque(false);
            b.setFocusable(false);
            b.setBorder(null);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, java.awt.Rectangle r) {
            g.setColor(PANEL);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, java.awt.Rectangle r) {
            if (r.isEmpty() || !c.isVisible()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? ORANGE
                    : isThumbRollover() ? ORANGE_CLARO : new Color(0x333333));
            if (scrollbar.getOrientation() == javax.swing.JScrollBar.VERTICAL) {
                g2.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 6, 6);
            } else {
                g2.fillRoundRect(r.x + 2, r.y + 3, r.width - 4, r.height - 6, 6, 6);
            }
            g2.dispose();
        }

        @Override
        protected Dimension getMinimumThumbSize() { return new Dimension(24, 24); }
    }

    /** Pestañas planas: tarjeta redondeada y subrayado naranja en la activa. */
    public static class TabsEstilo extends javax.swing.plaf.basic.BasicTabbedPaneUI {
        private int hover = -1;

        public static javax.swing.plaf.ComponentUI createUI(JComponent c) {
            return new TabsEstilo();
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(10, 18, 12, 18);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(0, 10, 0, 10);
            contentBorderInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            tabPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int i = tabForCoordinate(tabPane, e.getX(), e.getY());
                    if (i != hover) { hover = i; tabPane.repaint(); }
                }
            });
            tabPane.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = -1;
                    tabPane.repaint();
                }
            });
        }

        @Override
        protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return fontHeight + 18;
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(tabPane.getBackground());
            g2.fillRect(x, y, w, h);
            if (tabIndex == hover && !isSelected) {
                g2.setColor(CARD);
                g2.fillRect(x, y, w, h);
            }
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                int x, int y, int w, int h, boolean isSelected) {
            if (isSelected) {
                g.setColor(ORANGE);
                g.fillRect(x, y + h - 3, w, 3);
            }
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) { }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, java.awt.Rectangle[] rects,
                int tabIndex, java.awt.Rectangle iconRect, java.awt.Rectangle textRect, boolean isSelected) { }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                int tabIndex, String title, java.awt.Rectangle textRect, boolean isSelected) {
            g.setFont(isSelected ? font.deriveFont(Font.BOLD) : font);
            g.setColor(isSelected ? Color.WHITE : (tabIndex == hover ? TEXT : MUTED));
            g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        }
    }

    /** Tarjeta de estadística con barra de acento a la izquierda. */
    private static class TarjetaStat extends JPanel {
        private final Color acento;

        TarjetaStat(Color acento) {
            this.acento = acento;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(CARD);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            // Miniatura 16:9 con degradado y botón de reproducción, como las tarjetas del sitio.
            int th = Math.min(96, Math.max(0, getHeight() - 70));
            if (th > 12) {
                java.awt.Shape recorte = g.getClip();
                g.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g.setPaint(new java.awt.GradientPaint(0, 0, new Color(0x3a2a12),
                        getWidth(), th, new Color(0x1a1a1a)));
                g.fillRect(0, 0, getWidth(), th);
                int cx = getWidth() / 2, cy = th / 2, r = 17;
                g.setColor(ORANGE);
                g.fillOval(cx - r, cy - r, r * 2, r * 2);
                g.setColor(new Color(0x1b1b1b));
                g.fillPolygon(new int[]{cx - 5, cx - 5, cx + 8},
                        new int[]{cy - 7, cy + 7, cy}, 3);
                g.setClip(recorte);
            }
            g.setColor(acento);
            g.fillRect(0, 0, getWidth(), 3);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Triángulo de "play" sobre pastilla blanca para el logo. */
    private static class IconoPlay implements javax.swing.Icon {
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
        @Override
        public void paintIcon(java.awt.Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, 22, 22, 7, 7);
            g.setColor(ORANGE);
            g.fillPolygon(new int[]{x + 8, x + 8, x + 16}, new int[]{y + 6, y + 16, y + 11}, 3);
            g.dispose();
        }
    }

    /** Botón cuadrado naranja con icono, como la lupa del buscador del sitio. */
    private static class BotonIcono extends JButton {
        BotonIcono(javax.swing.Icon icono) {
            super(icono);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(BorderFactory.createEmptyBorder(8, 11, 8, 11));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getModel().isPressed() || getModel().isRollover() ? ORANGE_CLARO : ORANGE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 3, 3);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Lupa blanca para el botón de búsqueda. */
    private static class IconoBuscar implements javax.swing.Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(java.awt.Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawOval(x + 1, y + 1, 9, 9);
            g.drawLine(x + 10, y + 10, x + 13, y + 13);
            g.dispose();
        }
    }

    /** Casilla de verificación del tema: caja oscura que se rellena de naranja al marcarse. */
    private static class IconoCasilla implements javax.swing.Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(java.awt.Component c, Graphics g0, int x, int y) {
            boolean sel = c instanceof javax.swing.AbstractButton
                    && ((javax.swing.AbstractButton) c).isSelected();
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(sel ? ORANGE : new Color(0x333333));
            g.fillRoundRect(x, y, 16, 16, 5, 5);
            g.setColor(sel ? ORANGE_CLARO : BORDE);
            g.drawRoundRect(x, y, 15, 15, 5, 5);
            if (sel) {
                g.setColor(Color.WHITE);
                g.setStroke(new java.awt.BasicStroke(2f));
                g.drawPolyline(new int[]{x + 4, x + 7, x + 12}, new int[]{y + 8, y + 11, y + 5}, 3);
            }
            g.dispose();
        }
    }

    /** Punto de color para el estado de la barra inferior. */
    private static class IconoPunto implements javax.swing.Icon {
        private final Color color;

        IconoPunto(Color color) { this.color = color; }

        @Override public int getIconWidth() { return 9; }
        @Override public int getIconHeight() { return 9; }
        @Override
        public void paintIcon(java.awt.Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillOval(x, y, 9, 9);
            g.dispose();
        }
    }

    // =========================================================== main
    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        String carpeta = "datos";
        for (String a : args) if (!a.startsWith("--")) { carpeta = a; break; }
        final String carpetaFinal = carpeta;
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignorada) { /* se usa el aspecto por defecto */ }
            // Intro tipo Pornhub con sonido: al terminar muestra la ventana principal.
            VentanaPrincipal ventana = new VentanaPrincipal(carpetaFinal);
            try {
                new IntroSplash(ventana).mostrar();
            } catch (RuntimeException sinIntro) {
                ventana.setVisible(true); // sin animación, pero la app siempre abre
            }
        });
    }
}
