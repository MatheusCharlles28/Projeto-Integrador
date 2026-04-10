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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmitirNotaPanel extends JPanel {

    private final EntityManagerFactory emf;
    private CaixaPanel caixaPanel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Pedidos pendentes
    private DefaultTableModel pendentesModel;
    private JTable pendentesTable;
    private Long pedidoSelecionadoId = null;

    // Itens
    private DefaultTableModel itensModel;
    private JTable itensTable;

    // Info e totais
    private JLabel lblPedidoInfo;
    private JLabel lblSubtotal;
    private JLabel lblDesconto;
    private JLabel lblTotal;

    // Desconto
    private JTextField tfDesconto;
    private JComboBox<String> cbTipoDesconto;
    private BigDecimal descontoAplicado = BigDecimal.ZERO;
    private BigDecimal subtotalAtual    = BigDecimal.ZERO;

    public void setCaixaPanel(CaixaPanel cp) { this.caixaPanel = cp; }

    public EmitirNotaPanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);
        build();
    }

    public void reloadPendentes() {
        pedidoSelecionadoId = null;
        itensModel.setRowCount(0);
        lblPedidoInfo.setText("Selecione um pedido pendente na lista acima");
        subtotalAtual = BigDecimal.ZERO;
        descontoAplicado = BigDecimal.ZERO;
        tfDesconto.setText("0");
        atualizarLabels();
        carregarPendentes();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Emitir Nota — Atendimento de Pedidos"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildPendentesPanel(), buildAtendimentoPanel());
        split.setDividerLocation(200);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        add(split, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildPendentesPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 4, 8));

        JLabel lbl = new JLabel("Pedidos Pendentes — Aguardando Atendimento");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(new Color(180, 60, 0));
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        pendentesModel = new DefaultTableModel(
                new String[]{"#", "Data", "Cliente", "Itens", "Total (R$)", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pendentesTable = UIFactory.styledTable();
        pendentesTable.setModel(pendentesModel);
        pendentesTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pendentesTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        pendentesTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        pendentesTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        pendentesTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        pendentesTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        pendentesTable.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(2, 5, 2, 5));
            cell.setBackground(isSelected ? UIFactory.XP_TABLE_SEL : new Color(255, 245, 220));
            cell.setForeground(isSelected ? Color.WHITE : Color.BLACK);
            if (col == 4) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            return cell;
        });

        pendentesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pendentesTable.getSelectedRow() >= 0) {
                Long id = (Long) pendentesModel.getValueAt(pendentesTable.getSelectedRow(), 0);
                carregarPedido(id);
            }
        });

        JScrollPane scroll = new JScrollPane(pendentesTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        JPanel rod = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rod.setBackground(new Color(212, 208, 200));
        rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        rod.add(UIFactory.bigActionButton("↻ Atualizar", e -> reloadPendentes()));
        p.add(rod, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildAtendimentoPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(4, 8, 8, 8));

        // ── Esquerda: itens ───────────────────────────────────────────────────
        JPanel esquerda = new JPanel(new BorderLayout(0, 4));
        esquerda.setBackground(UIFactory.XP_BG);
        esquerda.setBorder(UIFactory.groupBorder("Itens do Pedido"));

        lblPedidoInfo = new JLabel("Selecione um pedido pendente na lista acima");
        lblPedidoInfo.setFont(UIFactory.FONT_BOLD);
        lblPedidoInfo.setForeground(new Color(0, 84, 166));
        lblPedidoInfo.setBorder(new EmptyBorder(2, 4, 4, 4));
        esquerda.add(lblPedidoInfo, BorderLayout.NORTH);

        itensModel = new DefaultTableModel(
                new String[]{"ID", "Produto", "Cód. Barras", "Qtd", "Preço Unit.", "Subtotal"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itensTable = UIFactory.styledTable();
        itensTable.setModel(itensModel);
        itensTable.getColumnModel().getColumn(0).setMaxWidth(0);
        itensTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        itensTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        itensTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        itensTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        itensTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        itensTable.getColumnModel().getColumn(5).setPreferredWidth(90);

        JScrollPane scrollItens = new JScrollPane(itensTable);
        scrollItens.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        esquerda.add(scrollItens, BorderLayout.CENTER);
        p.add(esquerda, BorderLayout.CENTER);

        // ── Direita: desconto + totais + botões ───────────────────────────────
        JPanel direita = new JPanel(new BorderLayout(0, 6));
        direita.setBackground(UIFactory.XP_BG);
        direita.setPreferredSize(new Dimension(240, 0));

        // Desconto
        JPanel descontoPanel = new JPanel(new GridBagLayout());
        descontoPanel.setBackground(UIFactory.XP_BG);
        descontoPanel.setBorder(UIFactory.groupBorder("Desconto"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        descontoPanel.add(UIFactory.labelLight("Tipo:"), gc);
        cbTipoDesconto = new JComboBox<>(new String[]{"R$ (Reais)", "% (Percentual)"});
        cbTipoDesconto.setFont(UIFactory.FONT_NORMAL);
        gc.gridx = 1; gc.weightx = 1; descontoPanel.add(cbTipoDesconto, gc);

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        descontoPanel.add(UIFactory.labelLight("Valor:"), gc);
        tfDesconto = UIFactory.styledField("0");
        gc.gridx = 1; gc.weightx = 1; descontoPanel.add(tfDesconto, gc);

        JButton btnAplicar = UIFactory.bigActionButton("✔ Aplicar", e -> aplicarDesconto());
        btnAplicar.setBackground(new Color(0, 84, 166)); btnAplicar.setForeground(Color.WHITE);
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2; descontoPanel.add(btnAplicar, gc);
        tfDesconto.addActionListener(e -> aplicarDesconto());

        direita.add(descontoPanel, BorderLayout.NORTH);

        // Totais
        JPanel totaisPanel = new JPanel(new GridBagLayout());
        totaisPanel.setBackground(UIFactory.XP_BG);
        totaisPanel.setBorder(UIFactory.groupBorder("Resumo"));
        GridBagConstraints gt = new GridBagConstraints();
        gt.insets = new Insets(4, 8, 4, 8);
        gt.fill = GridBagConstraints.HORIZONTAL;

        lblSubtotal = new JLabel("R$ 0,00");
        lblSubtotal.setFont(UIFactory.FONT_NORMAL);
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        gt.gridx = 0; gt.gridy = 0; gt.weightx = 0; totaisPanel.add(UIFactory.labelLight("Subtotal:"), gt);
        gt.gridx = 1; gt.weightx = 1; totaisPanel.add(lblSubtotal, gt);

        lblDesconto = new JLabel("R$ 0,00");
        lblDesconto.setFont(UIFactory.FONT_BOLD);
        lblDesconto.setForeground(new Color(180, 0, 0));
        lblDesconto.setHorizontalAlignment(SwingConstants.RIGHT);
        gt.gridx = 0; gt.gridy = 1; gt.weightx = 0; totaisPanel.add(UIFactory.labelLight("Desconto:"), gt);
        gt.gridx = 1; gt.weightx = 1; totaisPanel.add(lblDesconto, gt);

        totaisPanel.add(new JSeparator(), new GridBagConstraints() {{
            gridx = 0; gridy = 2; gridwidth = 2; fill = HORIZONTAL;
            insets = new Insets(2, 8, 2, 8);
        }});

        lblTotal = new JLabel("R$ 0,00");
        lblTotal.setFont(new Font("Tahoma", Font.BOLD, 16));
        lblTotal.setForeground(new Color(0, 110, 0));
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        gt.gridx = 0; gt.gridy = 3; gt.weightx = 0; totaisPanel.add(new JLabel("TOTAL:") {{ setFont(UIFactory.FONT_BOLD); }}, gt);
        gt.gridx = 1; gt.weightx = 1; totaisPanel.add(lblTotal, gt);

        direita.add(totaisPanel, BorderLayout.CENTER);

        // Botões
        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(0, 4, 4, 4));

        JButton btnEmitir = UIFactory.bigActionButton("✔ Emitir Nota / Pagar", e -> emitirNota());
        btnEmitir.setBackground(new Color(0, 110, 0)); btnEmitir.setForeground(Color.WHITE);
        btnEmitir.setFont(new Font("Tahoma", Font.BOLD, 12));

        JButton btnCancelar = UIFactory.bigActionButton("✖ Cancelar Pedido", e -> cancelarPedido());
        btnCancelar.setBackground(new Color(180, 0, 0)); btnCancelar.setForeground(Color.WHITE);

        btnPanel.add(btnEmitir);
        btnPanel.add(btnCancelar);
        direita.add(btnPanel, BorderLayout.SOUTH);

        p.add(direita, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        JLabel hint = new JLabel("Dica: Selecione um pedido → aplique desconto se necessário → Emitir Nota para cobrar e débitar estoque.");
        hint.setFont(new Font("Tahoma", Font.ITALIC, 10));
        hint.setForeground(new Color(80, 80, 80));
        bar.add(hint);
        return bar;
    }

    // ── Carregar dados ────────────────────────────────────────────────────────

    private void carregarPendentes() {
        pendentesModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Pedido> list = em.createQuery(
                    "SELECT p FROM Pedido p LEFT JOIN FETCH p.cliente WHERE p.status = :s ORDER BY p.dataPedido ASC",
                    Pedido.class).setParameter("s", StatusPedido.PENDENTE).getResultList();
            for (Pedido ped : list) {
                Long qtd = em.createQuery("SELECT COUNT(i) FROM ItensPedido i WHERE i.pedido.id = :id", Long.class)
                        .setParameter("id", ped.getId()).getSingleResult();
                pendentesModel.addRow(new Object[]{
                        ped.getId(), ped.getDataPedido().format(FMT),
                        ped.getCliente().getNome(), qtd,
                        String.format("R$ %.2f", ped.getValorTotal() != null ? ped.getValorTotal() : BigDecimal.ZERO),
                        ped.getStatus().toString()
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    private void carregarPedido(Long pedidoId) {
        pedidoSelecionadoId = pedidoId;
        itensModel.setRowCount(0);
        descontoAplicado = BigDecimal.ZERO;
        tfDesconto.setText("0");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Pedido ped = em.createQuery(
                    "SELECT p FROM Pedido p LEFT JOIN FETCH p.itensPedidos i LEFT JOIN FETCH i.produto LEFT JOIN FETCH p.cliente WHERE p.id = :id",
                    Pedido.class).setParameter("id", pedidoId).getSingleResult();

            lblPedidoInfo.setText("Pedido #" + ped.getId() + " — " + ped.getCliente().getNome()
                    + "  |  " + ped.getDataPedido().format(FMT));

            subtotalAtual = BigDecimal.ZERO;
            for (ItensPedido item : ped.getItensPedidos()) {
                BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                subtotalAtual = subtotalAtual.add(sub);
                itensModel.addRow(new Object[]{
                        item.getId(),
                        item.getProduto().getNome(),
                        item.getProduto().getCodBarras() != null ? item.getProduto().getCodBarras() : "—",
                        item.getQuantidade(),
                        String.format("R$ %.2f", item.getPrecoUnitario()),
                        String.format("R$ %.2f", sub)
                });
            }
            atualizarLabels();
        } catch (Exception ex) { ex.printStackTrace(); showError("Erro ao carregar pedido: " + ex.getMessage()); }
        finally { close(em); }
    }

    // ── Desconto ──────────────────────────────────────────────────────────────

    private void aplicarDesconto() {
        if (pedidoSelecionadoId == null) return;
        try {
            BigDecimal val = Validator.parseBigDecimal(tfDesconto.getText());
            if (val.compareTo(BigDecimal.ZERO) < 0) { showError("Desconto não pode ser negativo."); return; }

            boolean percentual = cbTipoDesconto.getSelectedIndex() == 1;
            if (percentual) {
                if (val.compareTo(new BigDecimal("100")) > 0) { showError("Desconto não pode ser maior que 100%."); return; }
                descontoAplicado = subtotalAtual.multiply(val).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            } else {
                if (val.compareTo(subtotalAtual) > 0) { showError("Desconto não pode ser maior que o subtotal."); return; }
                descontoAplicado = val;
            }
            atualizarLabels();
        } catch (Exception e) { showError("Valor de desconto inválido."); }
    }

    private void atualizarLabels() {
        BigDecimal totalFinal = subtotalAtual.subtract(descontoAplicado).max(BigDecimal.ZERO);
        lblSubtotal.setText(String.format("R$ %.2f", subtotalAtual));
        lblDesconto.setText(descontoAplicado.compareTo(BigDecimal.ZERO) > 0
                ? "- R$ " + String.format("%.2f", descontoAplicado) : "R$ 0,00");
        lblTotal.setText(String.format("R$ %.2f", totalFinal));
        lblTotal.setForeground(descontoAplicado.compareTo(BigDecimal.ZERO) > 0
                ? new Color(0, 130, 0) : new Color(0, 84, 166));
    }

    // ── Emitir Nota ───────────────────────────────────────────────────────────

    private void emitirNota() {
        if (pedidoSelecionadoId == null) { JOptionPane.showMessageDialog(this, "Selecione um pedido pendente.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        if (itensModel.getRowCount() == 0) { showError("O pedido não tem itens."); return; }
        if (caixaPanel == null || !caixaPanel.isCaixaAberto()) {
            JOptionPane.showMessageDialog(this, "O caixa está fechado!\nAbra o caixa antes de emitir a nota.", "Caixa Fechado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal totalFinal = subtotalAtual.subtract(descontoAplicado).max(BigDecimal.ZERO);

        // Diálogo de pagamento
        JPanel dlg = new JPanel(new GridBagLayout());
        dlg.setBackground(UIFactory.XP_BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8); gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblSub2 = new JLabel("Subtotal: R$ " + String.format("%.2f", subtotalAtual));
        lblSub2.setFont(UIFactory.FONT_NORMAL);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; dlg.add(lblSub2, gc);

        if (descontoAplicado.compareTo(BigDecimal.ZERO) > 0) {
            JLabel lblDesc2 = new JLabel("Desconto: - R$ " + String.format("%.2f", descontoAplicado));
            lblDesc2.setFont(UIFactory.FONT_BOLD); lblDesc2.setForeground(new Color(180, 0, 0));
            gc.gridy = 1; dlg.add(lblDesc2, gc);
        }

        JLabel lblTot2 = new JLabel("TOTAL: R$ " + String.format("%.2f", totalFinal));
        lblTot2.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblTot2.setForeground(new Color(0, 110, 0));
        gc.gridy = 2; dlg.add(lblTot2, gc);

        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 3; dlg.add(UIFactory.labelLight("Forma de pagamento:"), gc);
        JComboBox<FormaPagamento> cbForma = new JComboBox<>(FormaPagamento.values());
        cbForma.setFont(UIFactory.FONT_NORMAL);
        gc.gridx = 1; dlg.add(cbForma, gc);

        gc.gridx = 0; gc.gridy = 4; dlg.add(UIFactory.labelLight("Valor recebido (R$):"), gc);
        JTextField tfPago = UIFactory.styledField(String.format("%.2f", totalFinal));
        gc.gridx = 1; dlg.add(tfPago, gc);

        JLabel lblTroco = new JLabel("Troco: R$ 0,00");
        lblTroco.setFont(UIFactory.FONT_BOLD); lblTroco.setForeground(new Color(0, 100, 0));
        gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 2; dlg.add(lblTroco, gc);

        final BigDecimal tf = totalFinal;
        tfPago.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void up() {
                try {
                    BigDecimal pago = Validator.parseBigDecimal(tfPago.getText());
                    BigDecimal tr = pago.subtract(tf);
                    lblTroco.setText("Troco: R$ " + String.format("%.2f", tr.max(BigDecimal.ZERO)));
                    lblTroco.setForeground(tr.compareTo(BigDecimal.ZERO) >= 0 ? new Color(0,100,0) : new Color(180,0,0));
                } catch (Exception e) { lblTroco.setText("Troco: -"); }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { up(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { up(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { up(); }
        });

        int res = JOptionPane.showConfirmDialog(this, dlg, "Registrar Pagamento", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        FormaPagamento forma = (FormaPagamento) cbForma.getSelectedItem();
        BigDecimal valorPago;
        try {
            valorPago = Validator.parseBigDecimal(tfPago.getText());
            if (valorPago.compareTo(BigDecimal.ZERO) <= 0) { showError("Valor inválido."); return; }
            if (valorPago.compareTo(totalFinal) < 0) { showError("Valor menor que o total."); return; }
        } catch (Exception e) { showError("Valor inválido."); return; }

        BigDecimal troco = valorPago.subtract(totalFinal);

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            Pedido ped = em.find(Pedido.class, pedidoSelecionadoId);
            if (ped == null) { em.getTransaction().rollback(); showError("Pedido não encontrado."); return; }

            // Remove itens antigos e recria
            em.createQuery("DELETE FROM ItensPedido i WHERE i.pedido.id = :id")
                    .setParameter("id", pedidoSelecionadoId).executeUpdate();
            ped.getItensPedidos().clear();

            BigDecimal totalReal = BigDecimal.ZERO;
            for (int i = 0; i < itensModel.getRowCount(); i++) {
                String nomeProd = itensModel.getValueAt(i, 1).toString();
                int qtd = Integer.parseInt(itensModel.getValueAt(i, 3).toString());
                BigDecimal preco = Validator.parseBigDecimal(
                        itensModel.getValueAt(i, 4).toString().replace("R$ ", ""));

                List<Produto> prods = em.createQuery("SELECT p FROM Produto p WHERE p.nome = :n", Produto.class)
                        .setParameter("n", nomeProd).getResultList();
                if (prods.isEmpty()) { em.getTransaction().rollback(); showError("Produto não encontrado: " + nomeProd); return; }
                Produto pm = prods.get(0);

                int estAtual = pm.getEstoque() != null ? pm.getEstoque() : 0;
                if (estAtual < qtd) {
                    em.getTransaction().rollback();
                    showError("Estoque insuficiente: " + pm.getNome() + "\nDisponível: " + estAtual + " | Necessário: " + qtd);
                    return;
                }
                pm.setEstoque(estAtual - qtd);
                em.merge(pm);

                ItensPedido item = new ItensPedido();
                item.setPedido(ped); item.setProduto(pm);
                item.setQuantidade(qtd); item.setPrecoUnitario(preco);
                item.calcularSubtotal();
                totalReal = totalReal.add(item.getSubtotal());
                em.persist(item);
                ped.adicionarItem(item);
            }

            ped.setValorTotal(totalReal);
            ped.setStatus(StatusPedido.CONFIRMADO);
            em.merge(ped);

            // Pagamento com desconto e valor final
            Pagamento pag = new Pagamento();
            pag.setPedido(ped);
            pag.setFormaPagamento(forma);
            pag.setValorPago(valorPago);
            pag.setDesconto(descontoAplicado);
            pag.setValorFinal(totalFinal);  // valor real cobrado com desconto
            pag.setTroco(troco);
            pag.setDataPagamento(LocalDateTime.now());
            em.persist(pag);

            em.getTransaction().commit();

            if (caixaPanel != null) caixaPanel.registrarVenda(pag);

            String msg = "✅ Nota emitida!\nPedido #" + ped.getId() + " — " + ped.getCliente().getNome()
                    + "\nSubtotal: R$ " + String.format("%.2f", totalReal);
            if (descontoAplicado.compareTo(BigDecimal.ZERO) > 0)
                msg += "\nDesconto: - R$ " + String.format("%.2f", descontoAplicado);
            msg += "\nTotal cobrado: R$ " + String.format("%.2f", totalFinal)
                    + "\nTroco: R$ " + String.format("%.2f", troco);
            JOptionPane.showMessageDialog(this, msg, "Nota Emitida", JOptionPane.INFORMATION_MESSAGE);

            reloadPendentes();

        } catch (Exception ex) { rollback(em); showError("Erro ao emitir nota: " + ex.getMessage()); ex.printStackTrace(); }
        finally { close(em); }
    }

    private void cancelarPedido() {
        if (pedidoSelecionadoId == null) { JOptionPane.showMessageDialog(this, "Selecione um pedido."); return; }
        int conf = JOptionPane.showConfirmDialog(this,
                "Confirma o CANCELAMENTO do Pedido #" + pedidoSelecionadoId + "?\nEsta ação não pode ser desfeita.",
                "Cancelar Pedido", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Pedido ped = em.find(Pedido.class, pedidoSelecionadoId);
            if (ped != null) { ped.setStatus(StatusPedido.CANCELADO); em.merge(ped); }
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Pedido #" + pedidoSelecionadoId + " cancelado.");
            reloadPendentes();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); }
        finally { close(em); }
    }

    private void rollback(EntityManager em) { if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback(); }
    private void close(EntityManager em)    { if (em != null) em.close(); }
    private void showError(String msg)      { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}