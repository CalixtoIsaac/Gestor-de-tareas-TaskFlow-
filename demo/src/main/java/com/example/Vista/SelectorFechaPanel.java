package com.example.Vista;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Componente compuesto para capturar la fecha de entrega de una tarea.
 * Permite dos modalidades de entrada, ambas manteniendo el mismo formato final:
 *   1) Escritura manual en un campo de texto, validada estrictamente con el
 *      formato yyyy-MM-dd (ej. 2025-12-31). Cualquier otro formato, como
 *      2025/12/31, es rechazado.
 *   2) Selección visual mediante un mini-calendario emergente que se abre
 *      al presionar el botón "Elegir Fecha".
 *
 * La validación real (aceptar/rechazar y avisar al usuario) la realiza el
 * Controlador llamando a obtenerFechaValidada(), manteniendo la separación MVC.
 */
public class SelectorFechaPanel extends JPanel {

    public static final String FORMATO_TEXTO = "yyyy-MM-dd";
    private static final DateTimeFormatter FORMATEADOR =
            DateTimeFormatter.ofPattern(FORMATO_TEXTO).withResolverStyle(ResolverStyle.STRICT);

    private final JTextField txtFecha;
    private final JButton btnCalendario;
    private YearMonth mesVisible;

    public SelectorFechaPanel() {
        super(new BorderLayout(4, 0));
        setOpaque(false);

        txtFecha = new JTextField();
        txtFecha.setToolTipText("Formato requerido: " + FORMATO_TEXTO + " (ejemplo: 2025-12-31)");
        txtFecha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        btnCalendario = new JButton("Elegir Fecha");
        btnCalendario.setFocusPainted(false);
        btnCalendario.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCalendario.addActionListener(e -> mostrarCalendario());

        add(txtFecha, BorderLayout.CENTER);
        add(btnCalendario, BorderLayout.EAST);
    }

    /** Formato de texto exigido, para mostrarlo como guía en la interfaz. */
    public String getFormatoTexto() {
        return FORMATO_TEXTO;
    }

    /** Texto crudo actualmente escrito en el campo (sin validar). */
    public String getTextoActual() {
        return txtFecha.getText().trim();
    }

    /** Coloca una fecha en el campo de texto con el formato exigido. */
    public void setFecha(LocalDate fecha) {
        txtFecha.setText(fecha.format(FORMATEADOR));
    }

    /** Limpia el campo (equivalente a no especificar fecha de entrega). */
    public void limpiar() {
        txtFecha.setText("");
    }

    /**
     * Valida el texto ingresado (manual o proveniente del calendario) con
     * formato estricto yyyy-MM-dd.
     *
     * @return la fecha ya validada, o null si el campo está vacío (el
     *         llamador puede interpretar null como "usar la fecha actual").
     * @throws DateTimeParseException si el texto no está vacío pero no
     *         cumple estrictamente el formato yyyy-MM-dd.
     */
    public LocalDate obtenerFechaValidada() {
        String texto = getTextoActual();
        if (texto.isEmpty()) {
            return null;
        }
        return LocalDate.parse(texto, FORMATEADOR);
    }

    private void mostrarCalendario() {
        LocalDate base;
        try {
            LocalDate actual = obtenerFechaValidada();
            base = (actual != null) ? actual : LocalDate.now();
        } catch (DateTimeParseException ex) {
            base = LocalDate.now();
        }
        mesVisible = YearMonth.from(base);

        JPopupMenu popup = new JPopupMenu();
        popup.add(construirPanelCalendario(popup));
        popup.show(btnCalendario, 0, btnCalendario.getHeight());
    }

    private JPanel construirPanelCalendario(JPopupMenu popup) {
        JPanel contenedor = new JPanel(new BorderLayout(4, 4));
        contenedor.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton btnAnterior = new JButton("<");
        JButton btnSiguiente = new JButton(">");
        JLabel lblMes = new JLabel("", SwingConstants.CENTER);
        lblMes.setFont(lblMes.getFont().deriveFont(Font.BOLD));

        JPanel diasPanel = new JPanel(new GridLayout(0, 7, 2, 2));

        Runnable[] refrescar = new Runnable[1];
        refrescar[0] = () -> {
            String nombreMes = mesVisible.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            nombreMes = Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1);
            lblMes.setText(nombreMes + " " + mesVisible.getYear());

            diasPanel.removeAll();
            for (String d : new String[]{"Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do"}) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
                diasPanel.add(lbl);
            }

            LocalDate primerDia = mesVisible.atDay(1);
            int espaciosVacios = primerDia.getDayOfWeek().getValue() - 1; // Lunes = 1 ... Domingo = 7
            for (int i = 0; i < espaciosVacios; i++) {
                diasPanel.add(new JLabel(""));
            }

            int totalDias = mesVisible.lengthOfMonth();
            for (int dia = 1; dia <= totalDias; dia++) {
                LocalDate fechaDelBoton = mesVisible.atDay(dia);
                JButton btnDia = new JButton(String.valueOf(dia));
                btnDia.setMargin(new Insets(2, 2, 2, 2));
                btnDia.setFocusPainted(false);
                btnDia.setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (fechaDelBoton.equals(LocalDate.now())) {
                    btnDia.setForeground(new Color(37, 99, 235));
                }
                btnDia.addActionListener(ev -> {
                    setFecha(fechaDelBoton);
                    popup.setVisible(false);
                });
                diasPanel.add(btnDia);
            }

            diasPanel.revalidate();
            diasPanel.repaint();
        };

        btnAnterior.addActionListener(e -> { mesVisible = mesVisible.minusMonths(1); refrescar[0].run(); });
        btnSiguiente.addActionListener(e -> { mesVisible = mesVisible.plusMonths(1); refrescar[0].run(); });

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.add(btnAnterior, BorderLayout.WEST);
        encabezado.add(lblMes, BorderLayout.CENTER);
        encabezado.add(btnSiguiente, BorderLayout.EAST);

        refrescar[0].run();

        contenedor.add(encabezado, BorderLayout.NORTH);
        contenedor.add(diasPanel, BorderLayout.CENTER);
        return contenedor;
    }
}