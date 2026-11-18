# StreamFlix - Sistema de recomendación de películas (Java)

Proyecto del curso *Artificial Intelligence with Machine Learning in Java* (SENATI).
Java puro: **NO usa librerías externas** (solo la librería estándar de Java).

## Cómo abrirlo en Apache NetBeans

**Opción A (recomendada): abrir como proyecto Maven**
1. Descomprime `StreamFlix.zip`.
2. NetBeans -> `File > Open Project...` -> selecciona la carpeta `StreamFlix` (se ve con icono de Maven).
3. Clic derecho sobre el proyecto -> `Run` (o botón verde). Interactúa en la ventana *Output*.

**Opción B: crear tu propio proyecto y copiar el código**
1. `File > New Project > Java with Ant > Java Application`, desmarca "Create Main Class".
2. Copia la carpeta `streamflix` (de `src/main/java/`) dentro de `src` de tu proyecto.
3. Copia la carpeta `datos` a la **raíz** del proyecto (junto a `src`, NO dentro).
4. Project Properties > Run > Main Class: `streamflix.Main`.

Requisitos: JDK 11 o superior (funciona con 17 y 21).

## Interfaz gráfica (Swing)
Muestra cargar, recorrer y consultar los datos usando las clases reales del proyecto,
y agrega el flujo del **administrador**: patrón del usuario con el árbol de decisión,
tabla de recomendaciones con porcentajes y publicación.
- En NetBeans: clic derecho sobre `streamflix/gui/VentanaPrincipal.java` -> `Run File`
  (o ejecuta `Main` con el argumento `--gui`: Project Properties > Run > Arguments).
- Desde consola (en la raíz del proyecto): `javac -encoding UTF-8 -d out $(find src -name "*.java")` y luego `java -cp out streamflix.Main --gui`.
- **Animación de entrada** (~6,8 s), con el mismo guion de la identidad de Pornhub (2D plano, sin brillos): `REC` de cámara naranja (con corchetes)
  punto parpadeante → se voltea en un portátil oscuro → un círculo naranja sube por la pantalla con un cursor
  encima → el portátil se voltea y el círculo se convierte en botón de play que se invierte de color →
  un cursor lo pulsa → se forma el logo `stream` + `flix` en bloque naranja → aparece una palabra letra a letra.
  Suena `sonidos/ph-theme-gta-san-andreas-theme.wav`. Java no reproduce MP3 con `javax.sound.sampled`,
  por eso el sonido se usa en WAV (8 s, mono 44.1 kHz, recortado del MP3 original que queda como fuente).
  Si el archivo no está, la intro se ve igual pero en silencio.
- Pestañas:
  1. Carga (`CargadorDatos`)
  2. Recorrer datos
  3. Consultas (`ConsultaDatos`)
  4. **Datos de prueba**: agrega usuarios, películas/series e interacciones; cada fila se guarda
     en los CSV y se vuelve a leer para verificar que el programa la interpreta bien.
  5. **Administrador**: elige un usuario, analiza el patrón del árbol de decisión, revisa la
     tabla de recomendaciones con porcentajes y publica.
  6. Código Java
- Importante: ejecútala desde la raíz del proyecto para que encuentre `datos/` (o elige la carpeta con "Examinar").

## Flujo del administrador (pestaña 5)
1. En la pestaña 4 agrega tus datos de prueba (o usa los 40 usuarios y 40 títulos del dataset).
   - **Usuarios**: ID, nombre, país, edad y género.
   - **Películas / Series**: ID, título, tipo, género, año, duración, idioma, director, país y valoración.
   - **Interacciones**: usuario, título, minutos vistos, terminó de verla, valoración, favorito y `le_gusto`.
   Cada guardado muestra el objeto releído desde el CSV (por ejemplo `U041 | ... | Perú | 30 años | F`).
2. En la pestaña 5 elige el usuario. El panel izquierdo muestra el **patrón**:
   género preferido, porcentaje de "me gusta" de ese género, tiempo visto promedio,
   director/idioma preferido, usuarios similares y tendencias.
3. La tabla **Recomendaciones (árbol)** muestra las películas que el árbol propone, con:
   `% modelo` (probabilidad de que le guste, 0-100%), `Patrón (árbol)` (Alta Prioridad,
   Por Contenido o Exploración), `Confianza` de la regla y la `Decisión`.
   Al seleccionar una fila se ve la **ruta completa del árbol** con los valores reales
   (por ejemplo, "tiempo visto del género 80.1% → SÍ").
4. **Publicar recomendaciones** escribe `datos/publicaciones.csv`, muestra el diálogo de
   publicación y deja la tabla **Publicadas** con lo que verá el usuario.
5. La pestaña **Le gustaron** lista el historial con `le_gusto = 1`, para verificar el patrón.
- El árbol de reglas está en `ml/ArbolReglas.java` y sigue el diseño de la diapositiva:
  género → tiempo visto (> 70%) → valoración de usuarios similares (>= 4★) → director/idioma;
  y en la rama negativa: tendencias → edad/país.
- El `% modelo` sale del híbrido (filtrado colaborativo + contenido + árbol CART); el árbol de
  reglas aporta el patrón y la categoría. "Mostrar también las NO recomendadas" permite
  revisar por qué se descartó cada título.

## Importante
La carpeta `datos/` debe estar en la raíz del proyecto. Si no, el programa avisa con un mensaje claro.
También puedes indicar otra ruta como argumento: `Main C:\ruta\datos`.
Para ver todo de una vez sin menú: argumento `--todo`.

## Estructura

| Paquete / clase | Qué hace | Punto del trabajo |
|---|---|---|
| `modelo` (Usuario, Contenido, Historial, Recomendacion, BaseDatos) | Clases del modelo de datos | 6 / 7 |
| `datos.CargadorDatos` | CARGA los CSV y crea los objetos | 7 |
| `consultas.ConsultaDatos` | RECORRE y CONSULTA los datos | 7 |
| `ml.ArbolDecision`, `ml.ArbolModelo` | Árbol de decisión propio (Gini) | 5 |
| `ml.ArbolReglas` | Árbol de reglas de la diapositiva (Alta Prioridad / Por Contenido / Exploración) | Propuesta |
| `ml.FiltradoColaborativo` | Recomienda según usuarios similares | Propuesta |
| `ml.BasadoEnContenido` | Recomienda según características de la película | Propuesta |
| `ml.SistemaActual` | Línea base (géneros populares y ranking) | Comparación |
| `ml.Hibrido` | Combina los tres anteriores (modelo propuesto) | Propuesta |
| `ml.Evaluador` | Validación cruzada (5 bloques) y métricas | Propuesta |
| `recursividad` | Funciones recursivas y árbol de categorías | Propuesta |
| `gui.VentanaPrincipal` | Interfaz Swing: carga, recorrido, consultas, datos de prueba y administrador | 7 / Propuesta |
| `datos/publicaciones.csv` | Salida del botón "Publicar recomendaciones" (se crea al publicar) | Propuesta |
| `Main` | Menú de consola (`--gui` abre la interfaz) | - |

## Dataset (ficticio)
- `usuarios.csv`: id, nombre, pais, edad, genero
- `peliculas.csv`: id, titulo, tipo, genero, anio, duracion_min, idioma_original, director, pais, valoracion_promedio
  (incluye títulos del género «Adulto», C041-C046, C050-C052)
- `historial.csv`: usuario_id, contenido_id, minutos_vistos, termino, valoracion, favorito, **le_gusto** (variable objetivo)

Las tres primeras filas del historial reproducen el ejemplo de la diapositiva (Juan/Acción/120 min/1, Pedro/Terror/10 min/0, Ana/Comedia/90 min/1).
Son datos inventados (40 usuarios, 52 títulos, 1047 interacciones, incluido el género «Adulto»): los porcentajes de los resultados dependen de ellos.
