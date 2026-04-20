# Aplicacion de Gestion de Contactos (Mejorada)

## Objetivo
Esta version mejora la experiencia de usuario e interactividad de la interfaz Swing, incorporando nuevos componentes, eventos adicionales y una organizacion MVC mas clara.

## Mejoras implementadas

### 1) Nuevos componentes graficos
- `JTabbedPane` con dos pestanas:
  - `Contactos`: alta, edicion, eliminacion, busqueda, tabla y exportacion.
  - `Estadisticas`: tarjetas con metricas generales.
- `JTable` para visualizar contactos por columnas.
- `JProgressBar` para indicar estado de carga.
- `JPopupMenu` (menu contextual por clic derecho) sobre la tabla.

### 2) Eventos adicionales
- Eventos de teclado (atajos):
  - `Ctrl + S`: guardar (agregar o modificar segun seleccion).
  - `Ctrl + N`: limpiar formulario.
  - `Delete`: eliminar contacto seleccionado.
  - `Ctrl + E`: exportar CSV.
  - `Ctrl + F`: enfocar filtro de busqueda.
- Evento de mouse:
  - Clic derecho sobre una fila para abrir menu contextual con acciones:
    - Editar contacto
    - Eliminar contacto
    - Alternar favorito
    - Exportar visibles
- Eventos de documento (`DocumentListener`) en campo de busqueda para filtrar en tiempo real.

### 3) Funcionalidad avanzada
- `JTable` con:
  - Ordenamiento por columnas (`TableRowSorter`).
  - Filtrado por nombre, telefono o email (`RowFilter`).
- Exportacion a CSV de contactos visibles (respeta el filtro actual).
- Barra de progreso durante carga inicial de contactos (`SwingWorker`).

### 4) Aplicacion de MVC
Se reorganizo la logica por capas:
- Modelo:
  - `src/modelo/persona.java`: entidad de contacto.
  - `src/modelo/personaDAO.java`: persistencia en archivo y exportacion CSV.
- Vista:
  - `src/vista/ventana.java`: componentes Swing y distribucion visual.
- Controlador:
  - `src/controlador/logica_ventana.java`: validaciones, eventos, reglas de negocio, sincronizacion vista-modelo.

## Justificacion de diseno de interfaz
- Separar la app en pestanas reduce carga visual y facilita navegacion.
- Tabla en lugar de lista mejora lectura de multiples campos y permite ordenar/filtrar.
- Atajos de teclado aceleran tareas repetitivas.
- Menu contextual reduce pasos para acciones sobre un contacto especifico.
- Estadisticas en pestana dedicada permiten analisis rapido sin saturar el formulario.

## Estructura
- `src/modelo/persona.java`
- `src/modelo/personaDAO.java`
- `src/vista/ventana.java`
- `src/controlador/logica_ventana.java`

## Ejecucion rapida (PowerShell)
```powershell
Set-Location "C:\Users\Freddy Potes\IdeaProjects\u1c5_AGC"
if (!(Test-Path .\out)) { New-Item -ItemType Directory -Path .\out | Out-Null }
javac -d .\out .\src\modelo\*.java .\src\controlador\*.java .\src\vista\*.java
java -cp .\out vista.ventana
```

## Persistencia
- Archivo principal: `c:/gestionContactos/datosContactos.csv`
- Formato interno: separado por `;`
- Exportacion externa: CSV separado por `,`

