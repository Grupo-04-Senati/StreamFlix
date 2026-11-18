package streamflix.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Rectangle;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.Timer;

/**
 * Intro de entrada con el guion de la identidad de Pornhub (animación 2D plana), adaptada a la
 * marca del proyecto:
 *
 * <pre>
 *   1. "REC" naranja con punto que parpadea sobre negro.
 *   2. El "REC" se voltea y aparece un portátil oscuro.
 *   3. Un círculo naranja sube por su pantalla y un cursor se le superpone.
 *   4. El portátil se voltea y el círculo se convierte en botón de play, que se invierte de color.
 *   5. Un cursor pequeño pulsa el botón.
 *   6. Se forma el logo: "stream" en blanco + "flix" en el bloque naranja, como la cabecera.
 *   7. Debajo aparece una palabra letra a letra y el fondo funde al color exacto de la ventana.
 * </pre>
 *
 * Formas planas, sin degradados ni resplandores: así es la identidad original. Todo se dibuja a mano
 * con Java2D (sin imágenes) y suena {@code sonidos/...theme.wav}.
 */
final class IntroSplash {

    private static final Color INTRO = Color.BLACK;
    private static final Color ORANGE = new Color(0xe68200);
    private static final Color GRIS = new Color(0x9a9a9a);
    /** Mismo fondo y mismo negro del bloque que la ventana principal, para que el cambio no se note. */
    private static final Color FONDO_APP = new Color(0x1b1b1b);
    private static final Color PANTALLA = new Color(0x0c0c0c);
    private static final Color CARCASA = new Color(0x232323);
    private static final Color CARCASA_BORDE = new Color(0x3d3d3d);
    private static final Color BASE_PC = new Color(0x2c2c2c);

    /** Lienzo de diseño; luego se escala al tamaño real de la ventana. */
    private static final int ANCHO = 860;
    private static final int ALTO = 480;

    // --- tiempos de la animación (ms) ---
    private static final double REC_SALE = 820;
    private static final double REC_FIN = 930;
    private static final double PORTATIL_INI = 900, PORTATIL_FIN = 1620;
    private static final double PORTATIL_SALE = 2950, PORTATIL_SALE_FIN = 3300;
    private static final double CIRCULO_INI = 1700, CIRCULO_FIN = 2320;
    private static final double CURSOR_INI = 2350;
    private static final double BOTON_INI = 2950, BOTON_FIN = 3420;
    private static final double PLAY_INI = 3220, PLAY_FIN = 3520;
    private static final double INVERTIR_INI = 3620, INVERTIR_FIN = 3950;
    private static final double CLIC_INI = 4000, CLIC_FIN = 4420;
    private static final double CAJA_INI = 4420, CAJA_FIN = 5050;
    private static final double LOGO_INI = 4520;
    private static final double FLIX_INI = 5050;
    private static final double PALABRA_INI = 5450;
    private static final double FONDO_INI = 5250, FONDO_FIN = 6300;
    private static final double SONIDO_FUNDIDO = 5450;
    private static final double FUNDIDO = 6300;
    private static final double DURACION = 6800;

    /** Palabra que aparece letra a letra bajo el logo (como "community" en la original). */
    private static final String PALABRA = "comunidad";

    /** Rutas donde se busca el audio (se usa la primera que exista). */
    private static final String AUDIO = "ph-theme-gta-san-andreas-theme.wav";
    private static final String[] CARPETAS = {
        "src/main/java/sonidos",
        "sonidos",
        "datos/sonidos"
    };

    private final JFrame principal;
    private JWindow ventana;
    private Lienzo lienzo;
    private Clip clip;
    private Timer timer;
    private long inicio;
    private boolean cerrado;
    private boolean principalVisible;

    IntroSplash(JFrame principal) {
        this.principal = principal;
    }

    /** Abre la intro, arranca el sonido y, al acabar, muestra la ventana principal. */
    void mostrar() {
        lienzo = new Lienzo();
        ventana = new JWindow();
        ventana.setContentPane(lienzo);
        ventana.setBackground(Color.BLACK);
        // La intro ocupa la misma caja que la ventana principal: el relevo no cambia de tamaño.
        Rectangle marco = principal != null ? principal.getBounds() : null;
        if (marco != null && marco.width > 400 && marco.height > 400) {
            ventana.setBounds(marco);
        } else {
            ventana.setSize(ANCHO, ALTO);
            ventana.setLocationRelativeTo(null);
        }
        ventana.setAlwaysOnTop(true);
        ventana.setVisible(true);

        sonar();
        inicio = System.nanoTime();
        timer = new Timer(15, e -> paso());
        timer.start();
    }

    private void paso() {
        double t = (System.nanoTime() - inicio) / 1_000_000.0;
        if (t >= SONIDO_FUNDIDO) {
            double v = 1 - (t - SONIDO_FUNDIDO) / (DURACION - SONIDO_FUNDIDO);
            aplicarVolumen(Math.max(0, v));
        }
        if (t >= FUNDIDO) {
            if (!principalVisible && principal != null) {
                principalVisible = true;
                principal.setVisible(true); // ya detrás, con el mismo fondo: el fundido no salta
            }
            opacidad((float) Math.max(0, 1 - (t - FUNDIDO) / (DURACION - FUNDIDO)));
        }
        if (t >= DURACION) {
            cerrar();
            return;
        }
        lienzo.repaint();
    }

    private void cerrar() {
        if (cerrado) return;
        cerrado = true;
        if (timer != null) timer.stop();
        if (clip != null) {
            clip.stop();
            clip.close();
        }
        if (ventana != null) ventana.dispose();
        if (principal != null) {
            principal.setVisible(true);
            principal.toFront();
            principal.requestFocus();
        }
    }

    private void opacidad(float v) {
        try {
            ventana.setOpacity(Math.max(0f, Math.min(1f, v)));
        } catch (Exception sinTranslucidez) {
            // la plataforma no permite translucidez: la intro se cierra igual
        }
    }

    // ------------------------------------------------------------------ sonido
    private void sonar() {
        File f = archivoSonido();
        if (f == null) return;
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
            clip = AudioSystem.getClip();
            clip.open(ais);
            aplicarVolumen(1.0);
            clip.start();
        } catch (Exception e) {
            clip = null; // sin audio: la animación sigue igual
        }
    }

    private void aplicarVolumen(double v) {
        if (clip == null) return;
        try {
            FloatControl ganancia = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            double db = v <= 0.002 ? ganancia.getMinimum() : 20 * Math.log10(v);
            ganancia.setValue((float) Math.max(ganancia.getMinimum(),
                    Math.min(ganancia.getMaximum(), db)));
        } catch (Exception sinControlDeVolumen) {
            // el equipo no expone control de volumen: se deja el sonido tal cual
        }
    }

    private static File archivoSonido() {
        for (String carpeta : CARPETAS) {
            File f = new File(carpeta, AUDIO);
            if (f.isFile()) return f;
        }
        return null;
    }

    // ------------------------------------------------------------------ dibujo
    private final class Lienzo extends JPanel {

        Lienzo() {
            setBackground(Color.BLACK);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                double t = (System.nanoTime() - inicio) / 1_000_000.0;
                int w = getWidth();
                int h = getHeight();

                // El fondo acaba exactamente en el tono de la ventana principal (sin salto al cerrar).
                g.setColor(mezcla(INTRO, FONDO_APP, suave(t, FONDO_INI, FONDO_FIN)));
                g.fillRect(0, 0, w, h);
                // la viñeta se apaga con el fundido para no alterar el color final del fondo
                vineta(g, w, h, 1 - suave(t, FONDO_INI, FONDO_INI + 400));

                // La animación está diseñada en 860x480 y se centra/adapta al tamaño de la ventana.
                double k = Math.min(w / (double) ANCHO, h / (double) ALTO);
                g.translate(w / 2.0, h / 2.0);
                g.scale(k, k);
                g.translate(-ANCHO / 2.0, -ALTO / 2.0);
                pintar(g, t);
            } finally {
                g.dispose();
            }
        }

        /** Oscurece un poco las esquinas: da aspecto de pieza audiovisual, no de ventana. */
        private void vineta(Graphics2D g, int w, int h, double fuerza) {
            if (fuerza <= 0.01) return;
            float radio = (float) (Math.hypot(w, h) / 2.0);
            int alfa = (int) Math.round(85 * Math.min(1, fuerza));
            try {
                g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), radio,
                        new float[]{0.5f, 1f},
                        new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, alfa)},
                        MultipleGradientPaint.CycleMethod.NO_CYCLE));
                g.fillRect(0, 0, w, h);
            } catch (RuntimeException sinDegradado) {
                // sin degradados disponibles: la viñeta es opcional
            }
        }

        private void pintar(Graphics2D g, double t) {
            int w = ANCHO;
            int h = ALTO;
            double cx = w / 2.0;
            double cy = h / 2.0 - 10;

            // medidas del logo final (se necesitan antes para el morphing del botón)
            Font fLogo = new Font("SansSerif", Font.BOLD, 58);
            FontMetrics fmLogo = g.getFontMetrics(fLogo);
            String izq = "stream";
            String der = "flix";
            int pad = 13;
            int wIzq = fmLogo.stringWidth(izq);
            int wDer = fmLogo.stringWidth(der) + pad * 2;
            int altoLogo = fmLogo.getHeight() - 6;
            double xLogo = cx - (wIzq + wDer) / 2.0;
            double yLogo = cy - altoLogo / 2.0 - 4;
            double lineaBase = yLogo + fmLogo.getAscent() - 4;
            Rect caja = new Rect(xLogo + wIzq, yLogo, wDer, altoLogo);

            // 1) REC
            if (t < REC_FIN) pintarRec(g, cx, cy, t);

            // 2) portátil (entra y sale volteándose)
            if (t > PORTATIL_INI && t < PORTATIL_SALE_FIN) {
                double sx = t < PORTATIL_SALE
                        ? Math.max(0.002, Math.cos(Math.PI / 2 * (1 - suave(t, PORTATIL_INI, PORTATIL_FIN))))
                        : Math.max(0.002, Math.cos(Math.PI / 2 * suave(t, PORTATIL_SALE, PORTATIL_SALE_FIN)));
                double alfa = t < PORTATIL_SALE ? suave(t, PORTATIL_INI, PORTATIL_INI + 200)
                        : 1 - suave(t, PORTATIL_SALE, PORTATIL_SALE + 200);
                pintarPortatil(g, cx, cy, sx, alfa);
            }

            // 3) círculo naranja subiendo por la pantalla + cursor encima
            if (t > CIRCULO_INI && t < PORTATIL_SALE + 120) {
                double p = suave(t, CIRCULO_INI, CIRCULO_FIN);
                double r = 30 + 4 * p;
                double y = cy + 62 - (62 + r - 30) * p;
                g.setColor(ORANGE);
                g.fillOval((int) Math.round(cx - r), (int) Math.round(y - r),
                        (int) Math.round(r * 2), (int) Math.round(r * 2));
                if (t > CURSOR_INI) {
                    double entra = suave(t, CURSOR_INI, CURSOR_INI + 180);
                    double sale = suave(t, PORTATIL_SALE, PORTATIL_SALE + 200);
                    pintarCursor(g, cx + r * 0.55, y + r * 0.55, 1.0, Math.min(entra, 1 - sale));
                }
            }

            // 4) botón de play: nace del círculo, se invierte y acaba como bloque del logo
            if (t > BOTON_INI) pintarBoton(g, cx, cy, caja, t);

            // 5) logo "stream" + "flix" dentro del bloque
            if (t > LOGO_INI) pintarLogo(g, fLogo, fmLogo, izq, der, pad, xLogo, lineaBase, caja, t);

            // 6) palabra letra a letra debajo
            if (t > PALABRA_INI) pintarPalabra(g, cx, yLogo + altoLogo + 46, t);
        }

        /** "REC" de grabación: punto que parpadea y texto que se voltea hacia atrás. */
        private void pintarRec(Graphics2D g, double cx, double cy, double t) {
            double volteo = suave(t, REC_SALE, REC_FIN);
            if (volteo >= 1) return;
            double sx = Math.cos(Math.PI / 2 * volteo);
            if (sx <= 0.02) return;

            Font f = new Font("Monospaced", Font.BOLD, 32);
            FontMetrics fm = g.getFontMetrics(f);
            String texto = "REC";
            int tracking = 7;
            int anchoTexto = 0;
            for (int i = 0; i < texto.length(); i++) anchoTexto += fm.stringWidth(texto.substring(i, i + 1)) + tracking;
            anchoTexto -= tracking;
            int ancho = anchoTexto + 26; // hueco del punto

            Graphics2D gl = (Graphics2D) g.create();
            try {
                double entra = suave(t, 0, 140);
                gl.translate(cx, cy);
                gl.scale(Math.max(0.002, sx) * (0.94 + 0.06 * entra), 1);
                gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        (float) Math.min(1, entra * 2)));

                // corchetes de encuadre (coordenadas ya trasladadas al centro)
                double marcoAn = ancho / 2.0 + 22;
                double marcoAl = 30;
                gl.setColor(new Color(0x8a6a35));
                gl.setStroke(new BasicStroke(1.4f));
                int largo = 13;
                for (int sx1 = -1; sx1 <= 1; sx1 += 2) {
                    for (int sy1 = -1; sy1 <= 1; sy1 += 2) {
                        int x = (int) Math.round(sx1 * marcoAn);
                        int y = (int) Math.round(sy1 * marcoAl);
                        gl.drawLine(x, y, x - sx1 * largo, y);
                        gl.drawLine(x, y, x, y - sy1 * largo);
                    }
                }

                gl.setComposite(AlphaComposite.SrcOver);
                gl.setFont(f);
                gl.setColor(ORANGE);
                double x = -ancho / 2.0;
                double y = fm.getAscent() / 2.0 - 3;
                double parpadeo = 0.40 + 0.60 * Math.abs(Math.sin(t / 190.0));
                gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) parpadeo));
                gl.fillOval((int) Math.round(x), (int) Math.round(y - fm.getAscent() * 0.44), 12, 12);
                gl.setComposite(AlphaComposite.SrcOver);
                x += 25;
                for (int i = 0; i < texto.length(); i++) {
                    String c = texto.substring(i, i + 1);
                    gl.drawString(c, (float) x, (float) y);
                    x += fm.stringWidth(c) + tracking;
                }
            } finally {
                gl.dispose();
            }
        }

        /** Portátil oscuro de frente, con base en perspectiva (formas planas, sin reflejos). */
        private void pintarPortatil(Graphics2D g, double cx, double cy, double sx, double alfa) {
            if (sx <= 0.02 || alfa <= 0.01) return;
            int an = 320, al = 200;
            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        (float) Math.min(1f, alfa)));
                gl.translate(cx, cy);
                gl.scale(sx, 1);

                // tapa
                gl.setColor(CARCASA);
                gl.fillRoundRect(-an / 2, -al / 2, an, al, 14, 14);
                gl.setColor(CARCASA_BORDE);
                gl.setStroke(new BasicStroke(1.4f));
                gl.drawRoundRect(-an / 2, -al / 2, an, al, 14, 14);

                // pantalla
                gl.setColor(PANTALLA);
                gl.fillRoundRect(-an / 2 + 11, -al / 2 + 11, an - 22, al - 22, 8, 8);
                gl.setColor(new Color(0x2a2a2a));
                gl.setStroke(new BasicStroke(1f));
                gl.drawRoundRect(-an / 2 + 11, -al / 2 + 11, an - 22, al - 22, 8, 8);

                // base con perspectiva
                gl.setColor(BASE_PC);
                gl.fillPolygon(
                        new int[]{-an / 2 - 6, an / 2 + 6, an / 2 + 34, -an / 2 - 34},
                        new int[]{al / 2 - 2, al / 2 - 2, al / 2 + 16, al / 2 + 16}, 4);
                gl.setColor(CARCASA_BORDE);
                gl.drawPolygon(
                        new int[]{-an / 2 - 6, an / 2 + 6, an / 2 + 34, -an / 2 - 34},
                        new int[]{al / 2 - 2, al / 2 - 2, al / 2 + 16, al / 2 + 16}, 4);
                gl.setColor(new Color(0x3a3a3a));
                gl.drawLine(-an / 2 + 44, al / 2 + 7, an / 2 - 44, al / 2 + 7);
            } finally {
                gl.dispose();
            }
        }

        /** Cursor de ratón clásico (punta arriba-izquierda). */
        private void pintarCursor(Graphics2D g, double x, double y, double escala, double alfa) {
            if (alfa <= 0.01) return;
            double[] px = {0, 0, 4.6, 7.8, 10.6, 7.4, 12.6, 12.6};
            double[] py = {0, 18, 13.8, 21, 19.4, 12.6, 12.6, 12.6};
            int[] vx = new int[px.length];
            int[] vy = new int[vx.length];
            for (int i = 0; i < vx.length; i++) {
                vx[i] = (int) Math.round(x + px[i] * escala);
                vy[i] = (int) Math.round(y + py[i] * escala);
            }
            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        (float) Math.min(1, alfa)));
                gl.setColor(new Color(0x101010));
                gl.fillPolygon(vx, vy, vx.length);
                gl.setColor(Color.WHITE);
                gl.setStroke(new BasicStroke(1.6f));
                gl.drawPolygon(vx, vy, vx.length);
            } finally {
                gl.dispose();
            }
        }

        /** Círculo → botón de play → (se invierte) → bloque naranja del logo. */
        private void pintarBoton(Graphics2D g, double cx, double cy, Rect caja, double t) {
            double tam = lerp(60, 130, suave(t, BOTON_INI, BOTON_FIN));
            double radio = lerp(tam / 2, 26, suave(t, BOTON_INI, BOTON_FIN + 200));

            // morphing del botón hacia el bloque naranja de "flix"
            double pc = suave(t, CAJA_INI, CAJA_FIN);
            if (pc >= 1) {
                pintarBloque(g, caja);
                return;
            }
            double x = lerp(cx - tam / 2, caja.x, pc);
            double y = lerp(cy - tam / 2, caja.y, pc);
            double an = lerp(tam, caja.w, pc);
            double al = lerp(tam, caja.h, pc);
            double rr = lerp(radio, 12, pc);

            // pulsación del clic
            double presion = 1 - 0.08 * Math.sin(Math.PI * suave(t, CLIC_FIN - 240, CLIC_FIN - 40));
            if (t > CLIC_FIN - 240) {
                x = cx + (x - cx) * presion;
                y = cy + (y - cy) * presion;
                an *= presion;
                al *= presion;
            }

            // "se invierte de color": giro en Y con cambio de relleno
            double inv = suave(t, INVERTIR_INI, INVERTIR_FIN);
            double sx = 1;
            boolean naranja = true;
            if (inv > 0 && inv < 1) {
                sx = Math.abs(Math.cos(Math.PI * inv));
                naranja = inv < 0.5;
            } else if (inv >= 1) {
                naranja = false;
            }

            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.translate(x + an / 2, y + al / 2);
                gl.scale(Math.max(0.01, sx), 1);
                gl.translate(-(x + an / 2), -(y + al / 2));

                // al transformarse en el bloque del logo el relleno vuelve al naranja
                gl.setColor(mezcla(naranja ? ORANGE : new Color(0x101010), ORANGE, pc));
                gl.fillRoundRect((int) Math.round(x), (int) Math.round(y),
                        (int) Math.round(an), (int) Math.round(al),
                        (int) Math.round(rr), (int) Math.round(rr));
                if (!naranja && pc < 1) {
                    // el botón invertido se lee por el contorno naranja
                    Graphics2D gb = (Graphics2D) gl.create();
                    try {
                        gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                (float) (1 - pc)));
                        gb.setColor(ORANGE);
                        gb.setStroke(new BasicStroke(2f));
                        gb.drawRoundRect((int) Math.round(x), (int) Math.round(y),
                                (int) Math.round(an), (int) Math.round(al),
                                (int) Math.round(rr), (int) Math.round(rr));
                    } finally {
                        gb.dispose();
                    }
                }

                // triángulo de play, con entrada elástica y desvanecido al llegar el bloque
                double ap = suave(t, PLAY_INI, PLAY_FIN) * (1 - suave(t, CAJA_INI, CAJA_INI + 240));
                if (ap > 0.01) {
                    double lado = Math.min(an, al) * 0.42;
                    double reb = rebote(suave(t, PLAY_INI, PLAY_FIN));
                    Graphics2D gt = (Graphics2D) gl.create();
                    try {
                        gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) ap));
                        gt.setColor(naranja ? new Color(0x121212) : ORANGE);
                        gt.translate(x + an / 2, y + al / 2);
                        gt.scale(reb, reb); // entrada elástica uniforme (sin deformar el triángulo)
                        gt.fillPolygon(
                                new int[]{(int) Math.round(-lado * 0.40),
                                    (int) Math.round(lado * 0.60),
                                    (int) Math.round(-lado * 0.40)},
                                new int[]{(int) Math.round(-lado * 0.58), 0,
                                    (int) Math.round(lado * 0.58)}, 3);
                    } finally {
                        gt.dispose();
                    }
                }
            } finally {
                gl.dispose();
            }

            // cursor pequeño que pulsa el botón
            if (t > CLIC_INI && t < CLIC_FIN + 160) {
                double p = suave(t, CLIC_INI, CLIC_FIN - 240);
                double xc = cx + 160 - 112 * p;
                double yc = cy + 138 - 96 * p;
                double alfa = Math.min(suave(t, CLIC_INI, CLIC_INI + 140),
                        1 - suave(t, CLIC_FIN, CLIC_FIN + 160));
                pintarCursor(g, xc, yc, 0.95, alfa);
            }
        }

        /** Bloque naranja ya colocado en el logo. */
        private void pintarBloque(Graphics2D g, Rect caja) {
            g.setColor(ORANGE);
            g.fillRoundRect((int) Math.round(caja.x), (int) Math.round(caja.y),
                    (int) Math.round(caja.w), (int) Math.round(caja.h), 12, 12);
        }

        /** "stream" en blanco y "flix" dentro del bloque, letra a letra. */
        private void pintarLogo(Graphics2D g, Font fLogo, FontMetrics fm, String izq, String der,
                int pad, double xLogo, double lineaBase, Rect caja, double t) {
            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.setFont(fLogo);
                double x = xLogo;
                for (int i = 0; i < izq.length(); i++) {
                    String c = izq.substring(i, i + 1);
                    double p = suave(t, LOGO_INI + i * 55, LOGO_INI + i * 55 + 260);
                    if (p > 0) {
                        Graphics2D gc = (Graphics2D) gl.create();
                        try {
                            gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) p));
                            gc.setColor(Color.WHITE);
                            gc.drawString(c, (float) x, (float) (lineaBase + 10 * (1 - p)));
                        } finally {
                            gc.dispose();
                        }
                    }
                    x += fm.stringWidth(c);
                }

                double xd = caja.x + pad;
                for (int i = 0; i < der.length(); i++) {
                    String c = der.substring(i, i + 1);
                    double p = suave(t, FLIX_INI + i * 45, FLIX_INI + i * 45 + 240);
                    if (p > 0) {
                        Graphics2D gc = (Graphics2D) gl.create();
                        try {
                            gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) p));
                            gc.setColor(FONDO_APP); // mismo negro que el bloque de la cabecera
                            gc.drawString(c, (float) xd, (float) (lineaBase + 8 * (1 - p)));
                        } finally {
                            gc.dispose();
                        }
                    }
                    xd += fm.stringWidth(c);
                }
            } finally {
                gl.dispose();
            }
        }

        /** Palabra gris que aparece letra a letra con un pequeño rebote. */
        private void pintarPalabra(Graphics2D g, double cx, double y, double t) {
            Font f = new Font("SansSerif", Font.PLAIN, 20);
            FontMetrics fm = g.getFontMetrics(f);
            double ancho = 0;
            for (int i = 0; i < PALABRA.length(); i++) ancho += fm.stringWidth(PALABRA.substring(i, i + 1));
            double x = cx - ancho / 2.0;

            for (int i = 0; i < PALABRA.length(); i++) {
                String c = PALABRA.substring(i, i + 1);
                double p = suave(t, PALABRA_INI + i * 50, PALABRA_INI + i * 50 + 300);
                double wc = fm.stringWidth(c);
                if (p > 0) {
                    double escala = 0.6 + 0.4 * rebote(p);
                    Graphics2D gc = (Graphics2D) g.create();
                    try {
                        gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                (float) Math.min(1, p * 1.4)));
                        gc.translate(x + wc / 2, y);
                        gc.scale(escala, escala);
                        gc.setColor(GRIS);
                        gc.setFont(f);
                        gc.drawString(c, (float) (-wc / 2), 0f);
                    } finally {
                        gc.dispose();
                    }
                }
                x += wc;
            }
        }
    }

    /** Rectángulo simple para interpolar el botón hacia el bloque del logo. */
    private static final class Rect {
        final double x, y, w, h;

        Rect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    /** Progreso suavizado (smoothstep) entre dos instantes. */
    private static double suave(double t, double ini, double fin) {
        if (t <= ini) return 0;
        if (t >= fin) return 1;
        double p = (t - ini) / (fin - ini);
        return p * p * (3 - 2 * p);
    }

    /** Rebote elástico para la aparición de cada letra. */
    private static double rebote(double p) {
        double q = p - 1;
        return 1 + 2.4 * q * q * q + 1.4 * q * q;
    }

    private static double lerp(double a, double b, double p) {
        return a + (b - a) * p;
    }

    /** Mezcla lineal entre dos colores. */
    private static Color mezcla(Color a, Color b, double p) {
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * p);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * p);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * p);
        return new Color(r, g, bl);
    }
}
