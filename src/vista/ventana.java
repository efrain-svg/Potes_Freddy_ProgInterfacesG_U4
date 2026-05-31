package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.extras.FlatSVGUtils;

import controlador.logica_ventana;
import vista.charts.BarChartPanel;
import vista.charts.PieChartPanel;
import vista.theme.SvgIconLoader;
import vista.theme.ThemeManager;

public class ventana extends JFrame {

    private static final int PADDING = 12;
    private static final int SECTION_GAP = 12;
    private static final int INNER_GAP = 8;
    private static final int CONTROL_HEIGHT = 28;
    private static final int INPUT_STANDARD_WIDTH = 200;
    private static final int INPUT_EMAIL_WIDTH = 200;
    private static final int ICON_BUTTON_SIZE = 16;
    private static final Color CARD_BORDER = new Color(229, 231, 235);

    public JPanel contentPane;
    public JTabbedPane tabs;

    public JTextField txt_nombres;
    public JTextField txt_telefono;
    public JTextField txt_email;
    public JTextField txt_buscar;
    public JCheckBox chb_favorito;
    public JComboBox<String> cmb_categoria;
    public JComboBox<String> cmb_filtro_categoria;
    public JComboBox<String> cmb_idioma;
    public JButton btn_add;
    public JButton btn_modificar;
    public JButton btn_eliminar;
    public JButton btn_exportar;
    public JButton btn_importar_json;
    public JButton btn_exportar_json;
    public JTable tbl_contactos;
    public DefaultTableModel modeloTabla;
    public JProgressBar pgb_carga;

    public JLabel lbl_total;
    public JLabel lbl_favoritos;
    public JLabel lbl_familia;
    public JLabel lbl_amigos;
    public JLabel lbl_trabajo;
    public JLabel lbl_actualizacion;

    public JLabel lbl_tab_contactos;
    public JLabel lbl_tab_estadisticas;
    public JLabel lbl_nombre;
    public JLabel lbl_telefono;
    public JLabel lbl_email;
    public JLabel lbl_categoria;
    public JLabel lbl_filtro;
    public JLabel lbl_categoria_filtro;
    public JLabel lbl_idioma;
    public JLabel lbl_estado;

    public JLabel ttl_total;
    public JLabel ttl_favoritos;
    public JLabel ttl_familia;
    public JLabel ttl_amigos;
    public JLabel ttl_trabajo;

    public BarChartPanel pnl_chart_barras;
    public PieChartPanel pnl_chart_pastel;

    public JPopupMenu menuContextual;
    public JMenuItem mnu_editar;
    public JMenuItem mnu_eliminar;
    public JMenuItem mnu_favorito;
    public JMenuItem mnu_exportar;

    public static void main(String[] args) {
        ThemeManager.setupLookAndFeel();

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ventana frame = new ventana();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public ventana() {
        setTitle("Gestion de Contactos");
        setIconImage(cargarIconoVentana());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1180, 740);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        contentPane.setBackground(ThemeManager.BACKGROUND);
        setContentPane(contentPane);

        tabs = new JTabbedPane();
        tabs.setFont(fontBase(13, Font.BOLD));
        tabs.setBackground(ThemeManager.BACKGROUND);
        contentPane.add(tabs, BorderLayout.CENTER);

        construirTabContactos();
        construirTabEstadisticas();

        new logica_ventana(this);
    }

    private void construirTabContactos() {
        JPanel panelContactos = new JPanel(new BorderLayout(SECTION_GAP, SECTION_GAP));
        panelContactos.setBackground(ThemeManager.BACKGROUND);
        panelContactos.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        tabs.addTab("Contactos", panelContactos);

        JPanel panelCabecera = crearTarjetaSeccion();
        panelCabecera.setLayout(new BorderLayout(16, 0));
        panelContactos.add(panelCabecera, BorderLayout.NORTH);

        JPanel panelTitulo = new JPanel(new BorderLayout(0, 4));
        panelTitulo.setOpaque(false);
        lbl_tab_contactos = crearTituloSeccion("Detalles");
        panelTitulo.add(lbl_tab_contactos, BorderLayout.NORTH);
        panelCabecera.add(panelTitulo, BorderLayout.WEST);

        JPanel panelIdioma = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelIdioma.setOpaque(false);
        lbl_idioma = crearLabel("Idioma");
        cmb_idioma = new JComboBox<String>(new String[] { "ES", "EN", "PT" });
        configurarCombo(cmb_idioma, 92);
        panelIdioma.add(lbl_idioma);
        panelIdioma.add(cmb_idioma);
        panelCabecera.add(panelIdioma, BorderLayout.EAST);

        JPanel panelContenido = new JPanel(new GridBagLayout());
        panelContenido.setOpaque(false);
        panelContenido.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panelContactos.add(panelContenido, BorderLayout.CENTER);

        GridBagConstraints gbcContenido = new GridBagConstraints();
        gbcContenido.gridx = 0;
        gbcContenido.weightx = 1.0;
        gbcContenido.fill = GridBagConstraints.HORIZONTAL;
        gbcContenido.insets = new Insets(0, 0, SECTION_GAP, 0);

        JPanel panelFormulario = crearTarjetaSeccion();
        panelFormulario.setLayout(new GridBagLayout());
        GridBagConstraints gbcFormulario = new GridBagConstraints();
        gbcFormulario.anchor = GridBagConstraints.WEST;
        gbcFormulario.fill = GridBagConstraints.HORIZONTAL;
        gbcFormulario.insets = new Insets(0, 0, INNER_GAP, INNER_GAP);

        lbl_nombre = crearLabel("Nombres");
        aplicarIconoLabel(lbl_nombre, "user.svg");
        txt_nombres = crearTextField(INPUT_STANDARD_WIDTH);
        addGbc(panelFormulario, lbl_nombre, gbcFormulario, 0, 0, 1, 1, 0.0);
        addGbc(panelFormulario, txt_nombres, gbcFormulario, 1, 0, 1, 1, 0.33);

        lbl_telefono = crearLabel("Teléfono");
        aplicarIconoLabel(lbl_telefono, "phone.svg");
        txt_telefono = crearTextField(INPUT_STANDARD_WIDTH);
        addGbc(panelFormulario, lbl_telefono, gbcFormulario, 2, 0, 1, 1, 0.0);
        addGbc(panelFormulario, txt_telefono, gbcFormulario, 3, 0, 1, 1, 0.33);

        lbl_email = crearLabel("Correo electrónico");
        aplicarIconoLabel(lbl_email, "mail.svg");
        txt_email = crearTextField(INPUT_EMAIL_WIDTH);
        addGbc(panelFormulario, lbl_email, gbcFormulario, 4, 0, 1, 1, 0.0);
        addGbc(panelFormulario, txt_email, gbcFormulario, 5, 0, 1, 1, 0.33);

        gbcFormulario.insets = new Insets(0, 0, 0, INNER_GAP);
        lbl_categoria = crearLabel("Categoria");
        cmb_categoria = new JComboBox<>();
        configurarCombo(cmb_categoria, INPUT_STANDARD_WIDTH);
        addGbc(panelFormulario, lbl_categoria, gbcFormulario, 0, 1, 1, 1, 0.0);
        addGbc(panelFormulario, cmb_categoria, gbcFormulario, 1, 1, 1, 1, 0.33);

        chb_favorito = new JCheckBox("Favorito");
        chb_favorito.setOpaque(false);
        chb_favorito.setFont(fontBase(12, Font.BOLD));
        chb_favorito.setForeground(ThemeManager.TEXT_SECONDARY);
        addGbc(panelFormulario, chb_favorito, gbcFormulario, 2, 1, 2, 1, 0.33);

        gbcContenido.gridy = 0;
        panelContenido.add(panelFormulario, gbcContenido);

        JPanel panelAcciones = crearTarjetaSeccion();
        panelAcciones.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panelAcciones.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        btn_add = crearBotonPrincipal("Nuevo");
        aplicarIconoBoton(btn_add, "add.svg");
        btn_modificar = crearBotonSecundario("Modificar");
        aplicarIconoBoton(btn_modificar, "edit.svg");
        btn_importar_json = crearBotonSecundario("Importar JSON");
        aplicarIconoBoton(btn_importar_json, "add.svg");
        btn_exportar = crearBotonSecundario("Exportar CSV");
        aplicarIconoBoton(btn_exportar, "export.svg");
        btn_exportar_json = crearBotonSecundario("Exportar JSON");
        aplicarIconoBoton(btn_exportar_json, "export.svg");
        btn_eliminar = crearBotonPeligro("Eliminar");
        aplicarIconoBoton(btn_eliminar, "delete-white.svg");
        panelAcciones.add(btn_add);
        panelAcciones.add(btn_modificar);
        panelAcciones.add(btn_importar_json);
        panelAcciones.add(btn_exportar);
        panelAcciones.add(btn_exportar_json);
        panelAcciones.add(btn_eliminar);

        gbcContenido.gridy = 1;
        panelContenido.add(panelAcciones, gbcContenido);

        JPanel panelFiltros = crearTarjetaSeccion();
        panelFiltros.setLayout(new GridBagLayout());
        GridBagConstraints gbcFiltros = new GridBagConstraints();
        gbcFiltros.anchor = GridBagConstraints.WEST;
        gbcFiltros.fill = GridBagConstraints.HORIZONTAL;
        gbcFiltros.insets = new Insets(0, 0, 0, INNER_GAP);

        lbl_filtro = crearLabel("");
        aplicarIconoLabel(lbl_filtro, "search.svg");
        txt_buscar = crearTextField(320);
        txt_buscar.putClientProperty("JTextField.placeholderText", "Buscar...");
        addGbc(panelFiltros, lbl_filtro, gbcFiltros, 0, 0, 1, 1, 0.0);
        addGbc(panelFiltros, txt_buscar, gbcFiltros, 1, 0, 1, 1, 0.7);

        lbl_categoria_filtro = crearLabel("Categoria");
        cmb_filtro_categoria = new JComboBox<>();
        configurarCombo(cmb_filtro_categoria, 180);
        addGbc(panelFiltros, lbl_categoria_filtro, gbcFiltros, 2, 0, 1, 1, 0.0);
        addGbc(panelFiltros, cmb_filtro_categoria, gbcFiltros, 3, 0, 1, 1, 0.3);

        gbcContenido.gridy = 2;
        panelContenido.add(panelFiltros, gbcContenido);

        JPanel panelTabla = crearTarjetaSeccion();
        panelTabla.setLayout(new BorderLayout());

        modeloTabla = new DefaultTableModel(new Object[] { "Nombre", "Telefono", "Email", "Categoria", "Fav" }, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) {
                    return Boolean.class;
                }
                return String.class;
            }
        };
        tbl_contactos = new JTable(modeloTabla);
        tbl_contactos.setRowHeight(34);
        tbl_contactos.setFont(fontBase(13, Font.PLAIN));
        tbl_contactos.getTableHeader().setReorderingAllowed(false);
        tbl_contactos.getTableHeader().setFont(fontBase(13, Font.BOLD));
        tbl_contactos.setFillsViewportHeight(true);
        tbl_contactos.setShowHorizontalLines(false);
        tbl_contactos.setShowVerticalLines(false);
        tbl_contactos.setIntercellSpacing(new Dimension(0, 8));

        JScrollPane scrollTabla = new JScrollPane(tbl_contactos);
        scrollTabla.setBorder(new LineBorder(ThemeManager.BORDER, 1, true));
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        gbcContenido.gridy = 3;
        gbcContenido.weighty = 1.0;
        gbcContenido.fill = GridBagConstraints.BOTH;
        panelContenido.add(panelTabla, gbcContenido);

        JPanel panelEstado = new JPanel(new BorderLayout(8, 0));
        panelEstado.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(ThemeManager.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        panelEstado.setBackground(ThemeManager.SURFACE);
        panelContactos.add(panelEstado, BorderLayout.SOUTH);

        lbl_estado = new JLabel("\u2022 Listo");
        lbl_estado.setForeground(ThemeManager.SUCCESS);
        lbl_estado.setFont(fontBase(12, Font.PLAIN));
        panelEstado.add(lbl_estado, BorderLayout.WEST);

        pgb_carga = new JProgressBar();
        pgb_carga.setStringPainted(false);
        pgb_carga.setForeground(ThemeManager.PRIMARY);
        pgb_carga.setPreferredSize(new Dimension(150, 8));
        panelEstado.add(pgb_carga, BorderLayout.EAST);

        menuContextual = new JPopupMenu();
        mnu_editar = new JMenuItem("Editar contacto");
        mnu_eliminar = new JMenuItem("Eliminar contacto");
        mnu_favorito = new JMenuItem("Alternar favorito");
        mnu_exportar = new JMenuItem("Exportar visibles");
        menuContextual.add(mnu_editar);
        menuContextual.add(mnu_eliminar);
        menuContextual.add(mnu_favorito);
        menuContextual.addSeparator();
        menuContextual.add(mnu_exportar);
    }

    private void construirTabEstadisticas() {
        JPanel panelEstadisticas = new JPanel(new BorderLayout(SECTION_GAP, SECTION_GAP));
        panelEstadisticas.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        panelEstadisticas.setBackground(ThemeManager.BACKGROUND);
        tabs.addTab("Estadísticas", panelEstadisticas);

        JPanel panelCabecera = crearTarjetaSeccion();
        panelCabecera.setLayout(new BorderLayout(16, 0));
        panelEstadisticas.add(panelCabecera, BorderLayout.NORTH);

        // single tab title already shows 'Estadísticas' — avoid duplicate large title here

        lbl_actualizacion = new JLabel("Ultima actualizacion:");
        lbl_actualizacion.setFont(fontBase(12, Font.PLAIN));
        lbl_actualizacion.setForeground(ThemeManager.TEXT_SECONDARY);
        JPanel panelActualizacion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panelActualizacion.setOpaque(false);
        panelActualizacion.add(lbl_actualizacion);
        panelCabecera.add(panelActualizacion, BorderLayout.EAST);

        JPanel panelContenido = new JPanel(new GridBagLayout());
        panelContenido.setOpaque(false);
        panelEstadisticas.add(panelContenido, BorderLayout.CENTER);

        GridBagConstraints gbcContenido = new GridBagConstraints();
        gbcContenido.gridx = 0;
        gbcContenido.weightx = 1.0;
        gbcContenido.fill = GridBagConstraints.HORIZONTAL;
        gbcContenido.insets = new Insets(0, 0, SECTION_GAP, 0);

        JPanel panelTarjetas = new JPanel(new GridLayout(1, 5, 16, 0));
        panelTarjetas.setOpaque(false);

        ttl_total = new JLabel("Total Cont.", SwingConstants.CENTER);
        lbl_total = new JLabel("0", SwingConstants.CENTER);
        panelTarjetas.add(crearTarjeta(ttl_total, lbl_total));

        ttl_favoritos = new JLabel("Favoritos", SwingConstants.CENTER);
        lbl_favoritos = new JLabel("0", SwingConstants.CENTER);
        panelTarjetas.add(crearTarjeta(ttl_favoritos, lbl_favoritos));

        ttl_familia = new JLabel("Familia", SwingConstants.CENTER);
        lbl_familia = new JLabel("0", SwingConstants.CENTER);
        panelTarjetas.add(crearTarjeta(ttl_familia, lbl_familia));

        ttl_amigos = new JLabel("Amigos", SwingConstants.CENTER);
        lbl_amigos = new JLabel("0", SwingConstants.CENTER);
        panelTarjetas.add(crearTarjeta(ttl_amigos, lbl_amigos));

        ttl_trabajo = new JLabel("Trabajo", SwingConstants.CENTER);
        lbl_trabajo = new JLabel("0", SwingConstants.CENTER);
        panelTarjetas.add(crearTarjeta(ttl_trabajo, lbl_trabajo));

        gbcContenido.gridy = 0;
        panelContenido.add(panelTarjetas, gbcContenido);

        JPanel panelGraficas = new JPanel(new GridLayout(1, 2, 16, 0));
        panelGraficas.setOpaque(false);
        gbcContenido.gridy = 1;
        gbcContenido.weighty = 1.0;
        gbcContenido.fill = GridBagConstraints.BOTH;
        panelContenido.add(panelGraficas, gbcContenido);

        JPanel tarjetaBarras = crearTarjetaSeccion();
        tarjetaBarras.setLayout(new BorderLayout(8, 8));
        pnl_chart_barras = new BarChartPanel();
        pnl_chart_barras.setPreferredSize(new Dimension(420, 220));
        tarjetaBarras.add(pnl_chart_barras, BorderLayout.CENTER);
        panelGraficas.add(tarjetaBarras);

        JPanel tarjetaPastel = crearTarjetaSeccion();
        tarjetaPastel.setLayout(new BorderLayout(8, 8));
        pnl_chart_pastel = new PieChartPanel();
        pnl_chart_pastel.setPreferredSize(new Dimension(420, 220));
        tarjetaPastel.add(pnl_chart_pastel, BorderLayout.CENTER);
        panelGraficas.add(tarjetaPastel);
    }

    private JPanel crearTarjeta(JLabel titulo, JLabel valor) {
        JPanel tarjeta = new JPanel();
        tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
        tarjeta.setBackground(ThemeManager.SURFACE);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(ThemeManager.BORDER, 1, true),
            BorderFactory.createEmptyBorder(12, 10, 12, 10)));

        titulo.setAlignmentX(CENTER_ALIGNMENT);
        titulo.setFont(fontBase(14, Font.BOLD));
        titulo.setForeground(ThemeManager.TEXT_SECONDARY);

        valor.setAlignmentX(CENTER_ALIGNMENT);
        valor.setFont(fontBase(28, Font.BOLD));
        valor.setForeground(ThemeManager.PRIMARY);

        tarjeta.add(Box.createVerticalGlue());
        tarjeta.add(titulo);
        tarjeta.add(Box.createVerticalStrut(8));
        tarjeta.add(valor);
        tarjeta.add(Box.createVerticalGlue());
        return tarjeta;
    }

    private JLabel crearLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(new Color(107, 114, 128));
        label.setFont(fontBase(12, Font.BOLD));
        return label;
    }

    private JLabel crearTituloSeccion(String texto) {
        JLabel label = new JLabel(texto);
        label.setForeground(ThemeManager.TEXT_PRIMARY);
        label.setFont(fontBase(20, Font.BOLD));
        return label;
    }

    private JPanel crearTarjetaSeccion() {
        JPanel panel = new JPanel();
        panel.setBackground(ThemeManager.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        return panel;
    }

    private JTextField crearTextField(int width) {
        JTextField text = new JTextField();
        text.setPreferredSize(new Dimension(width, CONTROL_HEIGHT));
        text.setFont(fontBase(13, Font.PLAIN));
        return text;
    }

    private JButton crearBotonPrincipal(String texto) {
        return crearBotonBase(texto, ThemeManager.PRIMARY, Color.WHITE, ThemeManager.PRIMARY);
    }

    private JButton crearBotonSecundario(String texto) {
        return crearBotonBase(texto, new Color(243, 244, 246), ThemeManager.TEXT_PRIMARY, new Color(209, 213, 219));
    }

    private JButton crearBotonPeligro(String texto) {
        return crearBotonBase(texto, new Color(220, 38, 38), Color.WHITE, new Color(185, 28, 28));
    }

    private JButton crearBotonBase(String texto, Color fondo, Color frente, Color borde) {
        JButton boton = new JButton(texto);
        boton.putClientProperty("JButton.buttonType", "roundRect");
        boton.setBackground(fondo);
        boton.setForeground(frente);
        boton.setFocusPainted(false);
        boton.setFont(fontBase(12, Font.BOLD));
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(borde, 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        boton.setMargin(new Insets(6, 12, 6, 12));
        return boton;
    }

    private void aplicarIconoBoton(JButton boton, String iconName) {
        boton.setIcon(SvgIconLoader.load(iconName, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
        boton.setIconTextGap(8);
        boton.setVerticalAlignment(SwingConstants.CENTER);
        boton.setHorizontalAlignment(SwingConstants.CENTER);
        boton.setVerticalTextPosition(SwingConstants.CENTER);
        boton.setHorizontalTextPosition(SwingConstants.RIGHT);
    }

    private void aplicarIconoLabel(JLabel label, String iconName) {
        // Asegura que no queden iconos duplicados en la etiqueta.
        label.setIcon(null);
        label.setDisabledIcon(null);
        javax.swing.Icon icon = SvgIconLoader.load(iconName, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
        if (icon != null) {
            label.setIcon(icon);
            label.setIconTextGap(6);
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setHorizontalTextPosition(SwingConstants.RIGHT);
        }
    }

    private void configurarCombo(JComboBox<String> combo, int width) {
        combo.setPreferredSize(new Dimension(width, CONTROL_HEIGHT));
        combo.setFont(fontBase(12, Font.PLAIN));
    }

    private Font fontBase(int size, int style) {
        Font fuenteSistema = UIManager.getFont("Label.font");
        if (fuenteSistema == null) {
            return new Font("Segoe UI", style, size);
        }
        return fuenteSistema.deriveFont(style, (float) size);
    }

    private void addGbc(JPanel panel, java.awt.Component comp, GridBagConstraints gbc, int x, int y, int w, int h, double wx) {
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.weightx = wx;
        panel.add(comp, gbc);
    }

    private Image cargarIconoVentana() {
        URL svgUrl = ventana.class.getResource("/assets/icons/contacts.svg");
        if (svgUrl == null) {
            return null;
        }
        Image base = FlatSVGUtils.svg2image(svgUrl, 1f);
        if (base == null) {
            return null;
        }
        return base.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
    }
}
