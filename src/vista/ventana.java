package vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
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
import javax.swing.table.DefaultTableModel;

import controlador.logica_ventana;

public class ventana extends JFrame {

    public JPanel contentPane;
    public JTabbedPane tabs;

    public JTextField txt_nombres;
    public JTextField txt_telefono;
    public JTextField txt_email;
    public JTextField txt_buscar;
    public JCheckBox chb_favorito;
    public JComboBox<String> cmb_categoria;
    public JButton btn_add;
    public JButton btn_modificar;
    public JButton btn_eliminar;
    public JButton btn_exportar;
    public JTable tbl_contactos;
    public DefaultTableModel modeloTabla;
    public JProgressBar pgb_carga;

    public JLabel lbl_total;
    public JLabel lbl_favoritos;
    public JLabel lbl_familia;
    public JLabel lbl_amigos;
    public JLabel lbl_trabajo;

    public JPopupMenu menuContextual;
    public JMenuItem mnu_editar;
    public JMenuItem mnu_eliminar;
    public JMenuItem mnu_favorito;
    public JMenuItem mnu_exportar;

    public static void main(String[] args) {
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
        setTitle("Gestion de contactos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1040, 720);
        setLocationRelativeTo(null);

        contentPane = new JPanel(new BorderLayout(8, 8));
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setContentPane(contentPane);

        tabs = new JTabbedPane();
        contentPane.add(tabs, BorderLayout.CENTER);

        construirTabContactos();
        construirTabEstadisticas();

        new logica_ventana(this);
    }

    private void construirTabContactos() {
        JPanel panelContactos = new JPanel(new BorderLayout(10, 10));
        panelContactos.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tabs.addTab("Contactos", panelContactos);

        JPanel panelFormulario = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panelContactos.add(panelFormulario, BorderLayout.NORTH);

        JLabel lblNombre = new JLabel("Nombres:");
        lblNombre.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelFormulario.add(lblNombre);

        txt_nombres = new JTextField();
        txt_nombres.setPreferredSize(new Dimension(180, 28));
        panelFormulario.add(txt_nombres);

        JLabel lblTelefono = new JLabel("Telefono:");
        lblTelefono.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelFormulario.add(lblTelefono);

        txt_telefono = new JTextField();
        txt_telefono.setPreferredSize(new Dimension(140, 28));
        panelFormulario.add(txt_telefono);

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(new Font("Tahoma", Font.BOLD, 13));
        panelFormulario.add(lblEmail);

        txt_email = new JTextField();
        txt_email.setPreferredSize(new Dimension(210, 28));
        panelFormulario.add(txt_email);

        chb_favorito = new JCheckBox("Favorito");
        panelFormulario.add(chb_favorito);

        cmb_categoria = new JComboBox<String>();
        cmb_categoria.setPreferredSize(new Dimension(140, 28));
        cmb_categoria.addItem("Elija una Categoria");
        cmb_categoria.addItem("Familia");
        cmb_categoria.addItem("Amigos");
        cmb_categoria.addItem("Trabajo");
        panelFormulario.add(cmb_categoria);

        btn_add = new JButton("Agregar");
        panelFormulario.add(btn_add);

        btn_modificar = new JButton("Modificar");
        panelFormulario.add(btn_modificar);

        btn_eliminar = new JButton("Eliminar");
        panelFormulario.add(btn_eliminar);

        btn_exportar = new JButton("Exportar CSV");
        panelFormulario.add(btn_exportar);

        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelContactos.add(panelCentro, BorderLayout.CENTER);

        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelCentro.add(panelBuscar, BorderLayout.NORTH);
        panelBuscar.add(new JLabel("Filtro por nombre/telefono/email:"));
        txt_buscar = new JTextField();
        txt_buscar.setPreferredSize(new Dimension(320, 28));
        panelBuscar.add(txt_buscar);

        modeloTabla = new DefaultTableModel(new Object[] { "Nombre", "Telefono", "Email", "Categoria", "Favorito" }, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tbl_contactos = new JTable(modeloTabla);
        tbl_contactos.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollTabla = new JScrollPane(tbl_contactos);
        panelCentro.add(scrollTabla, BorderLayout.CENTER);

        pgb_carga = new JProgressBar();
        pgb_carga.setStringPainted(true);
        pgb_carga.setString("Listo");
        panelContactos.add(pgb_carga, BorderLayout.SOUTH);

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
        JPanel panelEstadisticas = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        tabs.addTab("Estadisticas", panelEstadisticas);

        lbl_total = crearTarjeta(panelEstadisticas, "Total de contactos", "0");
        lbl_favoritos = crearTarjeta(panelEstadisticas, "Favoritos", "0");
        lbl_familia = crearTarjeta(panelEstadisticas, "Familia", "0");
        lbl_amigos = crearTarjeta(panelEstadisticas, "Amigos", "0");
        lbl_trabajo = crearTarjeta(panelEstadisticas, "Trabajo", "0");
    }

    private JLabel crearTarjeta(JPanel contenedor, String titulo, String valor) {
        JPanel tarjeta = new JPanel(new BorderLayout(4, 4));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tarjeta.setPreferredSize(new Dimension(170, 90));

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Tahoma", Font.BOLD, 12));
        tarjeta.add(lblTitulo, BorderLayout.NORTH);

        JLabel lblValor = new JLabel(valor, SwingConstants.CENTER);
        lblValor.setFont(new Font("Tahoma", Font.BOLD, 28));
        tarjeta.add(lblValor, BorderLayout.CENTER);

        contenedor.add(tarjeta);
        return lblValor;
    }
}
