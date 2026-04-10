package br.carmel.ui.panels;


import br.carmel.model.Produto;
import br.carmel.util.UIFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Relatório de Estoque - lista todos os produtos com quantidades e valores.
 */
public class RelatorioEstoquePanel extends BackgroundPanel {

    private final EntityManagerFactory emf;
    private DefaultTableModel tableModel;
    private JLabel lblTotalProdutos;
    private JLabel lblTotalItens;
    private JLabel lblValorEstoque;
    private JLabel lblEstoqueBaixo;

    public RelatorioEstoquePanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());


        build();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Relatório de Estoque"), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setBackground(UIFactory.XP_BG);
        centro.setBorder(new EmptyBorder(8, 10, 8, 10));

        centro.add(buildResumoPanel(), BorderLayout.NORTH);
        centro.add(buildTabelaPanel(), BorderLayout.CENTER);
        centro.add(buildBotoesPanel(), BorderLayout.SOUTH);

        add(centro, BorderLayout.CENTER);
    }

    // ── Resumo ────────────────────────────────────────────────────────────────

    private JPanel buildResumoPanel() {
        JPanel p = new JPanel(new GridLayout(1, 4, 10, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(UIFactory.groupBorder("Resumo do Estoque"));

        lblTotalProdutos = cardItem(p, "Total de Produtos",   "0", new Color(0, 84, 166));
        lblTotalItens    = cardItem(p, "Total de Itens",      "0", new Color(0, 120, 0));
        lblValorEstoque  = cardItem(p, "Valor em Estoque",    "R$ 0,00", new Color(0, 84, 166));
        lblEstoqueBaixo  = cardItem(p, "Estoque Zerado/Baixo","0", new Color(180, 0, 0));

        return p;
    }

    private JLabel cardItem(JPanel parent, String titulo, String valor, Color cor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(UIFactory.XP_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(172, 168, 153)),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Tahoma", Font.PLAIN, 10));
        lblTit.setForeground(new Color(80, 80, 80));

        JLabel lblVal = new JLabel(valor, SwingConstants.CENTER);
        lblVal.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblVal.setForeground(cor);

        card.add(lblTit);
        card.add(lblVal);
        parent.add(card);
        return lblVal;
    }

    // ── Tabela ────────────────────────────────────────────────────────────────

    private JPanel buildTabelaPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);

        // Filtro de estoque baixo
        JPanel filtroBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filtroBar.setBackground(UIFactory.XP_BG);
        JCheckBox chkBaixo = new JCheckBox("Mostrar apenas estoque zerado ou baixo (≤ 5 un.)");
        chkBaixo.setBackground(UIFactory.XP_BG);
        chkBaixo.setFont(UIFactory.FONT_NORMAL);
        chkBaixo.addActionListener(e -> carregarDados(chkBaixo.isSelected()));
        filtroBar.add(chkBaixo);
        p.add(filtroBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Nome", "Estoque", "Preço Venda", "Preço Custo", "Preço Médio", "Margem %", "Valor Total Estoque"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = UIFactory.styledTable();
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(85);
        table.getColumnModel().getColumn(4).setPreferredWidth(85);
        table.getColumnModel().getColumn(5).setPreferredWidth(85);
        table.getColumnModel().getColumn(6).setPreferredWidth(65);
        table.getColumnModel().getColumn(7).setPreferredWidth(110);

        // Renderizador colorido por estoque
        table.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 5, 1, 5));

            if (isSelected) {
                cell.setBackground(UIFactory.XP_TABLE_SEL);
                cell.setForeground(Color.WHITE);
            } else {
                // Pega valor de estoque da linha
                Object estoqueObj = tableModel.getValueAt(row, 2);
                int est = 0;
                try { est = Integer.parseInt(estoqueObj.toString()); } catch (Exception ignored) {}

                if (est <= 0)
                    cell.setBackground(new Color(255, 220, 220)); // vermelho claro - zerado
                else if (est <= 5)
                    cell.setBackground(new Color(255, 245, 200)); // amarelo - baixo
                else
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 252));

                // Cor do texto por coluna
                if (col == 2) { // estoque
                    cell.setFont(UIFactory.FONT_BOLD);
                    cell.setForeground(est <= 0 ? new Color(180, 0, 0) : est <= 5 ? new Color(140, 100, 0) : new Color(0, 110, 0));
                    cell.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 5) { // preço médio — destaca em azul
                    cell.setForeground(new Color(0, 84, 166));
                    cell.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 6) { // margem
                    cell.setFont(UIFactory.FONT_BOLD);
                    try {
                        double m = Double.parseDouble(value.toString().replace("%", "").replace(",", "."));
                        cell.setForeground(m >= 0 ? new Color(0, 110, 0) : new Color(180, 0, 0));
                    } catch (Exception ignored) { cell.setForeground(Color.GRAY); }
                    cell.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 3 || col == 4 || col == 7) {
                    cell.setHorizontalAlignment(SwingConstants.RIGHT);
                    cell.setForeground(Color.BLACK);
                } else {
                    cell.setForeground(Color.BLACK);
                }
            }
            return cell;
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));

        // Legenda
        JPanel legenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legenda.setBackground(UIFactory.XP_BG);
        legenda.add(legendaItem(new Color(255, 220, 220), "Estoque zerado"));
        legenda.add(legendaItem(new Color(255, 245, 200), "Estoque baixo (≤ 5)"));
        legenda.add(legendaItem(Color.WHITE, "Estoque normal"));

        p.add(scroll, BorderLayout.CENTER);
        p.add(legenda, BorderLayout.SOUTH);

        return p;
    }

    private JPanel legendaItem(Color cor, String texto) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel box = new JLabel("  ");
        box.setOpaque(true);
        box.setBackground(cor);
        box.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("Tahoma", Font.PLAIN, 10));
        item.add(box);
        item.add(lbl);
        return item;
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    private JPanel buildBotoesPanel() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        JButton btnAtualizar = UIFactory.bigActionButton("↻ Atualizar", e -> carregarDados(false));

        JButton btnImprimir = UIFactory.bigActionButton("🖨 Imprimir", e -> imprimir());
        btnImprimir.setBackground(new Color(0, 84, 166));
        btnImprimir.setForeground(Color.WHITE);

        bar.add(btnAtualizar);
        bar.add(btnImprimir);
        return bar;
    }

    // ── Carga de dados ────────────────────────────────────────────────────────

    public void carregarDados(boolean apenasEstoqueBaixo) {
        tableModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            String jpql = apenasEstoqueBaixo
                    ? "SELECT p FROM Produto p WHERE p.estoque <= 5 ORDER BY p.estoque ASC, p.nome ASC"
                    : "SELECT p FROM Produto p ORDER BY p.nome ASC";
            List<Produto> list = em.createQuery(jpql, Produto.class).getResultList();

            int totalItens  = 0;
            BigDecimal valorTotal = BigDecimal.ZERO;
            int baixo = 0;

            for (Produto p : list) {
                int est = p.getEstoque() != null ? p.getEstoque() : 0;
                totalItens += est;
                if (est <= 5) baixo++;

                // Margem
                String margem = "—";
                if (p.getPrecoCusto() != null && p.getPrecoCusto().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal m = p.getValor().subtract(p.getPrecoCusto())
                            .divide(p.getPrecoCusto(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    margem = String.format("%.1f%%", m);
                }

                // Valor total em estoque — usa preço médio se disponível, senão custo, senão venda
                BigDecimal base = p.getPrecoMedio() != null ? p.getPrecoMedio()
                        : p.getPrecoCusto() != null ? p.getPrecoCusto() : p.getValor();
                BigDecimal vEstoque = base.multiply(BigDecimal.valueOf(est));
                valorTotal = valorTotal.add(vEstoque);

                String custoStr = p.getPrecoCusto() != null
                        ? String.format("R$ %.2f", p.getPrecoCusto()) : "—";
                String medioStr = p.getPrecoMedio() != null
                        ? String.format("R$ %.2f", p.getPrecoMedio()) : "—";

                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getNome(),
                        est,
                        String.format("R$ %.2f", p.getValor()),
                        custoStr,
                        medioStr,
                        margem,
                        String.format("R$ %.2f", vEstoque)
                });
            }

            // Atualiza resumo
            lblTotalProdutos.setText(String.valueOf(list.size()));
            lblTotalItens.setText(String.valueOf(totalItens));
            lblValorEstoque.setText(String.format("R$ %.2f", valorTotal));
            lblEstoqueBaixo.setText(String.valueOf(baixo));

        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (em != null) em.close(); }
    }

    // ── Impressão ─────────────────────────────────────────────────────────────

    private void imprimir() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Carregue os dados antes de imprimir.",
                    "Atenção", JOptionPane.WARNING_MESSAGE); return;
        }

        JPanel pp = buildPrintPanel();
        pp.setSize(pp.getPreferredSize());
        pp.doLayout();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Relatório de Estoque");
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double scale = Math.min(pf.getImageableWidth() / pp.getWidth(),
                    pf.getImageableHeight() / pp.getHeight());
            if (scale < 1.0) g2.scale(scale, scale);
            pp.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao imprimir: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildPrintPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 28, 20, 28));
        p.setPreferredSize(new Dimension(700, 80 + tableModel.getRowCount() * 18 + 120));

        Font fTitle  = new Font("Tahoma", Font.BOLD, 14);
        Font fBold   = new Font("Tahoma", Font.BOLD, 10);
        Font fNormal = new Font("Tahoma", Font.PLAIN,  9);

        addPrint(p, "CARMEL SISTEMA DE GESTÃO", fTitle, new Color(0, 60, 140));
        addPrint(p, "RELATÓRIO DE ESTOQUE", new Font("Tahoma", Font.BOLD, 12), Color.BLACK);
        addPrint(p, "Gerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                fNormal, Color.GRAY);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(sep());

        // Resumo
        JPanel resumo = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 4));
        resumo.setOpaque(false);
        resumo.add(resumoLabel("Produtos: " + lblTotalProdutos.getText(), fBold));
        resumo.add(resumoLabel("Itens: " + lblTotalItens.getText(), fBold));
        resumo.add(resumoLabel("Valor: " + lblValorEstoque.getText(), fBold));
        resumo.add(resumoLabel("Estoque baixo/zerado: " + lblEstoqueBaixo.getText(), fBold));
        resumo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.add(resumo);
        p.add(sep());
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        // Cabeçalho tabela
        JPanel header = new JPanel(new GridLayout(1, 7));
        header.setBackground(new Color(10, 36, 106));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        for (String h : new String[]{"Nome", "Estoque", "Venda", "Custo", "Médio", "Margem", "Val. Estoque"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            JPanel row = new JPanel(new GridLayout(1, 7));
            row.setBackground(i % 2 == 0 ? Color.WHITE : new Color(245, 245, 250));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            for (int col : new int[]{1, 2, 3, 4, 5, 6, 7}) {
                Object v = tableModel.getValueAt(i, col);
                JLabel l = new JLabel("  " + (v != null ? v : "")); l.setFont(fNormal); row.add(l);
            }
            p.add(row);
        }

        return p;
    }

    private void addPrint(JPanel p, String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private JLabel resumoLabel(String text, Font font) {
        JLabel l = new JLabel(text); l.setFont(font); return l;
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(180, 180, 180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }
}