package br.carmel.ui.panels;


import br.carmel.model.*;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CaixaPanel extends BackgroundPanel {

    private final EntityManagerFactory emf;
    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Estado do caixa
    private boolean    caixaAberto = false;
    private BigDecimal saldoAtual  = BigDecimal.ZERO;
    private LocalDateTime inicioTurnoAtual = null; // horário da última abertura

    // Componentes de status
    private JLabel lblStatus;
    private JLabel lblSaldo;
    private JLabel lblData;

    // Botões principais
    private JButton btnAbrirCaixa;
    private JButton btnFecharCaixa;
    private JButton btnSangria;
    private JButton btnSuprimento;
    private JButton btnVerFechamento;

    // Tabelas do dia
    private DefaultTableModel movModel;
    private JTable movTable;
    private DefaultTableModel resumoModel;

    // Aba de caixas fechados
    private DefaultTableModel fechadosModel;

    public CaixaPanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());


        build();
    }

    public boolean isCaixaAberto() { return caixaAberto; }

    public void reloadCaixa() {
        verificarEstadoCaixa();
        reloadMovimentos();
        reloadResumo();
        reloadCaixasFechados();
    }

    // ── Montagem UI ───────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Caixa"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIFactory.FONT_BOLD);
        tabs.setBackground(UIFactory.XP_BG);
        tabs.addTab("Caixa do Dia",       buildDiaPanel());
        tabs.addTab("Caixas Fechados",    buildFechadosPanel());
        add(tabs, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Aba: Caixa do Dia ─────────────────────────────────────────────────────

    private JPanel buildDiaPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBackground(UIFactory.XP_BG);
        center.setBorder(new EmptyBorder(8, 8, 8, 8));
        center.add(buildTopPanel(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildMovimentosPanel(), buildResumoPanel());
        split.setDividerLocation(620);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        center.add(split, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(UIFactory.groupBorder("Status do Caixa"));

        JPanel info = new JPanel(new GridLayout(3, 1, 2, 2));
        info.setOpaque(false);
        lblStatus = new JLabel("● CAIXA FECHADO");
        lblStatus.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblStatus.setForeground(new Color(180, 0, 0));
        lblSaldo = new JLabel("Saldo atual: R$ 0,00");
        lblSaldo.setFont(UIFactory.FONT_BOLD);
        lblSaldo.setForeground(new Color(0, 100, 0));
        lblData = new JLabel("Data: " + LocalDate.now().format(FMT_DIA));
        lblData.setFont(UIFactory.FONT_NORMAL);
        info.add(lblStatus); info.add(lblSaldo); info.add(lblData);
        p.add(info, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btns.setOpaque(false);
        btnAbrirCaixa  = UIFactory.bigActionButton("Abrir Caixa",   e -> abrirCaixa());
        btnFecharCaixa = UIFactory.bigActionButton("Fechar Caixa",  e -> fecharCaixa());
        btnSangria     = UIFactory.bigActionButton("Sangria",        e -> sangria());
        btnSuprimento  = UIFactory.bigActionButton("Suprimento",     e -> suprimento());
        btnVerFechamento = UIFactory.bigActionButton("🖨 Rel. Fechamento", e -> abrirRelatorioHoje());
        JButton btnAtualizar = UIFactory.bigActionButton("Atualizar", e -> reloadCaixa());

        btnAbrirCaixa.setBackground(new Color(0, 128, 0));   btnAbrirCaixa.setForeground(Color.WHITE);
        btnFecharCaixa.setBackground(new Color(180, 0, 0));  btnFecharCaixa.setForeground(Color.WHITE);
        btnVerFechamento.setBackground(new Color(0, 84, 166)); btnVerFechamento.setForeground(Color.WHITE);

        btns.add(btnAbrirCaixa); btns.add(btnFecharCaixa);
        btns.add(btnSangria); btns.add(btnSuprimento);
        btns.add(btnAtualizar); btns.add(btnVerFechamento);
        p.add(btns, BorderLayout.EAST);
        p.setPreferredSize(new Dimension(0, 90));
        return p;
    }

    private JPanel buildMovimentosPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(0, 0, 0, 4));

        JLabel lbl = new JLabel("Movimentos do Dia");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(new Color(0, 84, 166));
        lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        movModel = new DefaultTableModel(
                new String[]{"Hora", "Tipo", "Descrição", "Valor (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        movTable = UIFactory.styledTable();
        movTable.setModel(movModel);
        movTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        movTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        movTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        movTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        movTable.setDefaultRenderer(Object.class, (table, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSelected) {
                cell.setBackground(UIFactory.XP_TABLE_SEL);
                cell.setForeground(Color.WHITE);
            } else {
                String tipo = (String) movModel.getValueAt(row, 1);
                cell.setForeground(Color.BLACK);
                cell.setBackground(switch (tipo) {
                    case "ABERTURA"   -> new Color(220, 240, 220);
                    case "VENDA"      -> new Color(220, 235, 255);
                    case "SANGRIA"    -> new Color(255, 220, 220);
                    case "SUPRIMENTO" -> new Color(255, 245, 200);
                    case "FECHAMENTO" -> new Color(230, 230, 230);
                    default           -> Color.WHITE;
                });
            }
            return cell;
        });

        JScrollPane scroll = new JScrollPane(movTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildResumoPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);

        JLabel lbl = new JLabel("Resumo do Dia");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(new Color(0, 84, 166));
        lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        resumoModel = new DefaultTableModel(
                new String[]{"Forma de Pagamento", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable resumoTable = UIFactory.styledTable();
        resumoTable.setModel(resumoModel);
        resumoTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        resumoTable.getColumnModel().getColumn(1).setPreferredWidth(100);

        JScrollPane scroll = new JScrollPane(resumoTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── Aba: Caixas Fechados ──────────────────────────────────────────────────

    private JPanel buildFechadosPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Pesquisa por período ──────────────────────────────────────────────
        JPanel buscaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        buscaPanel.setBackground(UIFactory.XP_PANEL_BG);
        buscaPanel.setBorder(UIFactory.groupBorder("Pesquisar Caixas"));

        buscaPanel.add(UIFactory.labelLight("De:"));
        JTextField tfBuscaDe  = UIFactory.styledField("");
        tfBuscaDe.setPreferredSize(new Dimension(100, 22));
        tfBuscaDe.setToolTipText("dd/MM/yyyy");
        buscaPanel.add(tfBuscaDe);

        buscaPanel.add(UIFactory.labelLight("Até:"));
        JTextField tfBuscaAte = UIFactory.styledField("");
        tfBuscaAte.setPreferredSize(new Dimension(100, 22));
        tfBuscaAte.setToolTipText("dd/MM/yyyy");
        buscaPanel.add(tfBuscaAte);

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", null);
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        JButton btnLimparBusca = UIFactory.bigActionButton("Limpar", null);

        Runnable executarBusca = () -> {
            String deStr  = tfBuscaDe.getText().trim();
            String ateStr = tfBuscaAte.getText().trim();
            LocalDate de = null, ate = null;
            DateTimeFormatter f = FMT_DIA;
            if (!deStr.isEmpty()) {
                try { de = LocalDate.parse(deStr, f); }
                catch (Exception ex) { JOptionPane.showMessageDialog(p, "Data 'De' inválida. Use dd/MM/yyyy."); return; }
            }
            if (!ateStr.isEmpty()) {
                try { ate = LocalDate.parse(ateStr, f); }
                catch (Exception ex) { JOptionPane.showMessageDialog(p, "Data 'Até' inválida. Use dd/MM/yyyy."); return; }
            }
            reloadCaixasFechadosFiltrado(de, ate);
        };

        btnBuscar.addActionListener(e -> executarBusca.run());
        btnLimparBusca.addActionListener(e -> {
            tfBuscaDe.setText(""); tfBuscaAte.setText("");
            reloadCaixasFechados();
        });
        java.awt.event.KeyAdapter enterBusca = new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) executarBusca.run();
            }
        };
        tfBuscaDe.addKeyListener(enterBusca);
        tfBuscaAte.addKeyListener(enterBusca);

        buscaPanel.add(btnBuscar);
        buscaPanel.add(btnLimparBusca);
        p.add(buscaPanel, BorderLayout.NORTH);

        fechadosModel = new DefaultTableModel(
                new String[]{"Data", "Abertura (R$)", "Vendas (R$)", "Sangrias (R$)", "Suprimentos (R$)", "Fechamento Sistema (R$)", "Valor Informado (R$)", "Diferença (R$)", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable fechadosTable = UIFactory.styledTable();
        fechadosTable.setModel(fechadosModel);
        fechadosTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        for (int i = 1; i <= 7; i++) fechadosTable.getColumnModel().getColumn(i).setPreferredWidth(110);
        fechadosTable.getColumnModel().getColumn(8).setPreferredWidth(90);

        // Colorir por status
        fechadosTable.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(col == 8 ? UIFactory.FONT_BOLD : UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 5, 1, 5));
            if (col >= 1) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            if (isSelected) {
                cell.setBackground(UIFactory.XP_TABLE_SEL);
                cell.setForeground(Color.WHITE);
            } else {
                String status = fechadosModel.getValueAt(row, 8) != null
                        ? fechadosModel.getValueAt(row, 8).toString() : "";
                cell.setBackground(switch (status) {
                    case "✅ Correto"   -> new Color(220, 255, 220);
                    case "⚠ Diferença" -> new Color(255, 240, 200);
                    default            -> row % 2 == 0 ? Color.WHITE : new Color(248, 248, 252);
                });
                cell.setForeground(col == 7
                        ? (status.contains("Correto") ? new Color(0, 110, 0) : new Color(160, 60, 0))
                        : Color.BLACK);
            }
            return cell;
        });

        // Clique duplo abre relatório do caixa fechado
        fechadosTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && fechadosTable.getSelectedRow() >= 0) {
                    String dataStr = (String) fechadosModel.getValueAt(fechadosTable.getSelectedRow(), 0);
                    try {
                        LocalDate data = LocalDate.parse(dataStr, FMT_DIA);
                        abrirRelatorioCaixaData(data);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(fechadosTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rodape.setBackground(new Color(212, 208, 200));
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        rodape.add(UIFactory.bigActionButton("↻ Atualizar", e -> reloadCaixasFechados()));
        JButton btnImpr = UIFactory.bigActionButton("🖨 Ver / Imprimir Selecionado", e -> {
            if (fechadosTable.getSelectedRow() < 0) {
                JOptionPane.showMessageDialog(p, "Selecione um caixa na lista.", "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String dataStr = (String) fechadosModel.getValueAt(fechadosTable.getSelectedRow(), 0);
            try {
                LocalDate data = LocalDate.parse(dataStr, FMT_DIA);
                abrirRelatorioCaixaData(data);
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
        rodape.add(btnImpr);
        JLabel dica = new JLabel("  Dica: clique duplo na linha para ver o relatório");
        dica.setFont(new Font("Tahoma", Font.ITALIC, 10));
        dica.setForeground(new Color(100, 100, 100));
        rodape.add(dica);
        p.add(rodape, BorderLayout.SOUTH);

        return p;
    }

    // ── Barra de status ───────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JLabel hint = new JLabel("Dica: O caixa deve estar aberto para registrar vendas. Ao fechar, informe o valor contado no caixa físico.");
        hint.setFont(new Font("Tahoma", Font.ITALIC, 10));
        hint.setForeground(new Color(80, 80, 80));
        bar.add(hint);
        return bar;
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    private void abrirCaixa() {
        if (caixaAberto) { JOptionPane.showMessageDialog(this, "O caixa já está aberto.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        String valorStr = JOptionPane.showInputDialog(this,
                "Informe o valor inicial do caixa (R$):", "Abrir Caixa", JOptionPane.QUESTION_MESSAGE);
        if (Validator.isBlank(valorStr)) return;

        BigDecimal valorInicial;
        try {
            valorInicial = Validator.parseBigDecimal(valorStr);
            if (valorInicial.compareTo(BigDecimal.ZERO) < 0) { showError("Valor inicial não pode ser negativo."); return; }
        } catch (Exception e) { showError("Valor inválido."); return; }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            CaixaMovimento mov = new CaixaMovimento();
            mov.setTipo(TipoCaixaMovimento.ABERTURA);
            mov.setValor(valorInicial);
            mov.setDescricao("Abertura de caixa com R$ " + String.format("%.2f", valorInicial));
            em.persist(mov);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Caixa aberto com R$ " + String.format("%.2f", valorInicial));
            reloadCaixa();
        } catch (Exception ex) {
            rollback(em); showError("Erro ao abrir caixa: " + ex.getMessage()); ex.printStackTrace();
        } finally { close(em); }
    }

    private void fecharCaixa() {
        if (!caixaAberto) { JOptionPane.showMessageDialog(this, "O caixa já está fechado.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        // ── Diálogo de fechamento ─────────────────────────────────────────────
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Fechar Caixa", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 220);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7, 6, 7, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Aviso: não mostra saldo do sistema
        JLabel aviso = new JLabel("Informe o valor contado fisicamente no caixa:");
        aviso.setFont(UIFactory.FONT_BOLD);
        aviso.setForeground(new Color(0, 84, 166));
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; form.add(aviso, gc);

        JLabel sub = new JLabel("O sistema verificará se o valor está correto após o fechamento.");
        sub.setFont(new Font("Tahoma", Font.ITALIC, 10));
        sub.setForeground(Color.GRAY);
        gc.gridy = 1; form.add(sub, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; form.add(UIFactory.labelLight("Valor no caixa (R$):"), gc);
        JTextField tfValorFechamento = UIFactory.styledField("");
        tfValorFechamento.setPreferredSize(new Dimension(160, 24));
        tfValorFechamento.setToolTipText("Digite o valor que você contou fisicamente");
        gc.gridx = 1; gc.weightx = 1; form.add(tfValorFechamento, gc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] confirmado = {false};
        JButton btnOk = UIFactory.bigActionButton("Fechar Caixa", e -> { confirmado[0] = true; dlg.dispose(); });
        btnOk.setBackground(new Color(180, 0, 0)); btnOk.setForeground(Color.WHITE);
        JButton btnCancelar = UIFactory.bigActionButton("Cancelar", e -> dlg.dispose());
        btns.add(btnOk); btns.add(btnCancelar);
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfValorFechamento::requestFocusInWindow);
        dlg.setVisible(true);

        if (!confirmado[0]) return;

        BigDecimal valorInformado;
        try {
            valorInformado = Validator.parseBigDecimal(tfValorFechamento.getText());
            if (valorInformado.compareTo(BigDecimal.ZERO) < 0) { showError("Valor não pode ser negativo."); return; }
        } catch (Exception e) { showError("Valor inválido. Digite um número (ex: 150.00)"); return; }

        // Persiste fechamento com valor informado na descrição
        BigDecimal diferenca = valorInformado.subtract(saldoAtual);
        String statusStr = diferenca.abs().compareTo(new BigDecimal("0.01")) <= 0 ? "CORRETO" : "DIFERENCA";

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            CaixaMovimento mov = new CaixaMovimento();
            mov.setTipo(TipoCaixaMovimento.FECHAMENTO);
            mov.setValor(saldoAtual);
            // Guarda valor informado e diferença na descrição (formato parseável)
            mov.setDescricao(String.format("FECHAMENTO|sistema=%.2f|informado=%.2f|diferenca=%.2f|status=%s",
                    saldoAtual, valorInformado, diferenca, statusStr));
            em.persist(mov);
            em.getTransaction().commit();

            // Mostra resultado APÓS fechar
            String msg;
            if ("CORRETO".equals(statusStr)) {
                msg = "✅ Caixa fechado com sucesso!\n\n" +
                        "Valor do sistema:   R$ " + String.format("%.2f", saldoAtual) + "\n" +
                        "Valor que você informou: R$ " + String.format("%.2f", valorInformado) + "\n\n" +
                        "✔ Caixa fechado corretamente!";
            } else {
                msg = "⚠ Caixa fechado com diferença!\n\n" +
                        "Valor do sistema:   R$ " + String.format("%.2f", saldoAtual) + "\n" +
                        "Valor que você informou: R$ " + String.format("%.2f", valorInformado) + "\n" +
                        "Diferença:          R$ " + String.format("%.2f", diferenca) + "\n\n" +
                        (diferenca.compareTo(BigDecimal.ZERO) > 0 ? "⬆ Sobrou dinheiro no caixa." : "⬇ Faltou dinheiro no caixa.");
            }
            JOptionPane.showMessageDialog(this, msg, "Resultado do Fechamento",
                    "CORRETO".equals(statusStr) ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

            reloadCaixa();
        } catch (Exception ex) {
            rollback(em); showError("Erro ao fechar caixa: " + ex.getMessage()); ex.printStackTrace();
        } finally { close(em); }
    }

    private void sangria() {
        if (!caixaAberto) { JOptionPane.showMessageDialog(this, "Abra o caixa primeiro.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        String valorStr = JOptionPane.showInputDialog(this, "Valor da sangria (R$):", "Sangria", JOptionPane.QUESTION_MESSAGE);
        if (Validator.isBlank(valorStr)) return;
        BigDecimal valor;
        try {
            valor = Validator.parseBigDecimal(valorStr);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) { showError("Valor deve ser maior que zero."); return; }
            if (valor.compareTo(saldoAtual) > 0) { showError("Valor da sangria maior que o saldo atual (R$ " + String.format("%.2f", saldoAtual) + ")."); return; }
        } catch (Exception e) { showError("Valor inválido."); return; }
        String desc = JOptionPane.showInputDialog(this, "Motivo da sangria (opcional):");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            CaixaMovimento mov = new CaixaMovimento();
            mov.setTipo(TipoCaixaMovimento.SANGRIA);
            mov.setValor(valor.negate());
            mov.setDescricao(Validator.isBlank(desc) ? "Sangria" : "Sangria: " + desc);
            em.persist(mov);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Sangria de R$ " + String.format("%.2f", valor) + " registrada.");
            reloadCaixa();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); ex.printStackTrace(); }
        finally { close(em); }
    }

    private void suprimento() {
        if (!caixaAberto) { JOptionPane.showMessageDialog(this, "Abra o caixa primeiro.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        String valorStr = JOptionPane.showInputDialog(this, "Valor do suprimento (R$):", "Suprimento", JOptionPane.QUESTION_MESSAGE);
        if (Validator.isBlank(valorStr)) return;
        BigDecimal valor;
        try {
            valor = Validator.parseBigDecimal(valorStr);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) { showError("Valor deve ser maior que zero."); return; }
        } catch (Exception e) { showError("Valor inválido."); return; }
        String desc = JOptionPane.showInputDialog(this, "Motivo do suprimento (opcional):");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            CaixaMovimento mov = new CaixaMovimento();
            mov.setTipo(TipoCaixaMovimento.SUPRIMENTO);
            mov.setValor(valor);
            mov.setDescricao(Validator.isBlank(desc) ? "Suprimento" : "Suprimento: " + desc);
            em.persist(mov);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Suprimento de R$ " + String.format("%.2f", valor) + " registrado.");
            reloadCaixa();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); ex.printStackTrace(); }
        finally { close(em); }
    }

    // ── Relatório de caixa ────────────────────────────────────────────────────

    private void abrirRelatorioHoje() {
        abrirRelatorioCaixaData(LocalDate.now());
    }

    private void abrirRelatorioCaixaData(LocalDate data) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<CaixaMovimento> movimentos = em.createQuery(
                    "SELECT m FROM CaixaMovimento m WHERE m.dataCaixa = :d ORDER BY m.dataMovimento ASC",
                    CaixaMovimento.class).setParameter("d", data).getResultList();

            if (movimentos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum movimento encontrado para " + data.format(FMT_DIA), "Atenção", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Quebra os movimentos em turnos (cada ABERTURA inicia novo turno)
            // Cada turno = lista de movimentos entre uma ABERTURA e o próximo FECHAMENTO
            java.util.List<java.util.List<CaixaMovimento>> turnos = new java.util.ArrayList<>();
            java.util.List<CaixaMovimento> turnoAtual = null;
            for (CaixaMovimento m : movimentos) {
                if (m.getTipo() == TipoCaixaMovimento.ABERTURA) {
                    turnoAtual = new java.util.ArrayList<>();
                    turnos.add(turnoAtual);
                }
                if (turnoAtual != null) turnoAtual.add(m);
            }

            // Se há múltiplos turnos, mostra seleção; senão abre direto
            if (turnos.size() > 1) {
                String[] opcoes = new String[turnos.size()];
                for (int i = 0; i < turnos.size(); i++) {
                    CaixaMovimento ab = turnos.get(i).get(0);
                    boolean fechado = turnos.get(i).stream().anyMatch(m -> m.getTipo() == TipoCaixaMovimento.FECHAMENTO);
                    opcoes[i] = "Turno " + (i+1) + " — " + ab.getDataMovimento().format(FMT) + (fechado ? " (fechado)" : " (aberto)");
                }
                String escolha = (String) JOptionPane.showInputDialog(this,
                        "Este dia possui " + turnos.size() + " turnos. Selecione:", "Selecionar Turno",
                        JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[opcoes.length - 1]);
                if (escolha == null) return;
                int idx = java.util.Arrays.asList(opcoes).indexOf(escolha);
                mostrarRelatorioTurno(data, turnos.get(idx), em);
            } else if (turnos.size() == 1) {
                mostrarRelatorioTurno(data, turnos.get(0), em);
            }

        } catch (Exception ex) { ex.printStackTrace(); showError("Erro ao gerar relatório: " + ex.getMessage()); }
        finally { close(em); }
    }

    private void mostrarRelatorioTurno(LocalDate data, java.util.List<CaixaMovimento> movsTurno, EntityManager em) {
        BigDecimal abertura = BigDecimal.ZERO, sangrias = BigDecimal.ZERO,
                suprimentos = BigDecimal.ZERO, valorInformado = null, diferenca = null;
        String statusFechamento = null;
        LocalDateTime horaAbertura = null, horaFechamento = null;

        for (CaixaMovimento m : movsTurno) {
            switch (m.getTipo()) {
                case ABERTURA   -> { abertura = m.getValor(); horaAbertura = m.getDataMovimento(); }
                case SANGRIA    -> sangrias = sangrias.add(m.getValor().abs());
                case SUPRIMENTO -> suprimentos = suprimentos.add(m.getValor());
                case FECHAMENTO -> {
                    horaFechamento = m.getDataMovimento();
                    String desc = m.getDescricao();
                    if (desc != null && desc.startsWith("FECHAMENTO|")) {
                        try {
                            for (String part : desc.split("\\|")) {
                                if (part.startsWith("informado=")) valorInformado = new BigDecimal(part.substring(10));
                                if (part.startsWith("diferenca=")) diferenca      = new BigDecimal(part.substring(10));
                                if (part.startsWith("status="))    statusFechamento = part.substring(7);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                default -> {}
            }
        }

        // Vendas apenas do período deste turno (abertura até fechamento ou agora)
        LocalDateTime fimTurno = horaFechamento != null ? horaFechamento : LocalDateTime.now();
        BigDecimal vendas = em.createQuery(
                        "SELECT COALESCE(SUM(p.valorPago), 0) FROM Pagamento p " +
                                "WHERE p.dataPagamento >= :inicio AND p.dataPagamento <= :fim",
                        BigDecimal.class)
                .setParameter("inicio", horaAbertura)
                .setParameter("fim", fimTurno)
                .getSingleResult();

        BigDecimal saldoSistema = abertura.add(vendas).add(suprimentos).subtract(sangrias);

        mostrarRelatorioDialog(data, horaAbertura, horaFechamento,
                abertura, vendas, sangrias, suprimentos, saldoSistema,
                valorInformado, diferenca, statusFechamento, movsTurno);
    }

    private void mostrarRelatorioDialog(LocalDate data,
                                        LocalDateTime horaAbertura, LocalDateTime horaFechamento,
                                        BigDecimal abertura, BigDecimal vendas,
                                        BigDecimal sangrias, BigDecimal suprimentos, BigDecimal saldoSistema,
                                        BigDecimal valorInformado, BigDecimal diferenca,
                                        String statusFechamento, List<CaixaMovimento> movimentos) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Relatório de Caixa - " + data.format(FMT_DIA),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(600, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        // ── Conteúdo do relatório ─────────────────────────────────────────────
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        Font fTitle  = new Font("Tahoma", Font.BOLD, 14);
        Font fSub    = new Font("Tahoma", Font.BOLD, 11);
        Font fNormal = new Font("Tahoma", Font.PLAIN, 10);
        Font fBold   = new Font("Tahoma", Font.BOLD, 10);

        addLinha(content, "CARMEL SISTEMA DE GESTÃO", fTitle, new Color(0, 60, 140), SwingConstants.CENTER);
        addLinha(content, "RELATÓRIO DE FECHAMENTO DE CAIXA", fSub, Color.BLACK, SwingConstants.CENTER);
        addLinha(content, "Data: " + data.format(FMT_DIA), fNormal, Color.GRAY, SwingConstants.CENTER);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(sep());
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        // Horários
        String haStr  = horaAbertura   != null ? horaAbertura.format(FMT)   : "—";
        String hfStr  = horaFechamento != null ? horaFechamento.format(FMT) : "Em aberto";
        addLinha(content, "Abertura: " + haStr + "   |   Fechamento: " + hfStr, fNormal, Color.BLACK, SwingConstants.LEFT);
        content.add(Box.createRigidArea(new Dimension(0, 8)));

        // Resumo financeiro
        addLinha(content, "RESUMO FINANCEIRO", fSub, new Color(0, 84, 166), SwingConstants.LEFT);
        content.add(sep());
        addValor(content, "Valor de Abertura:",     abertura,    fNormal, Color.BLACK);
        addValor(content, "+ Vendas do dia:",        vendas,      fNormal, new Color(0, 110, 0));
        addValor(content, "+ Suprimentos:",          suprimentos, fNormal, new Color(0, 110, 0));
        addValor(content, "- Sangrias:",             sangrias,    fNormal, new Color(180, 0, 0));
        content.add(sep());
        addValor(content, "= Saldo do Sistema:",     saldoSistema, fBold,  new Color(0, 60, 140));

        if (valorInformado != null) {
            addValor(content, "Valor Informado (físico):", valorInformado, fBold, Color.BLACK);
            boolean ok = diferenca != null && diferenca.abs().compareTo(new BigDecimal("0.01")) <= 0;
            String difStr = diferenca != null ? String.format("R$ %.2f", diferenca) : "—";
            Color difColor = ok ? new Color(0, 110, 0) : new Color(180, 0, 0);
            JPanel difRow = new JPanel(new BorderLayout());
            difRow.setOpaque(false);
            difRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel difLbl = new JLabel("Diferença:");
            difLbl.setFont(fBold);
            JLabel difVal = new JLabel(difStr + (ok ? "  ✅ CORRETO" : "  ⚠ DIFERENÇA"));
            difVal.setFont(fBold); difVal.setForeground(difColor);
            difVal.setHorizontalAlignment(SwingConstants.RIGHT);
            difRow.add(difLbl, BorderLayout.WEST);
            difRow.add(difVal, BorderLayout.EAST);
            content.add(difRow);
        } else {
            addLinha(content, "⚠ Caixa ainda não fechado ou sem valor informado.", fNormal, Color.GRAY, SwingConstants.LEFT);
        }

        content.add(Box.createRigidArea(new Dimension(0, 10)));
        addLinha(content, "MOVIMENTOS DO DIA", fSub, new Color(0, 84, 166), SwingConstants.LEFT);
        content.add(sep());

        // Tabela de movimentos
        for (CaixaMovimento m : movimentos) {
            String desc = m.getDescricao();
            if (desc != null && desc.startsWith("FECHAMENTO|")) desc = "Fechamento de caixa";
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            JLabel lHora = new JLabel(m.getDataMovimento().format(DateTimeFormatter.ofPattern("HH:mm")) + "  [" + m.getTipo() + "]  " + desc);
            lHora.setFont(fNormal);
            JLabel lVal = new JLabel(String.format("R$ %.2f", m.getValor()));
            lVal.setFont(fNormal);
            lVal.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(lHora, BorderLayout.WEST);
            row.add(lVal, BorderLayout.EAST);
            content.add(row);
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        dlg.add(scroll, BorderLayout.CENTER);

        // Botão imprimir
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rodape.setBackground(UIFactory.XP_BG);
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JButton btnImprimir = UIFactory.bigActionButton("🖨 Imprimir", null);
        btnImprimir.setBackground(new Color(0, 84, 166)); btnImprimir.setForeground(Color.WHITE);
        btnImprimir.addActionListener(e -> imprimirRelatorio(content, "Caixa " + data.format(FMT_DIA)));
        JButton btnFechar = UIFactory.bigActionButton("Fechar", e2 -> dlg.dispose());
        rodape.add(btnImprimir); rodape.add(btnFechar);
        dlg.add(rodape, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void imprimirRelatorio(JPanel panel, String titulo) {
        panel.setSize(panel.getPreferredSize());
        panel.doLayout();
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(titulo);
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double scale = Math.min(pf.getImageableWidth() / panel.getWidth(), pf.getImageableHeight() / panel.getHeight());
            if (scale < 1.0) g2.scale(scale, scale);
            panel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) { showError("Erro ao imprimir: " + ex.getMessage()); }
        }
    }

    // ── Reload de dados ───────────────────────────────────────────────────────

    private void verificarEstadoCaixa() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            LocalDate hoje = LocalDate.now();

            List<CaixaMovimento> movimentos = em.createQuery(
                    "SELECT m FROM CaixaMovimento m WHERE m.dataCaixa = :hoje ORDER BY m.dataMovimento ASC",
                    CaixaMovimento.class).setParameter("hoje", hoje).getResultList();

            // Percorre em ordem e acompanha o estado real — cada ABERTURA inicia novo turno
            BigDecimal aberturaAtual   = BigDecimal.ZERO;
            BigDecimal sangriasAtual   = BigDecimal.ZERO;
            BigDecimal suprimentoAtual = BigDecimal.ZERO;
            inicioTurnoAtual = null;
            boolean caixaEstaAberto = false;
            boolean algumFechamento = false;

            for (CaixaMovimento m : movimentos) {
                switch (m.getTipo()) {
                    case ABERTURA -> {
                        aberturaAtual   = m.getValor();
                        sangriasAtual   = BigDecimal.ZERO;
                        suprimentoAtual = BigDecimal.ZERO;
                        inicioTurnoAtual = m.getDataMovimento();
                        caixaEstaAberto = true;
                    }
                    case FECHAMENTO -> {
                        caixaEstaAberto = false;
                        algumFechamento = true;
                    }
                    case SANGRIA    -> sangriasAtual   = sangriasAtual.add(m.getValor());
                    case SUPRIMENTO -> suprimentoAtual = suprimentoAtual.add(m.getValor());
                    default -> {}
                }
            }

            caixaAberto = caixaEstaAberto;

            // Vendas APENAS do turno atual (a partir da última abertura)
            BigDecimal vendasTurno = BigDecimal.ZERO;
            if (inicioTurnoAtual != null) {
                vendasTurno = em.createQuery(
                                "SELECT COALESCE(SUM(p.valorPago), 0) FROM Pagamento p " +
                                        "WHERE p.dataPagamento >= :inicio",
                                BigDecimal.class)
                        .setParameter("inicio", inicioTurnoAtual)
                        .getSingleResult();
            }

            // Saldo do turno = abertura + vendas do turno + suprimentos - sangrias
            saldoAtual = aberturaAtual.add(vendasTurno).add(suprimentoAtual).add(sangriasAtual);

            if (caixaAberto) {
                lblStatus.setText("● CAIXA ABERTO");
                lblStatus.setForeground(new Color(0, 130, 0));
                lblSaldo.setText("Saldo: (visível após fechamento)");
                lblSaldo.setForeground(new Color(120, 120, 120));
            } else if (algumFechamento) {
                lblStatus.setText("● CAIXA FECHADO");
                lblStatus.setForeground(new Color(180, 0, 0));
                lblSaldo.setText("Saldo do turno: R$ " + String.format("%.2f", saldoAtual));
                lblSaldo.setForeground(new Color(0, 100, 0));
            } else {
                lblStatus.setText("● CAIXA NÃO ABERTO HOJE");
                lblStatus.setForeground(new Color(180, 100, 0));
                lblSaldo.setText("Saldo: R$ 0,00");
                lblSaldo.setForeground(new Color(120, 120, 120));
            }

            btnAbrirCaixa.setEnabled(!caixaAberto);
            btnFecharCaixa.setEnabled(caixaAberto);
            btnSangria.setEnabled(caixaAberto);
            btnSuprimento.setEnabled(caixaAberto);
            btnVerFechamento.setEnabled(algumFechamento);
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    private void reloadMovimentos() {
        movModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<CaixaMovimento> list = em.createQuery(
                    "SELECT m FROM CaixaMovimento m WHERE m.dataCaixa = :hoje ORDER BY m.dataMovimento ASC",
                    CaixaMovimento.class).setParameter("hoje", LocalDate.now()).getResultList();
            for (CaixaMovimento m : list) {
                String desc = m.getDescricao();
                if (desc != null && desc.startsWith("FECHAMENTO|")) desc = "Fechamento de caixa";

                // Enquanto o caixa está aberto, oculta o valor das vendas para o
                // operador não conseguir calcular o total e "acertar" o fechamento
                String valorExibido;
                if (caixaAberto && m.getTipo() == TipoCaixaMovimento.VENDA) {
                    valorExibido = "****";
                } else if (caixaAberto && m.getTipo() == TipoCaixaMovimento.ABERTURA) {
                    valorExibido = "****";
                } else {
                    valorExibido = String.format("R$ %.2f", m.getValor());
                }

                movModel.addRow(new Object[]{
                        m.getDataMovimento().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        m.getTipo().toString(), desc,
                        valorExibido
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    private void reloadResumo() {
        resumoModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();

            if (caixaAberto) {
                // Caixa aberto: mostra só contagem de vendas do turno atual
                long qtdVendas = 0;
                if (inicioTurnoAtual != null) {
                    qtdVendas = em.createQuery(
                            "SELECT COUNT(p) FROM Pagamento p WHERE p.dataPagamento >= :inicio",
                            Long.class).setParameter("inicio", inicioTurnoAtual).getSingleResult();
                }
                resumoModel.addRow(new Object[]{ "Vendas no turno", qtdVendas + " venda(s)" });
                resumoModel.addRow(new Object[]{ "──────────────────", "──────────" });
                resumoModel.addRow(new Object[]{ "Valores ocultos até o fechamento", "" });
                return;
            }

            // Caixa fechado: mostra resumo do turno encerrado
            // inicioTurnoAtual aponta para a abertura do último turno fechado
            if (inicioTurnoAtual == null) return;

            for (FormaPagamento fp : FormaPagamento.values()) {
                List<Pagamento> pags = em.createQuery(
                                "SELECT p FROM Pagamento p WHERE p.formaPagamento = :fp AND p.dataPagamento >= :inicio",
                                Pagamento.class)
                        .setParameter("fp", fp)
                        .setParameter("inicio", inicioTurnoAtual)
                        .getResultList();
                BigDecimal total = pags.stream().map(Pagamento::getValorPago).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(BigDecimal.ZERO) > 0)
                    resumoModel.addRow(new Object[]{ fp.toString(), String.format("R$ %.2f", total) });
            }
            BigDecimal totalVendas = em.createQuery(
                    "SELECT COALESCE(SUM(p.valorPago), 0) FROM Pagamento p WHERE p.dataPagamento >= :inicio",
                    BigDecimal.class).setParameter("inicio", inicioTurnoAtual).getSingleResult();
            resumoModel.addRow(new Object[]{ "──────────────", "──────────" });
            resumoModel.addRow(new Object[]{ "TOTAL VENDAS", String.format("R$ %.2f", totalVendas) });
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    private void reloadCaixasFechados() {
        reloadCaixasFechadosFiltrado(null, null);
    }

    private void reloadCaixasFechadosFiltrado(LocalDate filtroInicio, LocalDate filtroFim) {
        if (fechadosModel == null) return;
        fechadosModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<LocalDate> datas = em.createQuery(
                    "SELECT DISTINCT m.dataCaixa FROM CaixaMovimento m WHERE m.tipo = :tipo ORDER BY m.dataCaixa DESC",
                    LocalDate.class).setParameter("tipo", TipoCaixaMovimento.FECHAMENTO).getResultList();

            for (LocalDate d : datas) {
                // Aplica filtro de datas se informado
                if (filtroInicio != null && d.isBefore(filtroInicio)) continue;
                if (filtroFim   != null && d.isAfter(filtroFim))      continue;

                List<CaixaMovimento> movs = em.createQuery(
                        "SELECT m FROM CaixaMovimento m WHERE m.dataCaixa = :d ORDER BY m.dataMovimento ASC",
                        CaixaMovimento.class).setParameter("d", d).getResultList();

                // Quebra em turnos para calcular cada um separadamente
                java.util.List<CaixaMovimento> turno = null;
                for (CaixaMovimento m : movs) {
                    if (m.getTipo() == TipoCaixaMovimento.ABERTURA) {
                        turno = new java.util.ArrayList<>();
                    }
                    if (turno != null) turno.add(m);
                }
                // Usa o último turno fechado para exibir na tabela
                if (turno == null) continue;

                BigDecimal ab = BigDecimal.ZERO, sg = BigDecimal.ZERO,
                        sp = BigDecimal.ZERO, infVal = null, difVal = null;
                String status = "—";
                LocalDateTime horaAb = null, horaFech = null;

                for (CaixaMovimento m : turno) {
                    switch (m.getTipo()) {
                        case ABERTURA   -> { ab = m.getValor(); horaAb = m.getDataMovimento(); }
                        case SANGRIA    -> sg = sg.add(m.getValor().abs());
                        case SUPRIMENTO -> sp = sp.add(m.getValor());
                        case FECHAMENTO -> {
                            horaFech = m.getDataMovimento();
                            String desc = m.getDescricao();
                            if (desc != null && desc.startsWith("FECHAMENTO|")) {
                                try {
                                    for (String part : desc.split("\\|")) {
                                        if (part.startsWith("informado=")) infVal = new BigDecimal(part.substring(10));
                                        if (part.startsWith("diferenca=")) difVal = new BigDecimal(part.substring(10));
                                        if (part.startsWith("status="))    status = part.substring(7);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        default -> {}
                    }
                }

                if (horaAb == null) continue;

                // Vendas apenas do período deste turno
                LocalDateTime fimTurno = horaFech != null ? horaFech : LocalDateTime.now();
                BigDecimal vd = em.createQuery(
                                "SELECT COALESCE(SUM(p.valorPago), 0) FROM Pagamento p " +
                                        "WHERE p.dataPagamento >= :inicio AND p.dataPagamento <= :fim",
                                BigDecimal.class)
                        .setParameter("inicio", horaAb)
                        .setParameter("fim", fimTurno)
                        .getSingleResult();

                BigDecimal saldoSistema = ab.add(vd).add(sp).subtract(sg);

                String statusLabel = "CORRETO".equals(status)   ? "✅ Correto"   :
                        "DIFERENCA".equals(status) ? "⚠ Diferença" : "—";

                fechadosModel.addRow(new Object[]{
                        d.format(FMT_DIA),
                        String.format("R$ %.2f", ab),
                        String.format("R$ %.2f", vd),
                        String.format("R$ %.2f", sg),
                        String.format("R$ %.2f", sp),
                        String.format("R$ %.2f", saldoSistema),
                        infVal != null ? String.format("R$ %.2f", infVal) : "—",
                        difVal != null ? String.format("R$ %.2f", difVal) : "—",
                        statusLabel
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    // ── Chamado pelo PedidoPanel ──────────────────────────────────────────────

    public void registrarVenda(Pagamento pagamento) {
        if (!caixaAberto) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            CaixaMovimento mov = new CaixaMovimento();
            mov.setTipo(TipoCaixaMovimento.VENDA);
            mov.setValor(pagamento.getValorPago());
            mov.setPagamento(pagamento);
            mov.setDescricao("Venda - Pedido #" + pagamento.getPedido().getId() + " | " + pagamento.getFormaPagamento());
            em.persist(mov);
            em.getTransaction().commit();
            reloadCaixa();
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addLinha(JPanel p, String text, Font font, Color color, int align) {
        JLabel l = new JLabel(text, align);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private void addValor(JPanel p, String label, BigDecimal valor, Font font, Color cor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JLabel lbl = new JLabel(label); lbl.setFont(font);
        JLabel val = new JLabel(String.format("R$ %.2f", valor));
        val.setFont(font); val.setForeground(cor); val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(lbl, BorderLayout.WEST); row.add(val, BorderLayout.EAST);
        p.add(row);
    }

    private JSeparator sep() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(180, 180, 180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private void rollback(EntityManager em) { if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback(); }
    private void close(EntityManager em)    { if (em != null) em.close(); }
    private void showError(String msg)      { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}