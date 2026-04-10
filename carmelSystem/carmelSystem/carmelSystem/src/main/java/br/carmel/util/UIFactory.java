package br.carmel.util;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;

/**
 * Fábrica de componentes com visual Windows XP clássico.
 */
public final class UIFactory {

    // ── Paleta XP ────────────────────────────────────────────────────────────
    public static final Color XP_BG         = new Color(236, 233, 216);
    public static final Color XP_PANEL_BG   = new Color(212, 208, 200);
    public static final Color XP_BTN_BG     = new Color(236, 233, 216);
    public static final Color XP_BTN_HOVER  = new Color(182, 189, 210);
    public static final Color XP_BTN_BORDER = new Color(112, 111, 145);
    public static final Color XP_FIELD_BG   = Color.WHITE;
    public static final Color XP_FIELD_FG   = Color.BLACK;
    public static final Color XP_TABLE_SEL  = new Color(49, 106, 197);
    public static final Color XP_MENU_BG    = new Color(58, 110, 165);
    public static final Color XP_MENU_BTN   = new Color(71, 128, 186);
    public static final Color XP_MENU_HOVER = new Color(255, 188, 72);

    public static final Font FONT_NORMAL = new Font("Tahoma", Font.PLAIN,  11);
    public static final Font FONT_BOLD   = new Font("Tahoma", Font.BOLD,   11);
    public static final Font FONT_TITLE  = new Font("Tahoma", Font.BOLD,   13);

    private UIFactory() {}

    // ── Campos ────────────────────────────────────────────────────────────────

    public static JTextField styledField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(XP_FIELD_BG);
        f.setForeground(XP_FIELD_FG);
        f.setFont(FONT_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(127, 157, 185)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        return f;
    }

    public static void styleSmallField(JTextField f) {
        f.setBackground(XP_FIELD_BG);
        f.setForeground(XP_FIELD_FG);
        f.setFont(FONT_NORMAL);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(127, 157, 185)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    }

    public static JPasswordField styledPasswordField(String text) {
        JPasswordField pf = new JPasswordField(text);
        pf.setBackground(XP_FIELD_BG);
        pf.setForeground(XP_FIELD_FG);
        pf.setFont(FONT_NORMAL);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(127, 157, 185)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        return pf;
    }

    public static JTextArea styledTextArea(int rows, int cols) {
        JTextArea ta = new JTextArea(rows, cols);
        ta.setBackground(XP_FIELD_BG);
        ta.setForeground(XP_FIELD_FG);
        ta.setFont(FONT_NORMAL);
        ta.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return ta;
    }

    // ── Labels ────────────────────────────────────────────────────────────────

    public static JLabel labelLight(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.BLACK);
        l.setFont(FONT_NORMAL);
        return l;
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    public static JButton bigMenuButton(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(XP_MENU_BTN);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Tahoma", Font.BOLD, 12));
        b.setPreferredSize(new Dimension(180, 46));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 80, 140)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(XP_MENU_HOVER); b.setForeground(Color.BLACK); }
            public void mouseExited(MouseEvent e)  { b.setBackground(XP_MENU_BTN);   b.setForeground(Color.WHITE); }
        });
        return b;
    }

    public static JButton bigActionButton(String text, ActionListener a) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setBackground(XP_BTN_BG);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(110, 26));
        b.setBorder(new CompoundBorder(
                new LineBorder(XP_BTN_BORDER, 1),
                new EmptyBorder(2, 8, 2, 8)));
        b.addActionListener(a);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(XP_BTN_HOVER); }
            public void mouseExited(MouseEvent e)  { b.setBackground(XP_BTN_BG); }
        });
        return b;
    }

    public static JButton bigSmallButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_NORMAL);
        b.setBackground(XP_BTN_BG);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(XP_BTN_BORDER, 1),
                new EmptyBorder(2, 8, 2, 8)));
        return b;
    }

    public static void styleActionButton(JButton b) {
        b.setBackground(XP_BTN_BG);
        b.setForeground(Color.BLACK);
        b.setFont(FONT_NORMAL);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(XP_BTN_BORDER, 1),
                new EmptyBorder(2, 8, 2, 8)));
    }

    // ── Tabela XP ─────────────────────────────────────────────────────────────

    public static JTable styledTable() {
        JTable t = new JTable();
        t.setFont(FONT_NORMAL);
        t.setBackground(Color.WHITE);
        t.setForeground(Color.BLACK);
        t.setGridColor(new Color(212, 208, 200));
        t.setSelectionBackground(XP_TABLE_SEL);
        t.setSelectionForeground(Color.WHITE);
        t.setRowHeight(20);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));

        DefaultTableCellRenderer hdr = new DefaultTableCellRenderer();
        hdr.setBackground(new Color(10, 36, 106));
        hdr.setForeground(Color.WHITE);
        hdr.setFont(FONT_BOLD);
        hdr.setHorizontalAlignment(SwingConstants.LEFT);
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(80, 110, 160)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        t.getTableHeader().setDefaultRenderer(hdr);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setBackground(new Color(10, 36, 106));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(FONT_BOLD);
        return t;
    }

    // ── GroupBox (borda com título) ────────────────────────────────────────────

    public static Border groupBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(172, 168, 153), 1),
                title,
                TitledBorder.LEFT, TitledBorder.TOP,
                FONT_BOLD, new Color(0, 84, 166));
    }

    // ── Barra de título XP (gradiente azul) ───────────────────────────────────

    public static JPanel xpTitleBar(String text) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 84, 166),
                        getWidth(), 0, new Color(166, 202, 240)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bar.setPreferredSize(new Dimension(0, 26));
        JLabel lbl = new JLabel("  " + text);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(FONT_TITLE);
        bar.add(lbl, BorderLayout.WEST);
        return bar;
    }

    // ── Cartão Home ───────────────────────────────────────────────────────────

    public static JPanel summaryCard(String title, String icon, ActionListener a) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(XP_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(172, 168, 153)),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        JLabel lbl = new JLabel(icon + "  " + title, SwingConstants.CENTER);
        lbl.setForeground(new Color(0, 84, 166));
        lbl.setFont(new Font("Tahoma", Font.BOLD, 14));
        card.add(lbl, BorderLayout.CENTER);
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bp.setOpaque(false);
        bp.add(bigActionButton("Abrir", a));
        card.add(bp, BorderLayout.SOUTH);
        return card;
    }
}