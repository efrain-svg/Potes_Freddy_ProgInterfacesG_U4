# Aplicacion de Gestion de Contactos (Redisenada)

## Objetivo
Esta version mejora la experiencia de usuario con una interfaz mas moderna y estructurada, flujo de trabajo claro, eventos de productividad y soporte multilingue.

## Mejoras implementadas

### 1) Diseno visual y accesibilidad
- Integracion de FlatLaf (via `pom.xml`) y configuracion visual en `src/vista/theme/ThemeManager.java`.
- Paleta UX aplicada:
  - Primary: `#0d6efd`
  - Background: `#F4F6F9`
  - Surface: `#FFFFFF`
  - Texto principal: `#212529`
  - Texto secundario: `#6C757D`
  - Exito: `#198754`
  - Favoritos: `#FFC107`
- Tipografia basada en fuente del sistema (Segoe UI/Roboto fallback) con jerarquia:
  - Titulos de tarjeta: 14 Bold
  - Valores estadisticos: 28 Bold
  - Inputs y tabla: 12-13

### 2) Rediseno de interfaz (Layouts)
- Pestaña `Contactos`:
  - `BorderLayout` principal.
  - Zona superior con `GridBagLayout` para formulario y filtros en dos filas.
  - Zona central con `JTable` en `JScrollPane`.
  - Zona inferior con barra de estado y `JProgressBar`.
- Pestaña `Estadisticas`:
  - `BorderLayout` principal.
  - Tarjetas en `GridLayout(1,5,20,0)` con estilo de superficie.

### 3) Interactividad y productividad
- Atajos:
  - `Ctrl + S` guardar
  - `Ctrl + N` limpiar
  - `Delete` eliminar
  - `Ctrl + E` exportar CSV
  - `Ctrl + F` enfocar busqueda
- Menu contextual (clic derecho sobre tabla): editar, eliminar, alternar favorito, exportar visibles.
- Tabla con ordenamiento, filtro por texto y filtro por categoria.
- Columna de favoritos con estrellas `★/☆`.

### 4) Soporte multilingue con ResourceBundle
- Idiomas soportados:
  1. Espanol
  2. Ingles
  3. Portugues (pt-BR)
- Recursos:
  - `src/i18n/messages_es.properties`
  - `src/i18n/messages_en.properties`
  - `src/i18n/messages_pt.properties`
- Carga de idioma en `src/vista/i18n/I18n.java`.
- Cambio de idioma desde `cmb_idioma` en tiempo real.
- Formato de fecha localizado en estado/estadisticas.

### 5) MVC y persistencia
- Modelo:
  - `src/modelo/persona.java`: entidad con fecha de registro.
  - `src/modelo/personaDAO.java`: lectura/escritura retrocompatible y exportacion CSV.
- Vista:
  - `src/vista/ventana.java`: componentes y layout.
- Controlador:
  - `src/controlador/logica_ventana.java`: eventos, validaciones, i18n, filtros y sincronizacion.

### 6) U3: Concurrencia y sincronizacion
- Busqueda asincrona con cancelacion para listas grandes (SwingWorker).
- Exportacion CSV en pool de hilos con lock para evitar corrupcion.
- Validacion de duplicados en segundo plano y estado UI ocupado.
- Bloqueo de edicion para prevenir modificaciones simultaneas.
- Notificaciones de estado en hilo dedicado con actualizacion segura del UI.

## Ejecucion rapida (PowerShell)
```powershell
Set-Location "C:\Users\Freddy Potes\IdeaProjects\u1c5_AGC"
if (!(Test-Path .\out)) { New-Item -ItemType Directory -Path .\out | Out-Null }
javac -cp ".\lib\flatlaf-3.6.jar;.\lib\flatlaf-extras-3.6.jar;.\lib\jsvg-1.4.0.jar" -d .\out .\src\modelo\*.java .\src\controlador\*.java .\src\vista\*.java .\src\vista\charts\*.java .\src\vista\i18n\*.java .\src\vista\theme\*.java
java -cp ".\out;.\lib\flatlaf-3.6.jar;.\lib\flatlaf-extras-3.6.jar;.\lib\jsvg-1.4.0.jar" vista.ventana
```

## Opcion con Maven (incluye FlatLaf)
```powershell
Set-Location "C:\Users\Freddy Potes\IdeaProjects\u1c5_AGC"
mvn clean package
```

## Persistencia
- Archivo interno: `c:/gestionContactos/datosContactos.csv`
- Formato interno: `;` con cabecera `NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO;FECHA_REGISTRO`
- Exportacion externa: CSV con separador `,`
