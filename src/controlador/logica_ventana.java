package controlador;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import modelo.persona;
import modelo.personaDAO;
import vista.ventana;
import vista.i18n.I18n;
import vista.theme.SvgIconLoader;

public class logica_ventana implements ActionListener, ListSelectionListener, ItemListener {

    private static final String CAT_FAMILY = "FAMILY";
    private static final String CAT_FRIENDS = "FRIENDS";
    private static final String CAT_WORK = "WORK";

    private static final int COL_NOMBRE = 0;
    private static final int COL_TELEFONO = 1;
    private static final int COL_EMAIL = 2;
    private static final int COL_CATEGORIA = 3;
    private static final int COL_FAV = 4;

    private final ventana delegado;
    private final personaDAO dao;
    private final I18n i18n;

    private String nombres;
    private String email;
    private String telefono;
    private String categoria = "";
    private boolean favorito = false;
    private boolean actualizandoCombos = false;

    private List<persona> contactos;
    private TableRowSorter<DefaultTableModel> sorter;

    private final javax.swing.Icon iconStarOn = SvgIconLoader.load("star-filled.svg", 16, 16);
    private final javax.swing.Icon iconStarOff = SvgIconLoader.load("star-outline.svg", 16, 16);

    private final Object contactosLock = new Object();
    private final Object exportLock = new Object();
    private final ReentrantLock editLock = new ReentrantLock();
    private int editLockIndex = -1;
    private final ExecutorService exportExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger searchSeq = new AtomicInteger(0);
    private final AtomicInteger busyCount = new AtomicInteger(0);
    private SwingWorker<RowFilter<Object, Object>, Void> searchWorker;

    public logica_ventana(ventana delegado) {
        this.delegado = delegado;
        this.dao = new personaDAO(new persona());
        this.i18n = new I18n();
        this.contactos = new ArrayList<>();

        configurarEventos();
        configurarTablaYFiltro();
        configurarAtajos();
        configurarMenuContextual();
        configurarCierre();
        aplicarIdioma(I18n.LANG_ES);
        cargarContactosRegistrados();
    }

    private void configurarCierre() {
        delegado.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exportExecutor.shutdown();
                notificationExecutor.shutdown();
            }
        });
    }

    private void configurarEventos() {
        delegado.btn_add.addActionListener(this);
        delegado.btn_eliminar.addActionListener(this);
        delegado.btn_modificar.addActionListener(this);
        delegado.btn_exportar.addActionListener(this);
        delegado.btn_importar_json.addActionListener(this);
        delegado.btn_exportar_json.addActionListener(this);
        delegado.cmb_categoria.addItemListener(this);
        delegado.cmb_filtro_categoria.addItemListener(this);
        delegado.cmb_idioma.addItemListener(this);
        delegado.chb_favorito.addItemListener(this);
        delegado.tbl_contactos.getSelectionModel().addListSelectionListener(this);

        delegado.mnu_editar.addActionListener(this);
        delegado.mnu_eliminar.addActionListener(this);
        delegado.mnu_favorito.addActionListener(this);
        delegado.mnu_exportar.addActionListener(this);
    }

    private void configurarTablaYFiltro() {
        sorter = new TableRowSorter<>(delegado.modeloTabla);

        configurarColumnasTabla();

        delegado.tbl_contactos.getTableHeader().setResizingAllowed(true);
        delegado.tbl_contactos.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        delegado.tbl_contactos.setRowHeight(34);
        delegado.tbl_contactos.setShowHorizontalLines(false);
        delegado.tbl_contactos.setShowVerticalLines(false);
        delegado.tbl_contactos.setIntercellSpacing(new java.awt.Dimension(0, 8));
        delegado.tbl_contactos.setRowMargin(0);

        delegado.txt_buscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                solicitarBusquedaAsync();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                solicitarBusquedaAsync();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                solicitarBusquedaAsync();
            }
        });

        delegado.tbl_contactos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 1) {
                    return;
                }

                int fila = delegado.tbl_contactos.rowAtPoint(e.getPoint());
                int col = delegado.tbl_contactos.columnAtPoint(e.getPoint());
                if (fila < 0 || col < 0) {
                    return;
                }

                delegado.tbl_contactos.setRowSelectionInterval(fila, fila);
                if (col == COL_FAV) {
                    alternarFavoritoSeleccionado();
                    return;
                }

                // double-click anywhere on a row opens edit
                if (e.getClickCount() == 2) {
                    prepararEdicionSeleccionada();
                }
            }

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
        actualizarEstado(i18n.t("status.loading"));

        SwingWorker<List<persona>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<persona> doInBackground() throws Exception {
                return dao.leerArchivo();
            }

            @Override
            protected void done() {
                try {
                    List<persona> cargados = get();
                    synchronized (contactosLock) {
                        contactos = cargados;
                    }
                    refrescarTabla();
                    actualizarEstadisticas();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(delegado, i18n.t("msg.loadError"));
                } finally {
                    delegado.pgb_carga.setIndeterminate(false);
                    delegado.pgb_carga.setValue(100);
                    actualizarEstado(MessageFormat.format(i18n.t("status.loaded"), contactos.size(), i18n.formatDate(LocalDate.now())));
                }
            }
        };

        worker.execute();
    }

    private void refrescarTabla() {
        DefaultTableModel model = delegado.modeloTabla;
        model.setRowCount(0);
        synchronized (contactosLock) {
            for (persona p : contactos) {
                model.addRow(new Object[] {
                    p.getNombre(),
                    p.getTelefono(),
                    p.getEmail(),
                    categoriaLabelPorCodigo(p.getCategoria()),
                    p.isFavorito()
                });
            }
        }
        solicitarBusquedaAsync();
        configurarColumnasTabla();
    }

    private void solicitarBusquedaAsync() {
        final String texto = delegado.txt_buscar.getText() == null ? "" : delegado.txt_buscar.getText().trim();
        final String codigoFiltroCategoria = obtenerCodigoCategoriaFiltro();
        final int seq = searchSeq.incrementAndGet();

        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }

        // Busqueda en segundo plano para evitar bloqueos en UI con listas grandes.
        notificarAsync(i18n.t("status.searching"));
        searchWorker = new SwingWorker<>() {
            @Override
            protected RowFilter<Object, Object> doInBackground() {
                List<RowFilter<Object, Object>> filtros = new ArrayList<>();

                if (!texto.isEmpty()) {
                    String patron = "(?i)" + Pattern.quote(texto);
                    filtros.add(RowFilter.regexFilter(patron, COL_NOMBRE, COL_TELEFONO, COL_EMAIL));
                }

                if (!codigoFiltroCategoria.isEmpty()) {
                    filtros.add(RowFilter.regexFilter("^" + Pattern.quote(categoriaLabelPorCodigo(codigoFiltroCategoria)) + "$", COL_CATEGORIA));
                }

                if (filtros.isEmpty()) {
                    return null;
                }
                return RowFilter.andFilter(filtros);
            }

            @Override
            protected void done() {
                if (isCancelled() || seq != searchSeq.get()) {
                    return;
                }

                RowFilter<Object, Object> filtro = null;
                try {
                    filtro = get();
                } catch (Exception ex) {
                    filtro = null;
                }

                final RowFilter<Object, Object> filtroFinal = filtro;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        sorter.setRowFilter(filtroFinal);
                        actualizarEstado(i18n.t("status.ready"));
                    }
                });
            }
        };

        searchWorker.execute();
    }

    private void aplicarFiltro() {
        solicitarBusquedaAsync();
    }

    private void inicializacionCampos() {
        nombres = delegado.txt_nombres.getText() == null ? "" : delegado.txt_nombres.getText().trim();
        email = delegado.txt_email.getText() == null ? "" : delegado.txt_email.getText().trim();
        telefono = delegado.txt_telefono.getText() == null ? "" : delegado.txt_telefono.getText().trim();
        categoria = obtenerCodigoCategoriaFormulario();
    }

    private boolean validarCampos() {
        if (nombres.isEmpty() || telefono.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.fillAll"));
            return false;
        }

        if (categoria == null || categoria.isEmpty()) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.selectCategory"));
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
            synchronized (contactosLock) {
                dao.actualizarContactos(contactos);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.saveError"));
        }
    }

    private void agregarContacto() {
        inicializacionCampos();
        if (!validarCampos()) {
            return;
        }

        validarContactoAsync(-1, new Runnable() {
            @Override
            public void run() {
                persona p = new persona(nombres, telefono, email, categoria, favorito);
                synchronized (contactosLock) {
                    contactos.add(p);
                }
                guardarCambiosDisco();
                refrescarTabla();
                actualizarEstadisticas();
                limpiarCampos();
                notificarAsync(i18n.t("msg.added"));
            }
        });
    }

    private void modificarContactoSeleccionado() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.selectToEdit"));
            return;
        }

        if (!bloquearEdicion(index)) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.editLocked"));
            return;
        }

        inicializacionCampos();
        if (!validarCampos()) {
            liberarEdicionSiCorresponde(index);
            return;
        }

        validarContactoAsync(index, new Runnable() {
            @Override
            public void run() {
                synchronized (contactosLock) {
                    persona p = contactos.get(index);
                    p.setNombre(nombres);
                    p.setTelefono(telefono);
                    p.setEmail(email);
                    p.setCategoria(categoria);
                    p.setFavorito(favorito);
                }

                guardarCambiosDisco();
                refrescarTabla();
                actualizarEstadisticas();
                liberarEdicionSiCorresponde(index);
                notificarAsync(i18n.t("msg.updated"));
            }
        });
    }

    private void eliminarSeleccionado() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.selectToDelete"));
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(
            delegado,
            i18n.t("msg.confirmDelete"),
            i18n.t("msg.confirmTitle"),
            JOptionPane.YES_NO_OPTION
        );

        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        synchronized (contactosLock) {
            contactos.remove(index);
        }
        guardarCambiosDisco();
        refrescarTabla();
        actualizarEstadisticas();
        limpiarCampos();
        liberarEdicionSiCorresponde(index);
        notificarAsync(i18n.t("msg.deleted"));
    }

    private void alternarFavoritoSeleccionado() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.selectContact"));
            return;
        }

        synchronized (contactosLock) {
            persona p = contactos.get(index);
            p.setFavorito(!p.isFavorito());
        }
        guardarCambiosDisco();
        refrescarTabla();
        actualizarEstadisticas();
        cargarContacto(index);
    }

    private void prepararEdicionSeleccionada() {
        int index = obtenerIndiceModeloSeleccionado();
        if (index < 0) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.selectContact"));
            return;
        }

        if (!bloquearEdicion(index)) {
            JOptionPane.showMessageDialog(delegado, i18n.t("msg.editLocked"));
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
        chooser.setDialogTitle(i18n.t("msg.exportDialog"));
        chooser.setSelectedFile(new File(i18n.t("csv.filename") + ".csv"));

        int opcion = chooser.showSaveDialog(delegado);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = chooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".csv")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".csv");
        }

        if (destino.exists()) {
            int confirmacion = JOptionPane.showConfirmDialog(
                delegado,
                i18n.t("msg.overwriteQuestion"),
                i18n.t("msg.confirmTitle"),
                JOptionPane.YES_NO_OPTION
            );
            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File destinoFinal = destino;
        final List<persona> visibles = obtenerContactosVisibles();
        final String[] cabeceras = obtenerCabecerasCsv();
        final List<String[]> filas = construirFilasCsv(visibles);

        setUiBusy(true);
        notificarAsync(i18n.t("status.exporting"));
        // Exportacion en segundo plano con sincronizacion para evitar corrupcion.
        exportExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (exportLock) {
                        dao.exportarCsv(destinoFinal, cabeceras, filas);
                    }
                    notificarAsync(i18n.t("status.exported"));
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setUiBusy(false);
                            JOptionPane.showMessageDialog(delegado, MessageFormat.format(i18n.t("msg.exported"), destinoFinal.getAbsolutePath()));
                        }
                    });
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setUiBusy(false);
                            JOptionPane.showMessageDialog(delegado, i18n.t("msg.exportError"));
                        }
                    });
                }
            }
        });
    }

    private void exportarJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(i18n.t("msg.exportJsonDialog"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));
        chooser.setSelectedFile(new File(i18n.t("json.filename") + ".json"));

        int opcion = chooser.showSaveDialog(delegado);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = chooser.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".json")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".json");
        }

        if (destino.exists()) {
            int confirmacion = JOptionPane.showConfirmDialog(
                delegado,
                i18n.t("msg.overwriteQuestion"),
                i18n.t("msg.confirmTitle"),
                JOptionPane.YES_NO_OPTION
            );
            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File destinoFinal = destino;
        final List<persona> visibles = obtenerContactosVisibles();

        setUiBusy(true);
        notificarAsync(i18n.t("status.exportingJson"));
        exportExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (exportLock) {
                        dao.exportarJson(destinoFinal, visibles);
                    }
                    notificarAsync(i18n.t("status.exportedJson"));
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setUiBusy(false);
                            JOptionPane.showMessageDialog(delegado, MessageFormat.format(i18n.t("msg.exportedJson"), destinoFinal.getAbsolutePath()));
                        }
                    });
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            setUiBusy(false);
                            JOptionPane.showMessageDialog(delegado, i18n.t("msg.exportJsonError"));
                        }
                    });
                }
            }
        });
    }

    private void importarJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(i18n.t("msg.importJsonDialog"));
        chooser.setFileFilter(new FileNameExtensionFilter("JSON (*.json)", "json"));

        int opcion = chooser.showOpenDialog(delegado);
        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final File fuente = chooser.getSelectedFile();
        setUiBusy(true);
        notificarAsync(i18n.t("status.importing"));

        exportExecutor.submit(() -> {
            try {
                ResultadoImportacion resultado = procesarImportacion(dao.importarJson(fuente));
                SwingUtilities.invokeLater(() -> {
                    setUiBusy(false);
                    refrescarTabla();
                    actualizarEstadisticas();
                    if (resultado.total == 0) {
                        JOptionPane.showMessageDialog(delegado, i18n.t("msg.importJsonEmpty"));
                    } else if (resultado.agregados == 0 && resultado.duplicados > 0) {
                        JOptionPane.showMessageDialog(delegado, i18n.t("msg.importJsonAllDuplicates"));
                    } else {
                        JOptionPane.showMessageDialog(
                            delegado,
                            MessageFormat.format(i18n.t("msg.importedJsonDedup"), resultado.agregados, resultado.duplicados)
                        );
                    }
                    notificarAsync(i18n.t("status.imported"));
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    setUiBusy(false);
                    JOptionPane.showMessageDialog(delegado, i18n.t("msg.importJsonError"));
                });
            }
        });
    }

    private ResultadoImportacion procesarImportacion(List<persona> importados) throws IOException {
        ResultadoImportacion resultado = new ResultadoImportacion(importados == null ? 0 : importados.size());
        if (importados == null || importados.isEmpty()) {
            return resultado;
        }

        List<persona> nuevos = new ArrayList<>();
        synchronized (contactosLock) {
            Set<String> emails = new HashSet<>();
            Set<String> telefonos = new HashSet<>();
            for (persona existente : contactos) {
                registrarClaveContacto(existente, emails, telefonos);
            }

            for (persona candidato : importados) {
                if (candidato == null) {
                    continue;
                }
                String emailNorm = normalizarEmail(candidato.getEmail());
                String telNorm = normalizarTelefono(candidato.getTelefono());
                if (esDuplicadoEnSets(emailNorm, telNorm, emails, telefonos)) {
                    resultado.duplicados++;
                    continue;
                }
                nuevos.add(candidato);
                registrarClaveContacto(emailNorm, telNorm, emails, telefonos);
            }

            if (!nuevos.isEmpty()) {
                contactos.addAll(nuevos);
                dao.actualizarContactos(contactos);
            }
        }
        resultado.agregados = nuevos.size();
        return resultado;
    }

    private static final class ResultadoImportacion {
        private final int total;
        private int agregados;
        private int duplicados;

        private ResultadoImportacion(int total) {
            this.total = total;
        }
    }

    private String[] obtenerCabecerasCsv() {
        return new String[] {
            i18n.t("csv.header.name"),
            i18n.t("csv.header.phone"),
            i18n.t("csv.header.email"),
            i18n.t("csv.header.category"),
            i18n.t("csv.header.favorite"),
            i18n.t("csv.header.date")
        };
    }

    private List<String[]> construirFilasCsv(List<persona> personasVisibles) {
        List<String[]> filas = new ArrayList<>();
        for (persona p : personasVisibles) {
            String fecha = p.getFechaRegistro() == null ? "" : i18n.formatDate(p.getFechaRegistro());
            filas.add(new String[] {
                p.getNombre(),
                p.getTelefono(),
                p.getEmail(),
                categoriaLabelPorCodigo(p.getCategoria()),
                p.isFavorito() ? i18n.t("csv.value.yes") : i18n.t("csv.value.no"),
                fecha
            });
        }
        return filas;
    }

    private List<persona> obtenerContactosVisibles() {
        List<persona> visibles = new ArrayList<>();
        synchronized (contactosLock) {
            for (int filaVista = 0; filaVista < delegado.tbl_contactos.getRowCount(); filaVista++) {
                int filaModelo = delegado.tbl_contactos.convertRowIndexToModel(filaVista);
                if (filaModelo >= 0 && filaModelo < contactos.size()) {
                    visibles.add(contactos.get(filaModelo));
                }
            }
        }
        return visibles;
    }

    private void cargarContacto(int indexModelo) {
        if (indexModelo < 0) {
            return;
        }

        synchronized (contactosLock) {
            if (indexModelo >= contactos.size()) {
                return;
            }
            persona p = contactos.get(indexModelo);
            delegado.txt_nombres.setText(p.getNombre());
            delegado.txt_telefono.setText(p.getTelefono());
            delegado.txt_email.setText(p.getEmail());
            delegado.chb_favorito.setSelected(p.isFavorito());
            delegado.cmb_categoria.setSelectedIndex(indiceCategoriaFormulario(p.getCategoria()));
        }
    }

    private void actualizarEstadisticas() {
        int total = 0;
        int favoritosCount = 0;
        int familia = 0;
        int amigos = 0;
        int trabajo = 0;

        synchronized (contactosLock) {
            total = contactos.size();
            for (persona p : contactos) {
                if (p.isFavorito()) {
                    favoritosCount++;
                }

                if (CAT_FAMILY.equalsIgnoreCase(p.getCategoria())) {
                    familia++;
                } else if (CAT_FRIENDS.equalsIgnoreCase(p.getCategoria())) {
                    amigos++;
                } else if (CAT_WORK.equalsIgnoreCase(p.getCategoria())) {
                    trabajo++;
                }
            }
        }

        delegado.lbl_total.setText(String.valueOf(total));
        delegado.lbl_favoritos.setText(String.valueOf(favoritosCount));
        delegado.lbl_familia.setText(String.valueOf(familia));
        delegado.lbl_amigos.setText(String.valueOf(amigos));
        delegado.lbl_trabajo.setText(String.valueOf(trabajo));
        delegado.lbl_actualizacion.setText(MessageFormat.format(i18n.t("stats.updated"), i18n.formatDate(LocalDate.now())));

        int otros = Math.max(0, total - familia - amigos - trabajo);
        actualizarGraficas(familia, amigos, trabajo, otros);
    }

    private void actualizarGraficas(int familia, int amigos, int trabajo, int otros) {
        delegado.pnl_chart_barras.setChart(
            i18n.t("stats.chart.categories"),
            new String[] {
                i18n.t("cat.family"),
                i18n.t("cat.friends"),
                i18n.t("cat.work"),
                i18n.t("cat.other")
            },
            new int[] { familia, amigos, trabajo, otros }
        );

        delegado.pnl_chart_pastel.setChart(
            i18n.t("stats.chart.distribution"),
            new String[] {
                i18n.t("cat.family"),
                i18n.t("cat.friends"),
                i18n.t("cat.work")
            },
            new int[] { familia, amigos, trabajo }
        );
    }

    private void actualizarEstado(String texto) {
        delegado.lbl_estado.setText("\u2022 " + texto);
    }

    private String obtenerCodigoCategoriaFormulario() {
        return obtenerCodigoCategoriaPorIndice(delegado.cmb_categoria.getSelectedIndex());
    }

    private String obtenerCodigoCategoriaFiltro() {
        return obtenerCodigoCategoriaPorIndice(delegado.cmb_filtro_categoria.getSelectedIndex());
    }

    private String obtenerCodigoCategoriaPorIndice(int idx) {
        if (idx == 1) {
            return CAT_FAMILY;
        }
        if (idx == 2) {
            return CAT_FRIENDS;
        }
        if (idx == 3) {
            return CAT_WORK;
        }
        return "";
    }

    private int indiceCategoriaFormulario(String codigo) {
        if (CAT_FAMILY.equalsIgnoreCase(codigo)) {
            return 1;
        }
        if (CAT_FRIENDS.equalsIgnoreCase(codigo)) {
            return 2;
        }
        if (CAT_WORK.equalsIgnoreCase(codigo)) {
            return 3;
        }
        return 0;
    }

    private String categoriaLabelPorCodigo(String codigo) {
        if (CAT_FAMILY.equalsIgnoreCase(codigo)) {
            return i18n.t("cat.family");
        }
        if (CAT_FRIENDS.equalsIgnoreCase(codigo)) {
            return i18n.t("cat.friends");
        }
        if (CAT_WORK.equalsIgnoreCase(codigo)) {
            return i18n.t("cat.work");
        }
        return i18n.t("cat.other");
    }

    private String obtenerCodigoIdiomaSeleccionado() {
        Object selected = delegado.cmb_idioma.getSelectedItem();
        if (selected == null) {
            return I18n.LANG_ES;
        }
        String code = selected.toString().trim().toLowerCase();
        if (I18n.LANG_EN.equals(code) || I18n.LANG_PT.equals(code)) {
            return code;
        }
        return I18n.LANG_ES;
    }

    private void aplicarIdioma(String code) {
        i18n.setLanguage(code);

        delegado.setTitle(i18n.t("app.title"));
        if (delegado.lbl_tab_contactos != null) {
            delegado.lbl_tab_contactos.setText(i18n.t("header.config"));
        }
        if (delegado.lbl_tab_estadisticas != null) {
            delegado.lbl_tab_estadisticas.setText(i18n.t("tab.stats"));
        }
        delegado.tabs.setTitleAt(0, i18n.t("tab.contacts"));
        delegado.tabs.setTitleAt(1, i18n.t("tab.stats"));

        delegado.lbl_nombre.setText(i18n.t("label.name"));
        delegado.lbl_telefono.setText(i18n.t("label.phone"));
        delegado.lbl_email.setText(i18n.t("label.email"));
        delegado.lbl_categoria.setText(i18n.t("label.category"));
        delegado.chb_favorito.setText(i18n.t("label.favorite"));
        delegado.lbl_filtro.setText(i18n.t("label.search"));
        delegado.lbl_categoria_filtro.setText(i18n.t("label.categoryFilter"));
        delegado.lbl_idioma.setText(i18n.t("label.language"));
        delegado.txt_buscar.setToolTipText(i18n.t("placeholder.search"));
        delegado.txt_buscar.putClientProperty("JTextField.placeholderText", i18n.t("placeholder.search"));

        delegado.btn_add.setText(i18n.t("button.add"));
        delegado.btn_modificar.setText(i18n.t("button.update"));
        delegado.btn_eliminar.setText(i18n.t("button.delete"));
        delegado.btn_exportar.setText(i18n.t("button.export"));
        delegado.btn_importar_json.setText(i18n.t("button.importJson"));
        delegado.btn_exportar_json.setText(i18n.t("button.exportJson"));

        delegado.mnu_editar.setText(i18n.t("menu.edit"));
        delegado.mnu_eliminar.setText(i18n.t("menu.delete"));
        delegado.mnu_favorito.setText(i18n.t("menu.favorite"));
        delegado.mnu_exportar.setText(i18n.t("menu.exportVisible"));

        delegado.modeloTabla.setColumnIdentifiers(new Object[] {
            i18n.t("table.name"),
            i18n.t("table.phone"),
            i18n.t("table.email"),
            i18n.t("table.category"),
            i18n.t("table.favorite")
        });
        configurarColumnasTabla();

        actualizandoCombos = true;
        int idxCategoria = Math.max(0, delegado.cmb_categoria.getSelectedIndex());
        int idxFiltro = Math.max(0, delegado.cmb_filtro_categoria.getSelectedIndex());

        delegado.cmb_categoria.removeAllItems();
        delegado.cmb_categoria.addItem(i18n.t("cat.choose"));
        delegado.cmb_categoria.addItem(i18n.t("cat.family"));
        delegado.cmb_categoria.addItem(i18n.t("cat.friends"));
        delegado.cmb_categoria.addItem(i18n.t("cat.work"));

        delegado.cmb_filtro_categoria.removeAllItems();
        delegado.cmb_filtro_categoria.addItem(i18n.t("cat.all"));
        delegado.cmb_filtro_categoria.addItem(i18n.t("cat.family"));
        delegado.cmb_filtro_categoria.addItem(i18n.t("cat.friends"));
        delegado.cmb_filtro_categoria.addItem(i18n.t("cat.work"));

        delegado.cmb_categoria.setSelectedIndex(Math.min(idxCategoria, delegado.cmb_categoria.getItemCount() - 1));
        delegado.cmb_filtro_categoria.setSelectedIndex(Math.min(idxFiltro, delegado.cmb_filtro_categoria.getItemCount() - 1));
        actualizandoCombos = false;

        delegado.ttl_total.setText(i18n.t("stats.total"));
        delegado.ttl_favoritos.setText(i18n.t("stats.favorites"));
        delegado.ttl_familia.setText(i18n.t("stats.family"));
        delegado.ttl_amigos.setText(i18n.t("stats.friends"));
        delegado.ttl_trabajo.setText(i18n.t("stats.work"));

        refrescarTabla();
        actualizarEstadisticas();
        actualizarEstado(i18n.t("status.ready"));
    }

    private void validarContactoAsync(final int indexIgnorado, final Runnable onValid) {
        setUiBusy(true);
        notificarAsync(i18n.t("status.validating"));

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                synchronized (contactosLock) {
                    for (int i = 0; i < contactos.size(); i++) {
                        if (i == indexIgnorado) {
                            continue;
                        }
                        if (esDuplicado(contactos.get(i), nombres, telefono, email)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            protected void done() {
                boolean duplicado = false;
                try {
                    duplicado = get();
                } catch (Exception ex) {
                    duplicado = false;
                }

                final boolean duplicadoFinal = duplicado;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setUiBusy(false);
                        if (duplicadoFinal) {
                            JOptionPane.showMessageDialog(delegado, i18n.t("msg.duplicateContact"));
                            notificarAsync(i18n.t("status.duplicate"));
                            if (indexIgnorado >= 0) {
                                liberarEdicionSiCorresponde(indexIgnorado);
                            }
                            return;
                        }
                        onValid.run();
                    }
                });
            }
        };

        worker.execute();
    }

    private boolean esDuplicado(persona existente, String nombre, String telefono, String email) {
        if (existente == null) {
            return false;
        }
        String emailNuevo = normalizarEmail(email);
        String emailExistente = normalizarEmail(existente.getEmail());
        String telNuevo = normalizarTelefono(telefono);
        String telExistente = normalizarTelefono(existente.getTelefono());

        if (!emailNuevo.isEmpty() && emailNuevo.equals(emailExistente)) {
            return true;
        }
        return !telNuevo.isEmpty() && telNuevo.equals(telExistente);
    }

    private boolean bloquearEdicion(int index) {
        // Bloqueo de recurso para que solo una edicion ocurra a la vez.
        if (editLock.tryLock()) {
            editLockIndex = index;
            return true;
        }
        return editLockIndex == index;
    }

    private void liberarEdicionSiCorresponde(int index) {
        if (editLockIndex == index && editLock.isHeldByCurrentThread()) {
            editLockIndex = -1;
            editLock.unlock();
        }
    }

    private boolean esDuplicadoEnSets(String emailNorm, String telNorm, Set<String> emails, Set<String> telefonos) {
        if (!emailNorm.isEmpty() && emails.contains(emailNorm)) {
            return true;
        }
        return !telNorm.isEmpty() && telefonos.contains(telNorm);
    }

    private void registrarClaveContacto(persona contacto, Set<String> emails, Set<String> telefonos) {
        if (contacto == null) {
            return;
        }
        registrarClaveContacto(normalizarEmail(contacto.getEmail()), normalizarTelefono(contacto.getTelefono()), emails, telefonos);
    }

    private void registrarClaveContacto(String emailNorm, String telNorm, Set<String> emails, Set<String> telefonos) {
        if (!emailNorm.isEmpty()) {
            emails.add(emailNorm);
        }
        if (!telNorm.isEmpty()) {
            telefonos.add(telNorm);
        }
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizarTelefono(String telefono) {
        return telefono == null ? "" : telefono.trim();
    }

    private void setUiBusy(boolean busy) {
        int count = busy
            ? busyCount.incrementAndGet()
            : busyCount.updateAndGet(value -> value > 0 ? value - 1 : 0);
        final boolean uiBusy = count > 0;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                delegado.btn_add.setEnabled(!uiBusy);
                delegado.btn_modificar.setEnabled(!uiBusy);
                delegado.btn_eliminar.setEnabled(!uiBusy);
                delegado.btn_exportar.setEnabled(!uiBusy);
                delegado.btn_importar_json.setEnabled(!uiBusy);
                delegado.btn_exportar_json.setEnabled(!uiBusy);
                delegado.cmb_categoria.setEnabled(!uiBusy);
                delegado.cmb_filtro_categoria.setEnabled(!uiBusy);
                delegado.cmb_idioma.setEnabled(!uiBusy);
                delegado.txt_buscar.setEnabled(!uiBusy);
                delegado.txt_nombres.setEnabled(!uiBusy);
                delegado.txt_telefono.setEnabled(!uiBusy);
                delegado.txt_email.setEnabled(!uiBusy);
                delegado.chb_favorito.setEnabled(!uiBusy);
                delegado.pgb_carga.setIndeterminate(uiBusy);
                if (!uiBusy) {
                    delegado.pgb_carga.setValue(0);
                }
            }
        });
    }

    private void notificarAsync(final String mensaje) {
        // Notificacion en hilo dedicado; UI se actualiza via invokeLater.
        notificationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        actualizarEstado(mensaje);
                    }
                });
            }
        });
    }

    private void configurarColumnasTabla() {
        if (delegado.tbl_contactos.getColumnModel().getColumnCount() <= COL_FAV) {
            return;
        }

        delegado.tbl_contactos.getColumnModel().getColumn(COL_NOMBRE).setPreferredWidth(180);
        delegado.tbl_contactos.getColumnModel().getColumn(COL_TELEFONO).setPreferredWidth(120);
        delegado.tbl_contactos.getColumnModel().getColumn(COL_EMAIL).setPreferredWidth(340);
        delegado.tbl_contactos.getColumnModel().getColumn(COL_CATEGORIA).setPreferredWidth(120);
        delegado.tbl_contactos.getColumnModel().getColumn(COL_FAV).setMinWidth(56);
        delegado.tbl_contactos.getColumnModel().getColumn(COL_FAV).setMaxWidth(56);
        // no actions column

        delegado.tbl_contactos.getColumnModel().getColumn(COL_FAV).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean fav = value instanceof Boolean && ((Boolean) value).booleanValue();
                setHorizontalAlignment(SwingConstants.CENTER);
                setIcon(fav ? iconStarOn : iconStarOff);
                setText(getIcon() == null ? (fav ? i18n.t("csv.value.yes") : i18n.t("csv.value.no")) : "");
                setOpaque(true);
                setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return this;
            }
        });

        // actions column removed — editing/deleting handled via double-click and context menu
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
        } else if (source == delegado.btn_importar_json) {
            importarJson();
        } else if (source == delegado.btn_exportar_json) {
            exportarJson();
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
        if (e.getSource() == delegado.chb_favorito) {
            favorito = delegado.chb_favorito.isSelected();
            return;
        }

        if (e.getStateChange() != ItemEvent.SELECTED || actualizandoCombos) {
            return;
        }

        if (e.getSource() == delegado.cmb_categoria) {
            categoria = obtenerCodigoCategoriaFormulario();
        } else if (e.getSource() == delegado.cmb_filtro_categoria) {
            aplicarFiltro();
        } else if (e.getSource() == delegado.cmb_idioma) {
            aplicarIdioma(obtenerCodigoIdiomaSeleccionado());
        }
    }
}

