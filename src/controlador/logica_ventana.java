package controlador;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import modelo.persona;
import modelo.personaDAO;
import vista.ventana;

public class logica_ventana implements ActionListener, ListSelectionListener, ItemListener {

    private final ventana delegado;
    private final personaDAO dao;

    private String nombres;
    private String email;
    private String telefono;
    private String categoria = "";
    private boolean favorito = false;

    private List<persona> contactos;
    private TableRowSorter<DefaultTableModel> sorter;

    public logica_ventana(ventana delegado) {
        this.delegado = delegado;
        this.dao = new personaDAO(new persona());
        this.contactos = new ArrayList<persona>();

        configurarEventos();
        configurarTablaYFiltro();
        configurarAtajos();
        configurarMenuContextual();
        cargarContactosRegistrados();
    }

    private void configurarEventos() {
        delegado.btn_add.addActionListener(this);
        delegado.btn_eliminar.addActionListener(this);
        delegado.btn_modificar.addActionListener(this);
        delegado.btn_exportar.addActionListener(this);
        delegado.cmb_categoria.addItemListener(this);
        delegado.chb_favorito.addItemListener(this);
        delegado.tbl_contactos.getSelectionModel().addListSelectionListener(this);

        delegado.mnu_editar.addActionListener(this);
        delegado.mnu_eliminar.addActionListener(this);
        delegado.mnu_favorito.addActionListener(this);
        delegado.mnu_exportar.addActionListener(this);
    }

    private void configurarTablaYFiltro() {
        sorter = new TableRowSorter<DefaultTableModel>(delegado.modeloTabla);
        delegado.tbl_contactos.setRowSorter(sorter);

        delegado.txt_buscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltro();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltro();
            }
        });

        delegado.tbl_contactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mostrarMenuSiCorresponde(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mostrarMenuSiCorresponde(e);
            }

            private void mostrarMenuSiCorresponde(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }

                int fila = delegado.tbl_contactos.rowAtPoint(e.getPoint());
                if (fila >= 0) {
                    delegado.tbl_contactos.setRowSelectionInterval(fila, fila);
                }
                delegado.menuContextual.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void configurarAtajos() {
        InputMap inputMap = delegado.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = delegado.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "atajo_guardar");
        actionMap.put("atajo_guardar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guardarDesdeAtajo();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "atajo_nuevo");
        actionMap.put("atajo_nuevo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                limpiarCampos();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "atajo_eliminar");
        actionMap.put("atajo_eliminar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eliminarSeleccionado();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK), "atajo_exportar");
        actionMap.put("atajo_exportar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportarCsv();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "atajo_buscar");
        actionMap.put("atajo_buscar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                delegado.txt_buscar.requestFocus();
                delegado.txt_buscar.selectAll();
            }
        });
    }

    private void configurarMenuContextual() {
        delegado.tbl_contactos.setComponentPopupMenu(delegado.menuContextual);
    }

    private void cargarContactosRegistrados() {
        delegado.pgb_carga.setIndeterminate(true);
        delegado.pgb_carga.setString("Cargando contactos...");

        SwingWorker<List<persona>, Void> worker = new SwingWorker<List<persona>, Void>() {
            @Override
            protected List<persona> doInBackground() throws Exception {
                return dao.leerArchivo();
            }

            @Override
            protected void done() {
                try {
                    contactos = get();
                    refrescarTabla();
                    actualizarEstadisticas();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(delegado, "Problemas al cargar los contactos");
                } finally {
                    delegado.pgb_carga.setIndeterminate(false);
                    delegado.pgb_carga.setValue(100);
                    delegado.pgb_carga.setString("Carga completada");
                }
            }
        };

        worker.execute();
    }

    private void refrescarTabla() {
        DefaultTableModel model = delegado.modeloTabla;
        model.setRowCount(0);
        for (persona p : contactos) {
            model.addRow(p.comoFilaTabla());
        }
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String texto = delegado.txt_buscar.getText() == null ? "" : delegado.txt_buscar.getText().trim();
        if (texto.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        String patron = "(?i)" + Pattern.quote(texto);
        sorter.setRowFilter(RowFilter.regexFilter(patron, 0, 1, 2));
    }

    private void inicializacionCampos() {
        nombres = delegado.txt_nombres.getText() == null ? "" : delegado.txt_nombres.getText().trim();
        email = delegado.txt_email.getText() == null ? "" : delegado.txt_email.getText().trim();
        telefono = delegado.txt_telefono.getText() == null ? "" : delegado.txt_telefono.getText().trim();
    }

    private boolean validarCampos() {
        if (nombres.isEmpty() || telefono.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(delegado, "Todos los campos deben estar llenos");
            return false;
        }

        if (categoria == null || categoria.isEmpty() || "Elija una Categoria".equals(categoria)) {
            JOptionPane.showMessageDialog(delegado, "Seleccione una categoria valida");
            return false;
        }
        return true;
    }

    private int obtenerIndiceModeloSeleccionado() {
        int filaVista = delegado.tbl_contactos.getSelectedRow();
        if (filaVista < 0) {
            return -1;
        }
        return delegado.tbl_contactos.convertRowIndexToModel(filaVista);
    }

    private void limpiarCampos() {
        delegado.txt_nombres.setText("");
        delegado.txt_telefono.setText("");
        delegado.txt_email.setText("");
        delegado.chb_favorito.setSelected(false);
        delegado.cmb_categoria.setSelectedIndex(0);
        delegado.tbl_contactos.clearSelection();
        categoria = "";
        favorito = false;
        delegado.txt_nombres.requestFocus();
    }

    private void guardarCambiosDisco() {
        try {
            dao.actualizarContactos(contactos);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(delegado, "No se pudo guardar en archivo");
        }
    }

    private void agregarContacto() {
        inicializacionCampos();
        if (!validarCampos()) {
            return;
        }

        persona p = new persona(nombres, telefono, email, categoria, favorito);
        contactos.add(p);
        guardarCambiosDisco();
        refrescarTabla();
        actualizarEstadisticas();
        limpiarCampos();
        JOptionPane.showMessageDialog(delegado, "Contacto registrado");
    }

    private void modificarContactoSeleccionado() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, "Seleccione un contacto para modificar");
            return;
        }

        inicializacionCampos();
        if (!validarCampos()) {
            return;
        }

        persona p = contactos.get(index);
        p.setNombre(nombres);
        p.setTelefono(telefono);
        p.setEmail(email);
        p.setCategoria(categoria);
        p.setFavorito(favorito);

        guardarCambiosDisco();
        refrescarTabla();
        actualizarEstadisticas();
        JOptionPane.showMessageDialog(delegado, "Contacto actualizado");
    }

    private void eliminarSeleccionado() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, "Seleccione un contacto para eliminar");
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
            delegado,
            "Esta seguro de eliminar el contacto?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION
        );

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        contactos.remove(index);
        guardarCambiosDisco();
        refrescarTabla();
        actualizarEstadisticas();
        limpiarCampos();
    }

    private void alternarFavoritoSeleccionado() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, "Seleccione un contacto");
            return;
        }

        persona p = contactos.get(index);
        p.setFavorito(!p.isFavorito());
        guardarCambiosDisco();
        refrescarTabla();
        actualizarEstadisticas();
        cargarContacto(index);
    }

    private void prepararEdicionSeleccionada() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, "Seleccione un contacto");
            return;
        }

        delegado.tabs.setSelectedIndex(0);
        cargarContacto(index);
        delegado.txt_nombres.requestFocus();
    }

    private void guardarDesdeAtajo() {
        if (obtenerIndiceModeloSeleccionado() >= 0) {
            modificarContactoSeleccionado();
        } else {
            agregarContacto();
        }
    }

    private void exportarCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar exportacion CSV");
        chooser.setSelectedFile(new File("contactos_exportados.csv"));

        int opcion = chooser.showSaveDialog(delegado);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = chooser.getSelectedFile();
        List<persona> visibles = obtenerContactosVisibles();

        try {
            dao.exportarCsv(destino, visibles);
            JOptionPane.showMessageDialog(delegado, "CSV exportado: " + destino.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(delegado, "No se pudo exportar el archivo CSV");
        }
    }

    private List<persona> obtenerContactosVisibles() {
        List<persona> visibles = new ArrayList<persona>();
        for (int filaVista = 0; filaVista < delegado.tbl_contactos.getRowCount(); filaVista++) {
            int filaModelo = delegado.tbl_contactos.convertRowIndexToModel(filaVista);
            if (filaModelo >= 0 && filaModelo < contactos.size()) {
                visibles.add(contactos.get(filaModelo));
            }
        }
        return visibles;
    }

    private void cargarContacto(int indexModelo) {
        if (indexModelo < 0 || indexModelo >= contactos.size()) {
            return;
        }

        persona p = contactos.get(indexModelo);
        delegado.txt_nombres.setText(p.getNombre());
        delegado.txt_telefono.setText(p.getTelefono());
        delegado.txt_email.setText(p.getEmail());
        delegado.chb_favorito.setSelected(p.isFavorito());
        delegado.cmb_categoria.setSelectedItem(p.getCategoria());
    }

    private void actualizarEstadisticas() {
        int total = contactos.size();
        int favoritos = 0;
        int familia = 0;
        int amigos = 0;
        int trabajo = 0;

        for (persona p : contactos) {
            if (p.isFavorito()) {
                favoritos++;
            }

            if ("Familia".equalsIgnoreCase(p.getCategoria())) {
                familia++;
            } else if ("Amigos".equalsIgnoreCase(p.getCategoria())) {
                amigos++;
            } else if ("Trabajo".equalsIgnoreCase(p.getCategoria())) {
                trabajo++;
            }
        }

        delegado.lbl_total.setText(String.valueOf(total));
        delegado.lbl_favoritos.setText(String.valueOf(favoritos));
        delegado.lbl_familia.setText(String.valueOf(familia));
        delegado.lbl_amigos.setText(String.valueOf(amigos));
        delegado.lbl_trabajo.setText(String.valueOf(trabajo));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == delegado.btn_add) {
            agregarContacto();
        } else if (source == delegado.btn_modificar) {
            modificarContactoSeleccionado();
        } else if (source == delegado.mnu_editar) {
            prepararEdicionSeleccionada();
        } else if (source == delegado.btn_eliminar || source == delegado.mnu_eliminar) {
            eliminarSeleccionado();
        } else if (source == delegado.btn_exportar || source == delegado.mnu_exportar) {
            exportarCsv();
        } else if (source == delegado.mnu_favorito) {
            alternarFavoritoSeleccionado();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        int index = obtenerIndiceModeloSeleccionado();
        if (index >= 0) {
            cargarContacto(index);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == delegado.cmb_categoria) {
            Object selected = delegado.cmb_categoria.getSelectedItem();
            categoria = selected == null ? "" : selected.toString();
        } else if (e.getSource() == delegado.chb_favorito) {
            favorito = delegado.chb_favorito.isSelected();
        }
    }
}