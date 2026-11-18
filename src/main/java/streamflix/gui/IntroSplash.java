package streamflix.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
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
 * Intro de entrada con el guion de la identidad de Pornhub, adaptada a la marca del proyecto:
 *
 * <pre>
 *   1. "REC" de cámara naranja (con corchetes y punto que parpadea) sobre negro.
 *   2. Corte con destello: el "REC" se voltea y aparece un portátil plateado.
 *   3. Un círculo naranja con brillo sube por la pantalla y un cursor se le superpone.
 *   4. El portátil se voltea y el círculo se convierte en botón de play, que se invierte de color.
 *   5. Un cursor pequeño pulsa el botón (con destello y golpe de cámara).
 *   6. Se forma el logo: "stream" en blanco + "flix" en el bloque naranja, como en la cabecera.
 *   7. Debajo aparece una palabra letra a letra y el fondo funde al color exacto de la ventana.
 * </pre>
 *
 * Todo se dibuja a mano con Java2D (sin imágenes) y suena {@code sonidos/...theme.wav}.
 */
final class IntroSplash {

    private static final Color INTRO = Color.BLACK;
    private static final Color ORANGE = new Color(0xe68200);
    private static final Color ORANGE_CLARO = new Color(0xf59b1f);
    private static final Color GRIS = new Color(0xb5b5b5);
    /** Mismo fondo y mismo negro del bloque que la ventana principal, para que el cambio no se note. */
    private static final Color FONDO_APP = new Color(0x1b1b1b);
    private static final Color PANTALLA = new Color(0x0a0a0a);
    private static final Color PLATA = new Color(0xd6d6d6);
    private static final Color PLATA_MEDIO = new Color(0x9a9a9a);
    private static final Color PLATA_OSCURO = new Color(0x6e6e6e);

    /** Lienzo de diseño; luego se escala al tamaño real de la ventana. */
    private static final int ANCHO = 860;
    private static final int ALTO = 480;

    // --- tiempos de la animación (ms) ---
    private static final double REC_SALE = 880;
    private static final double REC_FIN = 1000;
    private static final double DESTELLO1_INI = 900;
    private static final double PORTATIL_INI = 960, PORTATIL_FIN = 1680;
    private static final double PORTATIL_SALE = 3050, PORTATIL_SALE_FIN = 3400;
    private static final double CIRCULO_INI = 1750, CIRCULO_FIN = 2400;
    private static final double CURSOR_INI = 2400;
    private static final double BOTON_INI = 3100, BOTON_FIN = 3550;
    private static final double PLAY_INI = 3350, PLAY_FIN = 3650;
    private static final double INVERTIR_INI = 3750, INVERTIR_FIN = 4100;
    private static final double CLIC_INI = 4150, CLIC_FIN = 4600;
    private static final double CAJA_INI = 4600, CAJA_FIN = 5250;
    private static final double LOGO_INI = 4700;
    private static final double FLIX_INI = 5250;
    private static final double PALABRA_INI = 5650;
    private static final double FONDO_INI = 5500, FONDO_FIN = 6500;
    private static final double SONIDO_FUNDIDO = 5600;
    private static final double FUNDIDO = 6500;
    private static final double DURACION = 7000;

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
                if (t > CLIC_FIN - 260 && t < CLIC_FIN + 320) {
                    // golpe de cámara al pulsar el botón
                    double fuerza = 5 * (1 - suave(t, CLIC_FIN - 260, CLIC_FIN + 320));
                    g.translate(fuerza * Math.sin((t - CLIC_FIN + 260) / 14.0), fuerza * 0.35);
                }
                pintar(g, t);
            } finally {
                g.dispose();
            }
        }

        /** Oscurece las esquinas: da aspecto de pieza audiovisual, no de ventana. */
        private void vineta(Graphics2D g, int w, int h, double fuerza) {
            if (fuerza <= 0.01) return;
            float radio = (float) (Math.hypot(w, h) / 2.0);
            int alfa = (int) Math.round(120 * Math.min(1, fuerza));
            try {
                g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), radio,
                        new float[]{0.45f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, alfa)},
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

            // 1) REC de cámara
            if (t < REC_FIN) pintarRec(g, cx, cy, t);

            // 2) portátil plateado (entra y sale volteándose)
            if (t > PORTATIL_INI && t < PORTATIL_SALE_FIN) {
                double sx = t < PORTATIL_SALE
                        ? Math.max(0.002, Math.cos(Math.PI / 2 * (1 - suave(t, PORTATIL_INI, PORTATIL_FIN))))
                        : Math.max(0.002, Math.cos(Math.PI / 2 * suave(t, PORTATIL_SALE, PORTATIL_SALE_FIN)));
                double alfa = t < PORTATIL_SALE ? suave(t, PORTATIL_INI, PORTATIL_INI + 220)
                        : 1 - suave(t, PORTATIL_SALE, PORTATIL_SALE + 220);
                pintarPortatil(g, cx, cy, sx, alfa);
            }

            // 3) círculo naranja con brillo subiendo por la pantalla + cursor encima
            if (t > CIRCULO_INI && t < PORTATIL_SALE + 120) {
                double p = suave(t, CIRCULO_INI, CIRCULO_FIN);
                double r = 30 + 4 * p;
                double y = cy + 62 - (62 + r - 30) * p;
                brillo(g, cx, y, r * 3.1, 0.22);
                g.setColor(ORANGE);
                g.fillOval((int) Math.round(cx - r), (int) Math.round(y - r),
                        (int) Math.round(r * 2), (int) Math.round(r * 2));
                g.setColor(ORANGE_CLARO);
                g.fillOval((int) Math.round(cx - r * 0.55), (int) Math.round(y - r * 0.75),
                        (int) Math.round(r * 0.8), (int) Math.round(r * 0.8));
                if (t > CURSOR_INI) {
                    double entra = suave(t, CURSOR_INI, CURSOR_INI + 180);
                    double sale = suave(t, PORTATIL_SALE, PORTATIL_SALE + 200);
                    pintarCursor(g, cx + r * 0.55, y + r * 0.55, 1.0, Math.min(entra, 1 - sale));
                }
            }

            // 4) botón de play: nace del círculo, se invierte de color y acaba como bloque del logo
            if (t > BOTON_INI) pintarBoton(g, cx, cy, caja, t);

            // 5) logo "stream" + "flix" dentro del bloque
            if (t > LOGO_INI) pintarLogo(g, fLogo, fmLogo, izq, der, pad, xLogo, lineaBase, caja, t);

            // 6) palabra letra a letra debajo
            if (t > PALABRA_INI) pintarPalabra(g, cx, yLogo + altoLogo + 46, t);

            // 7) destellos de corte
            destello(g, w, h, t, DESTELLO1_INI, DESTELLO1_INI + 90, 0.25);
        }

        /** Destello blanco breve (corte / impacto). */
        private void destello(Graphics2D g, int w, int h, double t, double ini, double fin, double fuerza) {
            if (t <= ini || t >= fin) return;
            double p = (t - ini) / (fin - ini);
            float a = (float) (fuerza * (1 - p) * (1 - p));
            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, a)));
                gl.setColor(Color.WHITE);
                gl.fillRect(0, 0, w, h);
            } finally {
                gl.dispose();
            }
        }

        /** Resplandor naranja suave y sin escalones (degradado radial). */
        private void brillo(Graphics2D g, double x, double y, double radio, double fuerza) {
            if (fuerza <= 0.004 || radio <= 1) return;
            int alfa = (int) Math.round(255 * Math.min(1, fuerza));
            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.setPaint(new RadialGradientPaint(new Point2D.Float((float) x, (float) y), (float) radio,
                        new float[]{0f, 1f},
                        new Color[]{new Color(230, 130, 0, alfa), new Color(230, 130, 0, 0)},
                        MultipleGradientPaint.CycleMethod.NO_CYCLE));
                gl.fillOval((int) Math.round(x - radio), (int) Math.round(y - radio),
                        (int) Math.round(radio * 2), (int) Math.round(radio * 2));
            } catch (RuntimeException sinDegradado) {
                // sin degradados disponibles: el resplandor es opcional
            } finally {
                gl.dispose();
            }
        }

        /** "REC" de cámara: corchetes, punto que parpadea y texto que se voltea hacia atrás. */
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
                gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, entra * 2)));

                // corchetes de encuadre (coordenadas ya trasladadas al centro)
                double marcoAn = ancho / 2.0 + 22;
                double marcoAl = 30;
                gl.setColor(new Color(230, 130, 0, 110));
                gl.setStroke(new BasicStroke(1.6f));
                int largo = 14;
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

        /** Portátil plateado con perspectiva (base más ancha que el bisel) y reflejo de pantalla. */
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
                gl.setPaint(new GradientPaint(0, -al / 2, PLATA, 0, al / 2, PLATA_OSCURO));
                gl.fillRoundRect(-an / 2, -al / 2, an, al, 14, 14);
                gl.setColor(new Color(255, 255, 255, 70));
                gl.setStroke(new BasicStroke(1f));
                gl.drawRoundRect(-an / 2, -al / 2, an, al, 14, 14);

                // pantalla
                gl.setColor(PANTALLA);
                gl.fillRoundRect(-an / 2 + 10, -al / 2 + 10, an - 20, al - 20, 8, 8);

                // reflejo diagonal en la pantalla
                Graphics2D gr = (Graphics2D) gl.create();
                try {
                    gr.clip(new java.awt.geom.RoundRectangle2D.Double(
                            -an / 2 + 10, -al / 2 + 10, an - 20, al - 20, 8, 8));
                    gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f));
                    gr.setColor(Color.WHITE);
                    gr.fillPolygon(new int[]{-an / 2 + 10, -an / 2 + 150, -an / 2 + 20, -an / 2 + 10},
                            new int[]{-al / 2 + 10, -al / 2 + 10, al / 2 - 10, al / 2 - 10}, 4);
                } finally {
                    gr.dispose();
                }

                // base con perspectiva
                gl.setPaint(new GradientPaint(0, al / 2 - 2, new Color(0xdadada), 0, al / 2 + 20, PLATA_MEDIO));
                gl.fillPolygon(
                        new int[]{-an / 2 - 6, an / 2 + 6, an / 2 + 34, -an / 2 - 34},
                        new int[]{al / 2 - 2, al / 2 - 2, al / 2 + 16, al / 2 + 16}, 4);
                gl.setColor(new Color(0x8a8a8a));
                gl.setStroke(new BasicStroke(1.4f));
                gl.drawLine(-an / 2 + 40, al / 2 + 7, an / 2 - 40, al / 2 + 7);
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
                pintarBloque(g, caja, t);
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

            if (pc <= 0) brillo(g, x + an / 2, y + al / 2, an * 1.15, 0.18);

            Graphics2D gl = (Graphics2D) g.create();
            try {
                gl.translate(x + an / 2, y + al / 2);
                gl.scale(Math.max(0.01, sx), 1);
                gl.translate(-(x + an / 2), -(y + al / 2));

                // al transformarse en el bloque del logo el relleno vuelve al naranja
                gl.setColor(mezcla(naranja ? ORANGE : new Color(0x111111), ORANGE, pc));
                gl.fillRoundRect((int) Math.round(x), (int) Math.round(y),
                        (int) Math.round(an), (int) Math.round(al),
                        (int) Math.round(rr), (int) Math.round(rr));
                if (naranja) {
                    gl.setColor(ORANGE_CLARO);
                    gl.setStroke(new BasicStroke(1.2f));
                    gl.drawRoundRect((int) Math.round(x), (int) Math.round(y),
                            (int) Math.round(an), (int) Math.round(al),
                            (int) Math.round(rr), (int) Math.round(rr));
                } else if (pc < 1) {
                    Graphics2D gb = (Graphics2D) gl.create();
                    try {
                        gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - pc)));
                        gb.setColor(ORANGE);
                        gb.setStroke(new BasicStroke(2.2f));
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

            // onda del clic
            double onda = suave(t, CLIC_FIN - 220, CLIC_FIN + 220);
            if (onda > 0 && onda < 1) {
                Graphics2D gr = (Graphics2D) g.create();
                try {
                    gr.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                            (float) (0.5 * (1 - onda))));
                    gr.setColor(ORANGE_CLARO);
                    gr.setStroke(new BasicStroke(2.6f));
                    int r = (int) Math.round(72 + 80 * onda);
                    gr.drawOval((int) Math.round(cx - r), (int) Math.round(cy - r), r * 2, r * 2);
                } finally {
                    gr.dispose();
                }
            }
        }

        /** Bloque naranja ya colocado en el logo, con un brillo que lo recorre. */
        private void pintarBloque(Graphics2D g, Rect caja, double t) {
            double calidez = 1 - suave(t, CAJA_FIN + 200, CAJA_FIN + 900);
            brillo(g, caja.x + caja.w / 2, caja.y + caja.h / 2, caja.w * 0.85, 0.14 * calidez);
            g.setColor(ORANGE);
            g.fillRoundRect((int) Math.round(caja.x), (int) Math.round(caja.y),
                    (int) Math.round(caja.w), (int) Math.round(caja.h), 12, 12);
            double brillo = suave(t, CAJA_FIN + 120, CAJA_FIN + 620);
            if (brillo > 0 && brillo < 1) {
                Graphics2D gl = (Graphics2D) g.create();
                try {
                    gl.clip(new Rectangle2D.Double(caja.x, caja.y, caja.w, caja.h));
                    float a = (float) (0.24 * (1 - Math.abs(brillo - 0.5) * 2));
                    gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, a)));
                    gl.setColor(Color.WHITE);
                    int sx = (int) Math.round(caja.x - 80 + (caja.w + 160) * brillo);
                    gl.fillPolygon(new int[]{sx, sx + 40, sx + 14, sx - 26},
                            new int[]{(int) caja.y - 4, (int) caja.y - 4,
                                (int) (caja.y + caja.h) + 4, (int) (caja.y + caja.h) + 4}, 4);
                } finally {
                    gl.dispose();
                }
            }
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
                    double p = suave(t, LOGO_INI + i * 55, LOGO_INI + i * 55 + 280);
                    if (p > 0) {
                        Graphics2D gc = (Graphics2D) gl.create();
                        try {
                            gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) p));
                            gc.setColor(Color.WHITE);
                            gc.drawString(c, (float) x, (float) (lineaBase + 12 * (1 - p)));
                        } finally {
                            gc.dispose();
                        }
                    }
                    x += fm.stringWidth(c);
                }

                double xd = caja.x + pad;
                for (int i = 0; i < der.length(); i++) {
                    String c = der.substring(i, i + 1);
                    double p = suave(t, FLIX_INI + i * 45, FLIX_INI + i * 45 + 260);
                    if (p > 0) {
                        Graphics2D gc = (Graphics2D) gl.create();
                        try {
                            gc.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) p));
                            gc.setColor(FONDO_APP); // mismo negro que el bloque de la cabecera
                            gc.drawString(c, (float) xd, (float) (lineaBase + 10 * (1 - p)));
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

        /** Palabra naranja que aparece letra a letra con un pequeño rebote. */
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
                    double escala = 0.55 + 0.45 * rebote(p);
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
