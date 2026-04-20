package modelo;

// Definicion de la clase publica "persona"
public class persona {

	// Declaracion de variables privadas de la clase "persona"
	private String nombre, telefono, email, categoria;
	private boolean favorito;

	// Constructor publico de la clase "persona"
	public persona() {
		super();
		// Inicializa las variables
		this.nombre = "";
		this.telefono = "";
		this.email = "";
		this.categoria = "";
		this.favorito = false;
	}

	// Constructor publico de la clase "persona" que inicializa todos los campos
	public persona(String nombre, String telefono, String email, String categoria, boolean favorito) {
		super();
		this.nombre = limpiar(nombre);
		this.telefono = limpiar(telefono);
		this.email = limpiar(email);
		this.categoria = limpiar(categoria);
		this.favorito = favorito;
	}

	private String limpiar(String valor) {
		return valor == null ? "" : valor.trim();
	}

	// Metodo publico para obtener el valor de "nombre"
	public String getNombre() {
		return nombre;
	}

	// Metodo publico para establecer el valor de "nombre"
	public void setNombre(String nombre) {
		this.nombre = limpiar(nombre);
	}

	// Metodo publico para obtener el valor de "telefono"
	public String getTelefono() {
		return telefono;
	}

	// Metodo publico para establecer el valor de "telefono"
	public void setTelefono(String telefono) {
		this.telefono = limpiar(telefono);
	}

	// Metodo publico para obtener el valor de "email"
	public String getEmail() {
		return email;
	}

	// Metodo publico para establecer el valor de "email"
	public void setEmail(String email) {
		this.email = limpiar(email);
	}

	// Metodo publico para obtener el valor de "categoria"
	public String getCategoria() {
		return categoria;
	}

	// Metodo publico para establecer el valor de "categoria"
	public void setCategoria(String categoria) {
		this.categoria = limpiar(categoria);
	}

	// Metodo publico para verificar si el contacto es "favorito"
	public boolean isFavorito() {
		return favorito;
	}

	// Metodo publico para establecer si el contacto es "favorito"
	public void setFavorito(boolean favorito) {
		this.favorito = favorito;
	}

	// Metodo para proveer un formato para almacenar en un archivo
	public String datosContacto() {
		return String.format("%s;%s;%s;%s;%s", nombre, telefono, email, categoria, favorito);
	}

	// Metodo para proveer el formato de los campos que se van a imprimir en la lista
	public String formatoLista() {
		String contacto = String.format("%-40s%-40s%-40s%-40s", nombre, telefono, email, categoria);
		return contacto;
	}

	// Mapea el contacto para ser mostrado directamente en JTable.
	public Object[] comoFilaTabla() {
		return new Object[] { nombre, telefono, email, categoria, favorito ? "Si" : "No" };
	}

}
