package modelo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


// Definicion de la clase publica "personaDAO"
public class personaDAO {

	private static final String CABECERA = "NOMBRE;TELEFONO;EMAIL;CATEGORIA;FAVORITO";

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
				escribir(CABECERA);
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
			if (CABECERA.equalsIgnoreCase(contacto.trim())) {
				continue;
			}

			String[] columnas = contacto.split(";");
			if (columnas.length < 5) {
				continue;
			}

			persona p = new persona();
			p.setNombre(columnas[0]);
			p.setTelefono(columnas[1]);
			p.setEmail(columnas[2]);
			p.setCategoria(columnas[3]);
			p.setFavorito(Boolean.parseBoolean(columnas[4]));
			personas.add(p);
		}

		return personas;
	}

	// Metodo publico para guardar los contactos modificados o eliminados
	public void actualizarContactos(List<persona> personas) throws IOException {
		try (PrintWriter writer = new PrintWriter(archivo)) {
			writer.println(CABECERA);
			for (persona p : personas) {
				writer.println(p.datosContacto());
			}
		}
	}

	public void exportarCsv(File destino, List<persona> personas) throws IOException {
		if (destino == null) {
			throw new FileNotFoundException("No se especifico ruta de exportacion");
		}

		try (PrintWriter writer = new PrintWriter(destino)) {
			writer.println("nombre,telefono,email,categoria,favorito");
			for (persona p : personas) {
				String[] cols = new String[] {
					p.getNombre(),
					p.getTelefono(),
					p.getEmail(),
					p.getCategoria(),
					String.valueOf(p.isFavorito())
				};
				writer.println(armarLineaCsv(cols));
			}
		}
	}

	private String armarLineaCsv(String[] columnas) {
		StringBuilder linea = new StringBuilder();
		for (int i = 0; i < columnas.length; i++) {
			if (i > 0) {
				linea.append(",");
			}
			linea.append(escaparCsv(columnas[i]));
		}
		return linea.toString();
	}

	private String escaparCsv(String valor) {
		String limpio = valor == null ? "" : valor;
		if (limpio.contains(",") || limpio.contains("\"") || limpio.contains("\n")) {
			limpio = limpio.replace("\"", "\"\"");
			return "\"" + limpio + "\"";
		}
		return limpio;
	}
}
