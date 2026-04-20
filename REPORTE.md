# Reporte de mejoras implementadas

## Resumen
Se modernizo la aplicacion de Gestion de Contactos en Swing para mejorar usabilidad, interactividad y mantenibilidad. Se incluyeron componentes graficos nuevos, eventos de teclado/mouse, funcionalidades avanzadas y una separacion MVC mas clara.

## Componentes agregados
1. `JTabbedPane`
   - Pestana `Contactos` para operaciones CRUD, busqueda, tabla y exportacion.
   - Pestana `Estadisticas` para metricas de uso.
2. `JTable`
   - Visualizacion tabular de nombre, telefono, email, categoria y favorito.
3. `JProgressBar`
   - Indicador durante la carga inicial de contactos.
4. `JPopupMenu`
   - Acciones contextuales con clic derecho sobre filas.

## Eventos utilizados
- `ActionListener`: botones principales y opciones del menu contextual.
- `ListSelectionListener`: sincronizacion fila seleccionada -> formulario.
- `ItemListener`: cambios en categoria/favorito.
- `DocumentListener`: filtrado en tiempo real en el campo de busqueda.
- `MouseAdapter` + popup trigger: menu contextual por clic derecho.
- Atajos con `InputMap/ActionMap`:
  - `Ctrl+S`, `Ctrl+N`, `Delete`, `Ctrl+E`, `Ctrl+F`.

## Funcionalidades avanzadas
- Ordenamiento por columnas con `TableRowSorter`.
- Filtrado por texto con `RowFilter`.
- Exportacion CSV de contactos visibles (segun filtro activo).
- Carga asincrona con `SwingWorker` y feedback visual en `JProgressBar`.

## Aplicacion del patron MVC
- **Modelo** (`src/modelo`):
  - `persona.java`: datos del contacto y mapeo para tabla.
  - `personaDAO.java`: lectura/escritura en archivo y exportacion.
- **Vista** (`src/vista`):
  - `ventana.java`: construccion de interfaz y componentes.
- **Controlador** (`src/controlador`):
  - `logica_ventana.java`: reglas de negocio, eventos, validaciones y coordinacion.

## Justificacion de la interfaz
- La separacion en pestanas mejora la claridad: operacion y analitica en secciones distintas.
- La tabla mejora eficiencia en revision de datos y facilita ordenar/buscar.
- Los atajos reducen tiempo en tareas frecuentes.
- El menu contextual aporta acciones directas en el elemento objetivo.
- La barra de progreso comunica estado y evita incertidumbre de carga.

