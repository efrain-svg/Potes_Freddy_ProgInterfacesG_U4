package modelo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Definicion de la clase publica "personaDAO"
public class personaDAO {

    private static final String CABECERA_NUEVA = "NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO;FECHA_REGISTRO";
    private static final String CABECERA_ANTERIOR = "NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO";
    private static final char SEPARADOR_EXPORTACION = ';';
    private static final Gson GSON = construirGson();
    private static final Type TIPO_LISTA_PERSONA = new TypeToken<List<persona>>() { }.getType();

    private static Gson construirGson() {
        JsonSerializer<LocalDate> serializer = (src, typeOfSrc, context) -> new JsonPrimitive(src == null ? "" : src.toString());
        JsonDeserializer<LocalDate> deserializer = (json, type, context) -> {
            if (json == null || json.getAsString().trim().isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(json.getAsString());
            } catch (DateTimeParseException ex) {
                return LocalDate.now();
            }
        };
        return new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, serializer)
            .registerTypeAdapter(LocalDate.class, deserializer)
            .setPrettyPrinting()
            .create();
    }

    // Declaracion de atributos privados de la clase "personaDAO"
    private File archivo;
    private persona persona;

    // Constructor publico de la clase "personaDAO" que recibe un objeto "persona" como parametro
    public personaDAO(persona persona) {
        this.persona = persona;
        File base = new File("c:/gestionContactos");
        if (!base.exists()) {
            base.mkdirs();
        }
        archivo = new File(base.getAbsolutePath(), "datosContactos.csv");
        prepararArchivo();
    }

    // Metodo privado para gestionar el archivo utilizando la clase File
    private void prepararArchivo() {
        if (!archivo.exists()) {
            try {
                archivo.createNewFile();
                escribir(CABECERA_NUEVA);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void escribir(String texto) {
        try (FileWriter escribir = new FileWriter(archivo.getAbsolutePath(), true)) {
            escribir.write(texto + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo publico para escribir en el archivo
    public boolean escribirArchivo() {
        if (persona == null) {
            return false;
        }
        escribir(persona.datosContacto());
        return true;
    }

    // Metodo publico para leer los datos del archivo
    public List<persona> leerArchivo() throws IOException {
        List<persona> personas = new ArrayList<persona>();
        StringBuilder contenido = new StringBuilder();

        try (FileReader leer = new FileReader(archivo.getAbsolutePath())) {
            int c;
            while ((c = leer.read()) != -1) {
                contenido.append((char) c);
            }
        }

        String[] datos = contenido.toString().split("\\r?\\n");
        for (String contacto : datos) {
            if (contacto == null || contacto.trim().isEmpty()) {
                continue;
            }

            String registro = contacto.trim();
            if (CABECERA_NUEVA.equalsIgnoreCase(registro) || CABECERA_ANTERIOR.equalsIgnoreCase(registro)) {
                continue;
            }

            String[] columnas = registro.split(";");
            if (columnas.length < 5) {
                continue;
            }

            persona p = new persona();
            p.setNombre(columnas[0]);
            p.setTelefono(columnas[1]);
            p.setEmail(columnas[2]);
            p.setCategoria(normalizarCategoria(columnas[3]));
            p.setFavorito(Boolean.parseBoolean(columnas[4]));

            if (columnas.length >= 6) {
                try {
                    p.setFechaRegistro(LocalDate.parse(columnas[5]));
                } catch (DateTimeParseException ex) {
                    p.setFechaRegistro(LocalDate.now());
                }
            } else {
                p.setFechaRegistro(LocalDate.now());
            }

            personas.add(p);
        }

        return personas;
    }

    // Metodo publico para guardar los contactos modificados o eliminados
    public void actualizarContactos(List<persona> personas) throws IOException {
        try (PrintWriter writer = new PrintWriter(archivo)) {
            writer.println(CABECERA_NUEVA);
            for (persona p : personas) {
                writer.println(p.datosContacto());
            }
        }
    }

    public void exportarCsv(File destino, String[] cabeceras, List<String[]> filas) throws IOException {
        if (destino == null) {
            throw new FileNotFoundException("No se especifico ruta de exportacion");
        }

        try (PrintWriter writer = new PrintWriter(destino)) {
            writer.println(armarLineaCsv(cabeceras));
            for (String[] fila : filas) {
                writer.println(armarLineaCsv(fila));
            }
        }
    }

    public void exportarJson(File destino, List<persona> personas) throws IOException {
        if (destino == null) {
            throw new FileNotFoundException("No se especifico ruta de exportacion JSON");
        }

        try (FileWriter writer = new FileWriter(destino)) {
            GSON.toJson(personas, TIPO_LISTA_PERSONA, writer);
        }
    }

    public List<persona> importarJson(File fuente) throws IOException {
        if (fuente == null || !fuente.exists()) {
            throw new FileNotFoundException("No se especifico ruta de importacion JSON");
        }

        try (FileReader reader = new FileReader(fuente)) {
            List<persona> personas = GSON.fromJson(reader, TIPO_LISTA_PERSONA);
            return normalizarPersonas(personas);
        } catch (JsonParseException ex) {
            throw new IOException("JSON invalido", ex);
        }
    }

    private List<persona> normalizarPersonas(List<persona> personas) {
        List<persona> normalizadas = new ArrayList<persona>();
        if (personas == null) {
            return normalizadas;
        }

        for (persona p : personas) {
            if (p == null) {
                continue;
            }
            persona nuevo = new persona();
            nuevo.setNombre(p.getNombre());
            nuevo.setTelefono(p.getTelefono());
            nuevo.setEmail(p.getEmail());
            nuevo.setCategoria(normalizarCategoria(p.getCategoria()));
            nuevo.setFavorito(p.isFavorito());
            nuevo.setFechaRegistro(p.getFechaRegistro());
            normalizadas.add(nuevo);
        }
        return normalizadas;
    }

    private String normalizarCategoria(String valor) {
        if (valor == null) {
            return "";
        }

        String limpio = valor.trim();
        if ("FAMILY".equalsIgnoreCase(limpio) || "Familia".equalsIgnoreCase(limpio) || "Family".equalsIgnoreCase(limpio)) {
            return "FAMILY";
        }
        if ("FRIENDS".equalsIgnoreCase(limpio) || "Amigos".equalsIgnoreCase(limpio) || "Friends".equalsIgnoreCase(limpio)) {
            return "FRIENDS";
        }
        if ("WORK".equalsIgnoreCase(limpio) || "Trabajo".equalsIgnoreCase(limpio) || "Trabalho".equalsIgnoreCase(limpio)) {
            return "WORK";
        }
        return limpio;
    }

    private String armarLineaCsv(String[] columnas) {
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < columnas.length; i++) {
            if (i > 0) {
                linea.append(SEPARADOR_EXPORTACION);
            }
            linea.append(escaparCsv(columnas[i]));
        }
        return linea.toString();
    }

    private String escaparCsv(String valor) {
        String limpio = valor == null ? "" : valor;
        if (limpio.indexOf(SEPARADOR_EXPORTACION) >= 0 || limpio.contains("\"") || limpio.contains("\n")) {
            limpio = limpio.replace("\"", "\"\"");
            return "\"" + limpio + "\"";
        }
        return limpio;
    }
}
