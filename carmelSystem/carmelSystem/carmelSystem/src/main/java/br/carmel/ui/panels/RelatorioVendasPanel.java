package br.carmel.ui.panels;


import br.carmel.model.Pagamento;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Painel de Relatório de Vendas.
 * Filtra por período e exibe total geral + lista de vendas.
 */
public class RelatorioVendasPanel extends JPanel {

    private final EntityManagerFactory emf;
    private static final DateTimeFormatter FMT_IN  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_OUT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Campos de filtro
    private JTextField tfDataDe;
    private JTextField tfDataAte;

    // Resultados
    private DefaultTableModel tableModel;
    private JLabel lblTotalGeral;
    private JLabel lblQtdVendas;
    private JLabel lblPeriodo;

    public RelatorioVendasPanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);


        build();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Relatório de Vendas"), BorderLayout.NORTH);
        add(buildFiltroPanel(), BorderLayout.NORTH); // substitui via wrapper abaixo

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIFactory.XP_BG);
        wrapper.add(UIFactory.xpTitleBar("Relatório de Vendas"), BorderLayout.NORTH);
        wrapper.add(buildFiltroPanel(), BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        removeAll();
        add(wrapper, BorderLayout.NORTH);
        add(buildResultadoPanel(), BorderLayout.CENTER);
    }

    // ── Painel de filtro ──────────────────────────────────────────────────────

    private JPanel buildFiltroPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Título
        add(UIFactory.xpTitleBar("Relatório de Vendas"), BorderLayout.NORTH);

        // Filtros
        JPanel filtro = new JPanel(new GridBagLayout());
        filtro.setBackground(UIFactory.XP_PANEL_BG);
        filtro.setBorder(UIFactory.groupBorder("Período"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        filtro.add(UIFactory.labelLight("Data inicial (dd/MM/yyyy):"), gc);
        tfDataDe = UIFactory.styledField("");
        tfDataDe.setPreferredSize(new Dimension(120, 24));
        tfDataDe.setToolTipText("Ex: 01/03/2026");
        gc.gridx = 1; gc.weightx = 0.3;
        filtro.add(tfDataDe, gc);

        gc.gridx = 2; gc.weightx = 0;
        filtro.add(UIFactory.labelLight("Data final (dd/MM/yyyy):"), gc);
        tfDataAte = UIFactory.styledField("");
        tfDataAte.setPreferredSize(new Dimension(120, 24));
        tfDataAte.setToolTipText("Ex: 31/03/2026");
        gc.gridx = 3; gc.weightx = 0.3;
        filtro.add(tfDataAte, gc);

        // Botões
        JButton btnGerar   = UIFactory.bigActionButton("📊 Gerar Relatório", e -> gerarRelatorio());
        btnGerar.setBackground(new Color(0, 84, 166));
        btnGerar.setForeground(Color.WHITE);
        btnGerar.setPreferredSize(new Dimension(150, 28));

        JButton btnImprimir = UIFactory.bigActionButton("🖨 Imprimir", e -> imprimir());
        btnImprimir.setPreferredSize(new Dimension(110, 28));

        JButton btnLimpar = UIFactory.bigActionButton("Limpar", e -> limpar());
        btnLimpar.setPreferredSize(new Dimension(80, 28));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.setOpaque(false);
        btns.add(btnGerar);
        btns.add(btnImprimir);
        btns.add(btnLimpar);

        gc.gridx = 4; gc.weightx = 0;
        filtro.add(btns, gc);

        // Enter dispara geração
        java.awt.event.KeyAdapter enter = new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) gerarRelatorio();
            }
        };
        tfDataDe.addKeyListener(enter);
        tfDataAte.addKeyListener(enter);

        p.add(filtro, BorderLayout.CENTER);
        return p;
    }

    // ── Painel de resultado ───────────────────────────────────────────────────

    private JPanel buildResultadoPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(6, 8, 8, 8));

        // Resumo no topo
        JPanel resumo = new JPanel(new GridLayout(1, 3, 10, 0));
        resumo.setBackground(UIFactory.XP_PANEL_BG);
        resumo.setBorder(UIFactory.groupBorder("Resumo"));

        lblPeriodo   = resumoItem("Período", "—");
        lblQtdVendas = resumoItem("Total de Vendas", "0");
        lblTotalGeral = resumoItem("Valor Total", "R$ 0,00");

        resumo.add(lblPeriodo.getParent());
        resumo.add(lblQtdVendas.getParent());
        resumo.add(lblTotalGeral.getParent());

        p.add(resumo, BorderLayout.NORTH);

        // Tabela de vendas
        tableModel = new DefaultTableModel(
                new String[]{"#", "Data/Hora", "Cliente", "Forma Pagamento", "Subtotal (R$)", "Desconto (R$)", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = UIFactory.styledTable();
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);

        // Alinha valor à direita
        DefaultTableCellRenderer rightAlign = new DefaultTableCellRenderer();
        rightAlign.setHorizontalAlignment(SwingConstants.RIGHT);
        rightAlign.setFont(UIFactory.FONT_BOLD);
        table.getColumnModel().getColumn(4).setCellRenderer(rightAlign);
        table.getColumnModel().getColumn(5).setCellRenderer(rightAlign);
        table.getColumnModel().getColumn(6).setCellRenderer(rightAlign);

        // Linha de total no rodapé da tabela
        JPanel rodapeTabela = new JPanel(new BorderLayout());
        rodapeTabela.setBackground(new Color(10, 36, 106));
        rodapeTabela.setBorder(new EmptyBorder(4, 8, 4, 8));
        JLabel lblRodape = new JLabel("TOTAL GERAL:");
        lblRodape.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblRodape.setForeground(Color.WHITE);

        JLabel lblValorRodape = new JLabel("R$ 0,00");
        lblValorRodape.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblValorRodape.setForeground(new Color(100, 255, 100));
        lblValorRodape.setHorizontalAlignment(SwingConstants.RIGHT);
        lblValorRodape.setName("lblValorRodape");

        rodapeTabela.add(lblRodape, BorderLayout.WEST);
        rodapeTabela.add(lblValorRodape, BorderLayout.EAST);

        JPanel tabelaWrapper = new JPanel(new BorderLayout());
        tabelaWrapper.setBackground(UIFactory.XP_BG);
        tabelaWrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        tabelaWrapper.add(rodapeTabela, BorderLayout.SOUTH);

        p.add(tabelaWrapper, BorderLayout.CENTER);

        // Guarda referência ao label de valor do rodapé
        lblTotalGeral.putClientProperty("rodape", lblValorRodape);

        return p;
    }

    // ── Gerar relatório ───────────────────────────────────────────────────────

    private void gerarRelatorio() {
        String dataDe  = tfDataDe.getText().trim();
        String dataAte = tfDataAte.getText().trim();

        if (Validator.isBlank(dataDe) || Validator.isBlank(dataAte)) {
            JOptionPane.showMessageDialog(this,
                    "Informe as duas datas para gerar o relatório.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate de, ate;
        try { de = LocalDate.parse(dataDe, FMT_IN); }
        catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Data inicial inválida. Use dd/MM/yyyy (ex: 01/03/2026).",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfDataDe.requestFocus(); return;
        }
        try { ate = LocalDate.parse(dataAte, FMT_IN); }
        catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Data final inválida. Use dd/MM/yyyy (ex: 31/03/2026).",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            tfDataAte.requestFocus(); return;
        }

        if (de.isAfter(ate)) {
            JOptionPane.showMessageDialog(this,
                    "A data inicial não pode ser maior que a data final.",
                    "Atenção", JOptionPane.WARNING_MESSAGE); return;
        }

        LocalDateTime inicio = de.atStartOfDay();
        LocalDateTime fim    = ate.atTime(23, 59, 59);

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Pagamento> vendas = em.createQuery(
                            "SELECT p FROM Pagamento p " +
                                    "WHERE p.dataPagamento >= :ini AND p.dataPagamento <= :fim " +
                                    "ORDER BY p.dataPagamento ASC", Pagamento.class)
                    .setParameter("ini", inicio)
                    .setParameter("fim", fim)
                    .getResultList();

            tableModel.setRowCount(0);
            BigDecimal total = BigDecimal.ZERO;

            for (Pagamento pag : vendas) {
                String cliente = pag.getPedido() != null && pag.getPedido().getCliente() != null
                        ? pag.getPedido().getCliente().getNome() : "—";
                BigDecimal desconto   = pag.getDesconto() != null ? pag.getDesconto() : BigDecimal.ZERO;
                BigDecimal valorFinal = pag.getValorFinal() != null ? pag.getValorFinal() : pag.getValorPago();
                tableModel.addRow(new Object[]{
                        pag.getPedido() != null ? "#" + pag.getPedido().getId() : "—",
                        pag.getDataPagamento().format(FMT_OUT),
                        cliente,
                        pag.getFormaPagamento().toString(),
                        String.format("R$ %.2f", pag.getValorPago()),
                        desconto.compareTo(BigDecimal.ZERO) > 0 ? String.format("- R$ %.2f", desconto) : "—",
                        String.format("R$ %.2f", valorFinal)
                });
                total = total.add(valorFinal); // usa o valor com desconto
            }

            // Atualiza resumo
            String periodoStr = dataDe + " a " + dataAte;
            lblPeriodo.setText(periodoStr);
            lblQtdVendas.setText(String.valueOf(vendas.size()));
            String totalStr = String.format("R$ %.2f", total);
            lblTotalGeral.setText(totalStr);

            // Atualiza rodapé da tabela
            JLabel rodape = (JLabel) lblTotalGeral.getClientProperty("rodape");
            if (rodape != null) rodape.setText(totalStr);

            if (vendas.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Nenhuma venda encontrada no período informado.",
                        "Resultado", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao gerar relatório: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } finally { if (em != null) em.close(); }
    }

    private void limpar() {
        tfDataDe.setText("");
        tfDataAte.setText("");
        tableModel.setRowCount(0);
        lblPeriodo.setText("—");
        lblQtdVendas.setText("0");
        lblTotalGeral.setText("R$ 0,00");
        JLabel rodape = (JLabel) lblTotalGeral.getClientProperty("rodape");
        if (rodape != null) rodape.setText("R$ 0,00");
    }

    // ── Impressão ─────────────────────────────────────────────────────────────

    private void imprimir() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Gere o relatório antes de imprimir.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Monta um painel para impressão
        JPanel printPanel = buildPrintPanel();
        printPanel.setSize(printPanel.getPreferredSize());
        printPanel.doLayout();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Relatório de Vendas");
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scale = Math.min(
                    pageFormat.getImageableWidth()  / printPanel.getWidth(),
                    pageFormat.getImageableHeight() / printPanel.getHeight());
            if (scale < 1.0) g2.scale(scale, scale);
            printPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                        "Erro ao imprimir: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildPrintPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 28, 20, 28));
        p.setPreferredSize(new Dimension(650, 80 + tableModel.getRowCount() * 20 + 160));

        Font fTitle  = new Font("Tahoma", Font.BOLD, 14);
        Font fBold   = new Font("Tahoma", Font.BOLD, 11);
        Font fNormal = new Font("Tahoma", Font.PLAIN, 10);

        // Cabeçalho
        JLabel titulo = new JLabel("CARMEL SISTEMA DE GESTÃO", SwingConstants.CENTER);
        titulo.setFont(fTitle); titulo.setForeground(new Color(0, 60, 140));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        p.add(titulo);

        JLabel sub = new JLabel("RELATÓRIO DE VENDAS", SwingConstants.CENTER);
        sub.setFont(fBold); sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        p.add(sub);

        JLabel periodo = new JLabel("Período: " + lblPeriodo.getText(), SwingConstants.CENTER);
        periodo.setFont(fNormal); periodo.setAlignmentX(Component.CENTER_ALIGNMENT);
        periodo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        p.add(periodo);

        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(separator());
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        // Cabeçalho da tabela
        JPanel header = new JPanel(new GridLayout(1, 6));
        header.setBackground(new Color(10, 36, 106));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        for (String h : new String[]{"Pedido", "Data/Hora", "Cliente", "Forma Pag.", "Desconto", "Total (R$)"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE);
            header.add(l);
        }
        p.add(header);

        // Linhas
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            JPanel row = new JPanel(new GridLayout(1, 6));
            row.setBackground(i % 2 == 0 ? Color.WHITE : new Color(240, 244, 255));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
            for (int col : new int[]{0, 1, 2, 3, 5, 6}) {
                Object val = tableModel.getValueAt(i, col);
                JLabel l = new JLabel("  " + (val != null ? val.toString() : ""));
                l.setFont(fNormal);
                row.add(l);
            }
            p.add(row);
        }

        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(separator());
        p.add(Box.createRigidArea(new Dimension(0, 6)));

        // Total
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lblT = new JLabel("TOTAL GERAL:");
        lblT.setFont(fBold);
        JLabel lblV = new JLabel(lblTotalGeral.getText());
        lblV.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblV.setForeground(new Color(0, 100, 0));
        lblV.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(lblT, BorderLayout.WEST);
        totalRow.add(lblV, BorderLayout.EAST);
        p.add(totalRow);

        p.add(Box.createRigidArea(new Dimension(0, 6)));
        JLabel qtd = new JLabel("Total de vendas: " + lblQtdVendas.getText(), SwingConstants.RIGHT);
        qtd.setFont(fNormal); qtd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        p.add(qtd);

        p.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel gen = new JLabel("Gerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), SwingConstants.CENTER);
        gen.setFont(fNormal); gen.setForeground(Color.GRAY);
        gen.setAlignmentX(Component.CENTER_ALIGNMENT);
        gen.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        p.add(gen);

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Cria um card de resumo e retorna o JLabel do valor (pai é o card). */
    private JLabel resumoItem(String titulo, String valor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
        card.setBackground(UIFactory.XP_BG);
        card.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("Tahoma", Font.PLAIN, 10));
        lblTit.setForeground(new Color(80, 80, 80));

        JLabel lblVal = new JLabel(valor, SwingConstants.CENTER);
        lblVal.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblVal.setForeground(new Color(0, 84, 166));

        card.add(lblTit);
        card.add(lblVal);
        return lblVal;
    }

    private JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(180, 180, 180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    public void resetar() { limpar(); }
}