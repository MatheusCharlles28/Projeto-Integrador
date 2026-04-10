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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel de Notas de Transferência de Estoque (não fiscal).
 * Permite emitir entradas (recebimento de fornecedor) e saídas (transferência/ajuste).
 */
public class NotaTransferenciaPanel extends BackgroundPanel {

    private final EntityManagerFactory emf;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Campos do cabeçalho da nota ───────────────────────────────────────────
    private JComboBox<TipoNota>    cbTipo;
    private JComboBox<Object>      cbFornecedor; // Fornecedor ou "— Nenhum —"
    private JTextField             tfNumeroNota;
    private JTextArea              taObs;
    private JLabel                 lblTotalNota;

    // ── Itens da nota (em edição) ─────────────────────────────────────────────
    private final DefaultListModel<ItemNota> itensModel = new DefaultListModel<>();
    private JList<ItemNota>                  itensList;

    // ── Tabela de notas emitidas ──────────────────────────────────────────────
    private DefaultTableModel notasModel;
    private JTable            notasTable;

    // ── Filtros ───────────────────────────────────────────────────────────────
    private JTextField tfFiltroData;
    private JComboBox<Object> cbFiltroTipo;

    public NotaTransferenciaPanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());


        build();
    }

    // ── Montagem UI ───────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Notas de Transferência de Estoque"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIFactory.FONT_BOLD);
        tabs.setBackground(UIFactory.XP_BG);
        tabs.addTab("Nova Nota", buildNovaNota());
        tabs.addTab("Notas Emitidas", buildNotasEmitidas());

        // Ao entrar na aba de notas emitidas, recarrega
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 1) reloadNotas(); });

        add(tabs, BorderLayout.CENTER);
    }

    // ── Aba: Nova Nota ────────────────────────────────────────────────────────

    private JPanel buildNovaNota() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildCabecalhoPanel(), buildItensPanel());
        split.setDividerLocation(340);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        p.add(split, BorderLayout.CENTER);

        // Rodapé com total e botão emitir
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setBackground(new Color(212, 208, 200));
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        lblTotalNota = new JLabel("Total: R$ 0,00");
        lblTotalNota.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblTotalNota.setForeground(new Color(0, 84, 166));
        lblTotalNota.setBorder(new EmptyBorder(6, 12, 6, 12));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        btnPanel.setOpaque(false);
        JButton btnLimpar = UIFactory.bigActionButton("Limpar", e -> limparNota());
        JButton btnEmitir = UIFactory.bigActionButton("✔ Emitir Nota", e -> emitirNota());
        btnEmitir.setBackground(new Color(0, 110, 0));
        btnEmitir.setForeground(Color.WHITE);
        btnPanel.add(btnLimpar);
        btnPanel.add(btnEmitir);

        rodape.add(lblTotalNota, BorderLayout.WEST);
        rodape.add(btnPanel, BorderLayout.EAST);
        p.add(rodape, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildCabecalhoPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(UIFactory.groupBorder("Cabeçalho da Nota"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 8, 5, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Tipo
        cbTipo = new JComboBox<>(TipoNota.values());
        cbTipo.setFont(UIFactory.FONT_NORMAL);
        addFormRow(form, c, row++, "Tipo:", cbTipo);

        // Fornecedor
        cbFornecedor = new JComboBox<>();
        cbFornecedor.setFont(UIFactory.FONT_NORMAL);
        carregarFornecedores_interno();
        addFormRow(form, c, row++, "Fornecedor:", cbFornecedor);

        // Número da nota
        tfNumeroNota = UIFactory.styledField("");
        tfNumeroNota.setToolTipText("Número de referência (opcional)");
        addFormRow(form, c, row++, "Nº Referência:", tfNumeroNota);

        // Observações
        taObs = UIFactory.styledTextArea(4, 18);
        JScrollPane obsScroll = new JScrollPane(taObs);
        obsScroll.setPreferredSize(new Dimension(0, 80));
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("Observações:"), c);
        c.gridx = 1; c.weightx = 1; c.gridwidth = 1;
        form.add(obsScroll, c); row++;

        // Instrução
        JLabel instrucao = new JLabel("<html><i>ENTRADA: adiciona ao estoque<br>SAÍDA: retira do estoque</i></html>");
        instrucao.setFont(new Font("Tahoma", Font.ITALIC, 10));
        instrucao.setForeground(new Color(100, 100, 100));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(instrucao, c); row++;

        // Glue
        c.gridy = row; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildItensPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel lbl = new JLabel("Itens da Nota");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(new Color(0, 84, 166));
        p.add(lbl, BorderLayout.NORTH);

        // Lista de itens adicionados
        itensList = new JList<>(itensModel);
        itensList.setFont(UIFactory.FONT_NORMAL);
        itensList.setCellRenderer((list, item, index, isSelected, cellHasFocus) -> {
            String texto = String.format("%-30s  Qtd: %d  Custo: R$ %.2f  Sub: R$ %.2f",
                    item.getProduto().getNome(),
                    item.getQuantidade(),
                    item.getPrecoUnitario() != null ? item.getPrecoUnitario() : BigDecimal.ZERO,
                    item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO);
            JLabel l = new JLabel(texto);
            l.setFont(new Font("Monospaced", Font.PLAIN, 11));
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(2, 6, 2, 6));
            l.setBackground(isSelected ? UIFactory.XP_TABLE_SEL : (index % 2 == 0 ? Color.WHITE : new Color(248, 248, 252)));
            l.setForeground(isSelected ? Color.WHITE : Color.BLACK);
            return l;
        });

        JScrollPane scroll = new JScrollPane(itensList);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        // Botões de item
        JPanel btnItens = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btnItens.setBackground(UIFactory.XP_BG);
        JButton btnAdd = UIFactory.bigActionButton("+ Adicionar Item", e -> adicionarItem());
        btnAdd.setBackground(new Color(0, 84, 166)); btnAdd.setForeground(Color.WHITE);
        JButton btnRem = UIFactory.bigActionButton("− Remover", e -> removerItem());
        btnItens.add(btnAdd); btnItens.add(btnRem);
        p.add(btnItens, BorderLayout.SOUTH);

        return p;
    }

    // ── Aba: Notas Emitidas ───────────────────────────────────────────────────

    private JPanel buildNotasEmitidas() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filtros.setBackground(UIFactory.XP_PANEL_BG);
        filtros.setBorder(UIFactory.groupBorder("Filtros"));

        filtros.add(UIFactory.labelLight("Data (dd/MM/yyyy):"));
        tfFiltroData = UIFactory.styledField("");
        tfFiltroData.setPreferredSize(new Dimension(100, 22));
        filtros.add(tfFiltroData);

        filtros.add(UIFactory.labelLight("Tipo:"));
        cbFiltroTipo = new JComboBox<>(new Object[]{"Todos", TipoNota.ENTRADA, TipoNota.SAIDA});
        cbFiltroTipo.setFont(UIFactory.FONT_NORMAL);
        filtros.add(cbFiltroTipo);

        JButton btnBuscar = UIFactory.bigActionButton("🔍 Buscar", e -> reloadNotas());
        btnBuscar.setBackground(new Color(0, 84, 166)); btnBuscar.setForeground(Color.WHITE);
        JButton btnLimpar = UIFactory.bigActionButton("Limpar", e -> { tfFiltroData.setText(""); cbFiltroTipo.setSelectedIndex(0); reloadNotas(); });
        filtros.add(btnBuscar); filtros.add(btnLimpar);
        p.add(filtros, BorderLayout.NORTH);

        // Tabela de notas
        notasModel = new DefaultTableModel(
                new String[]{"ID", "Data", "Tipo", "Fornecedor", "Nº Ref.", "Itens", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        notasTable = UIFactory.styledTable();
        notasTable.setModel(notasModel);
        notasTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        notasTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        notasTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        notasTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        notasTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        notasTable.getColumnModel().getColumn(5).setPreferredWidth(50);
        notasTable.getColumnModel().getColumn(6).setPreferredWidth(90);

        // Colorir por tipo
        notasTable.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 5, 1, 5));
            if (isSelected) {
                cell.setBackground(UIFactory.XP_TABLE_SEL); cell.setForeground(Color.WHITE);
            } else {
                String tipo = notasModel.getValueAt(row, 2) != null ? notasModel.getValueAt(row, 2).toString() : "";
                cell.setBackground("Entrada".equals(tipo)
                        ? new Color(220, 240, 220)
                        : new Color(255, 235, 220));
                cell.setForeground(Color.BLACK);
                if (col == 6) cell.setHorizontalAlignment(SwingConstants.RIGHT);
            }
            return cell;
        });

        // Duplo clique abre relatório
        notasTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && notasTable.getSelectedRow() >= 0)
                    abrirRelatorioNota((Long) notasModel.getValueAt(notasTable.getSelectedRow(), 0));
            }
        });

        JScrollPane scroll = new JScrollPane(notasTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);

        // Rodapé
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rodape.setBackground(new Color(212, 208, 200));
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        rodape.add(UIFactory.bigActionButton("↻ Atualizar", e -> reloadNotas()));
        JButton btnVer = UIFactory.bigActionButton("🖨 Ver / Imprimir", e -> {
            if (notasTable.getSelectedRow() < 0) { JOptionPane.showMessageDialog(p, "Selecione uma nota."); return; }
            abrirRelatorioNota((Long) notasModel.getValueAt(notasTable.getSelectedRow(), 0));
        });
        btnVer.setBackground(new Color(0, 84, 166)); btnVer.setForeground(Color.WHITE);
        rodape.add(btnVer);
        JLabel dica = new JLabel("  Dica: clique duplo na linha para abrir o relatório");
        dica.setFont(new Font("Tahoma", Font.ITALIC, 10));
        dica.setForeground(new Color(100, 100, 100));
        rodape.add(dica);
        p.add(rodape, BorderLayout.SOUTH);

        return p;
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    private void adicionarItem() {
        // Busca produtos disponíveis
        EntityManager em = null;
        List<Produto> produtos;
        try {
            em = emf.createEntityManager();
            produtos = em.createQuery("SELECT p FROM Produto p ORDER BY p.nome", Produto.class).getResultList();
        } catch (Exception ex) { ex.printStackTrace(); return; }
        finally { close(em); }

        if (produtos.isEmpty()) { JOptionPane.showMessageDialog(this, "Nenhum produto cadastrado."); return; }

        // Diálogo de seleção de produto e quantidade
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Adicionar Item", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(420, 210);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Produto> cbProd = new JComboBox<>(produtos.toArray(new Produto[0]));
        cbProd.setFont(UIFactory.FONT_NORMAL);
        // Renderer mostra: Nome  (Estoque: X | Custo: R$ Y)
        cbProd.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel l = new JLabel();
            l.setOpaque(true);
            l.setFont(UIFactory.FONT_NORMAL);
            l.setBorder(new EmptyBorder(2, 6, 2, 6));
            if (value != null) {
                int est = value.getEstoque() != null ? value.getEstoque() : 0;
                String custo = value.getPrecoCusto() != null
                        ? String.format("R$ %.2f", value.getPrecoCusto()) : "—";
                l.setText(String.format("<html><b>%s</b>  <font color='gray'>Estoque: %d | Custo: %s</font></html>",
                        value.getNome(), est, custo));
                // Vermelho se sem estoque (para saídas)
                l.setBackground(isSelected ? new Color(0, 84, 166)
                        : est <= 0 ? new Color(255, 230, 230) : Color.WHITE);
                l.setForeground(isSelected ? Color.WHITE : Color.BLACK);
            }
            return l;
        });
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0; form.add(UIFactory.labelLight("Produto:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(cbProd, gc);

        JTextField tfQtd = UIFactory.styledField("1");
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; form.add(UIFactory.labelLight("Quantidade:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(tfQtd, gc);

        JTextField tfCusto = UIFactory.styledField("");
        tfCusto.setToolTipText("Preço de custo unitário (opcional para saídas)");
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; form.add(UIFactory.labelLight("Custo unitário (R$):"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(tfCusto, gc);

        // Auto-preenche custo do produto ao selecionar
        cbProd.addActionListener(e -> {
            Produto sel = (Produto) cbProd.getSelectedItem();
            if (sel != null && sel.getPrecoCusto() != null)
                tfCusto.setText(String.format("%.2f", sel.getPrecoCusto()));
            else if (sel != null && sel.getPrecoMedio() != null)
                tfCusto.setText(String.format("%.2f", sel.getPrecoMedio()));
        });
        // Dispara uma vez
        if (!produtos.isEmpty() && produtos.get(0).getPrecoCusto() != null)
            tfCusto.setText(String.format("%.2f", produtos.get(0).getPrecoCusto()));

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Adicionar", e -> { ok[0] = true; dlg.dispose(); });
        btnOk.setBackground(new Color(0, 100, 0)); btnOk.setForeground(Color.WHITE);
        btns.add(btnOk); btns.add(UIFactory.bigActionButton("Cancelar", e -> dlg.dispose()));
        dlg.add(btns, BorderLayout.SOUTH);
        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfQtd::requestFocusInWindow);
        dlg.setVisible(true);

        if (!ok[0]) return;

        Produto prodSel = (Produto) cbProd.getSelectedItem();
        if (prodSel == null) return;

        int qtd;
        try {
            qtd = Integer.parseInt(tfQtd.getText().trim());
            if (qtd <= 0 || qtd > 9999) { showError("Quantidade deve ser entre 1 e 9999."); return; }
        } catch (Exception e) { showError("Quantidade inválida."); return; }

        // Para SAÍDA, verifica estoque disponível
        if (cbTipo.getSelectedItem() == TipoNota.SAIDA) {
            int estDisp = prodSel.getEstoque() != null ? prodSel.getEstoque() : 0;
            // Desconta itens já adicionados na mesma nota
            for (int i = 0; i < itensModel.size(); i++) {
                if (itensModel.get(i).getProduto().getId().equals(prodSel.getId()))
                    estDisp -= itensModel.get(i).getQuantidade();
            }
            if (estDisp < qtd) {
                showError("Estoque insuficiente para " + prodSel.getNome()
                        + "\nDisponível: " + estDisp + " | Solicitado: " + qtd);
                return;
            }
        }

        BigDecimal custo = BigDecimal.ZERO;
        if (!Validator.isBlank(tfCusto.getText())) {
            try { custo = Validator.parseBigDecimal(tfCusto.getText()); }
            catch (Exception e) { showError("Custo unitário inválido."); return; }
        }

        ItemNota item = new ItemNota();
        item.setProduto(prodSel);
        item.setQuantidade(qtd);
        item.setPrecoUnitario(custo);
        item.calcularSubtotal();
        itensModel.addElement(item);
        atualizarTotalNota();
    }

    private void removerItem() {
        int sel = itensList.getSelectedIndex();
        if (sel >= 0) { itensModel.remove(sel); atualizarTotalNota(); }
        else JOptionPane.showMessageDialog(this, "Selecione um item para remover.");
    }

    private void atualizarTotalNota() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < itensModel.size(); i++) {
            BigDecimal sub = itensModel.get(i).getSubtotal();
            if (sub != null) total = total.add(sub);
        }
        lblTotalNota.setText("Total: R$ " + String.format("%.2f", total));
    }

    private void limparNota() {
        itensModel.clear();
        cbTipo.setSelectedIndex(0);
        cbFornecedor.setSelectedIndex(0);
        tfNumeroNota.setText("");
        taObs.setText("");
        lblTotalNota.setText("Total: R$ 0,00");
    }

    private void emitirNota() {
        if (itensModel.isEmpty()) { showError("Adicione pelo menos um item à nota."); return; }

        TipoNota tipo = (TipoNota) cbTipo.getSelectedItem();
        String tipoLabel = tipo == TipoNota.ENTRADA ? "entrada" : "saída";

        int conf = JOptionPane.showConfirmDialog(this,
                "Confirma a emissão da nota de " + tipoLabel + " com " + itensModel.size() + " item(ns)?\n\n" +
                        (tipo == TipoNota.ENTRADA ? "O estoque será ADICIONADO." : "O estoque será RETIRADO."),
                "Confirmar Emissão", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            NotaTransferencia nota = new NotaTransferencia();
            nota.setTipo(tipo);
            nota.setDataNota(LocalDateTime.now());
            nota.setNumeroNota(tfNumeroNota.getText().trim());
            nota.setObservacoes(taObs.getText().trim());

            Object fornSel = cbFornecedor.getSelectedItem();
            if (fornSel instanceof Fornecedor) {
                Fornecedor fManaged = em.find(Fornecedor.class, ((Fornecedor) fornSel).getId());
                nota.setFornecedor(fManaged);
            }
            em.persist(nota);

            List<ItemNota> itensConfirmados = new ArrayList<>();
            for (int i = 0; i < itensModel.size(); i++) {
                ItemNota orig = itensModel.get(i);
                Produto pm = em.find(Produto.class, orig.getProduto().getId());
                if (pm == null) { em.getTransaction().rollback(); showError("Produto não encontrado: " + orig.getProduto().getNome()); return; }

                // Ajusta estoque
                int estoqueAtual = pm.getEstoque() != null ? pm.getEstoque() : 0;
                if (tipo == TipoNota.ENTRADA) {
                    pm.setEstoque(estoqueAtual + orig.getQuantidade());

                    // Atualiza preço médio ponderado se custo informado
                    if (orig.getPrecoUnitario() != null && orig.getPrecoUnitario().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal novoCusto  = orig.getPrecoUnitario();
                        BigDecimal medioAnt   = pm.getPrecoMedio();
                        BigDecimal qtdHist    = pm.getQtdHistoricoCusto();
                        BigDecimal pesoEntrada= BigDecimal.valueOf(orig.getQuantidade());

                        if (medioAnt == null || qtdHist == null || qtdHist.compareTo(BigDecimal.ZERO) == 0) {
                            pm.setPrecoMedio(novoCusto);
                            pm.setQtdHistoricoCusto(pesoEntrada);
                        } else {
                            BigDecimal novaQtd   = qtdHist.add(pesoEntrada);
                            BigDecimal novoMedio = qtdHist.multiply(medioAnt)
                                    .add(pesoEntrada.multiply(novoCusto))
                                    .divide(novaQtd, 2, java.math.RoundingMode.HALF_UP);
                            pm.setPrecoMedio(novoMedio);
                            pm.setQtdHistoricoCusto(novaQtd);
                        }
                        pm.setPrecoCusto(novoCusto); // atualiza custo atual também
                    }
                } else {
                    // SAÍDA — valida estoque
                    if (estoqueAtual < orig.getQuantidade()) {
                        em.getTransaction().rollback();
                        showError("Estoque insuficiente para: " + pm.getNome()
                                + "\nDisponível: " + estoqueAtual + " | Necessário: " + orig.getQuantidade());
                        return;
                    }
                    pm.setEstoque(estoqueAtual - orig.getQuantidade());
                }
                em.merge(pm);

                ItemNota item = new ItemNota();
                item.setNota(nota);
                item.setProduto(pm);
                item.setQuantidade(orig.getQuantidade());
                item.setPrecoUnitario(orig.getPrecoUnitario());
                item.calcularSubtotal();
                em.persist(item);
                itensConfirmados.add(item);
                nota.getItens().add(item);
            }

            nota.calcularTotal();
            em.merge(nota);
            em.getTransaction().commit();

            JOptionPane.showMessageDialog(this,
                    "Nota #" + nota.getId() + " emitida com sucesso!\n" +
                            tipo + " de " + itensConfirmados.size() + " produto(s).",
                    "Nota Emitida", JOptionPane.INFORMATION_MESSAGE);

            // Abre relatório da nota recém-emitida
            Long notaId = nota.getId();
            limparNota();
            abrirRelatorioNota(notaId);

        } catch (Exception ex) {
            rollback(em); showError("Erro ao emitir nota: " + ex.getMessage()); ex.printStackTrace();
        } finally { close(em); }
    }

    // ── Reload ────────────────────────────────────────────────────────────────

    private void reloadNotas() {
        notasModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();

            String jpql = "SELECT n FROM NotaTransferencia n LEFT JOIN FETCH n.fornecedor ORDER BY n.dataNota DESC";
            List<NotaTransferencia> list = em.createQuery(jpql, NotaTransferencia.class).getResultList();

            // Aplica filtros
            String dataFiltro = tfFiltroData != null ? tfFiltroData.getText().trim() : "";
            Object tipoFiltro = cbFiltroTipo != null ? cbFiltroTipo.getSelectedItem() : "Todos";

            for (NotaTransferencia n : list) {
                // Filtro data
                if (!dataFiltro.isEmpty()) {
                    try {
                        String dataStr = n.getDataNota().format(FMT_DIA);
                        if (!dataStr.equals(dataFiltro)) continue;
                    } catch (Exception ignored) {}
                }
                // Filtro tipo
                if (tipoFiltro instanceof TipoNota && n.getTipo() != tipoFiltro) continue;

                String fornNome = n.getFornecedor() != null ? n.getFornecedor().getRazaoSocial() : "—";
                int qtdItens = n.getItens() != null ? n.getItens().size() : 0;

                notasModel.addRow(new Object[]{
                        n.getId(),
                        n.getDataNota().format(FMT),
                        n.getTipo().toString(),
                        fornNome,
                        n.getNumeroNota() != null && !n.getNumeroNota().isEmpty() ? n.getNumeroNota() : "—",
                        qtdItens,
                        String.format("R$ %.2f", n.getValorTotal() != null ? n.getValorTotal() : BigDecimal.ZERO)
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    public void carregarFornecedores() { carregarFornecedores_interno(); }

    private void carregarFornecedores_interno() {
        cbFornecedor.removeAllItems();
        cbFornecedor.addItem("— Nenhum —");
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Fornecedor> list = em.createQuery("SELECT f FROM Fornecedor f ORDER BY f.razaoSocial", Fornecedor.class).getResultList();
            for (Fornecedor f : list) cbFornecedor.addItem(f);
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    // ── Relatório / Impressão ─────────────────────────────────────────────────

    private void abrirRelatorioNota(Long notaId) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            NotaTransferencia nota = em.createQuery(
                            "SELECT n FROM NotaTransferencia n LEFT JOIN FETCH n.itens i LEFT JOIN FETCH i.produto " +
                                    "LEFT JOIN FETCH n.fornecedor WHERE n.id = :id", NotaTransferencia.class)
                    .setParameter("id", notaId).getSingleResult();

            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Nota #" + nota.getId() + " — " + nota.getTipo(),
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL);
            dlg.setSize(560, 600);
            dlg.setLocationRelativeTo(this);
            dlg.setLayout(new BorderLayout());

            JPanel content = buildConteudoRelatorio(nota);
            dlg.add(new JScrollPane(content), BorderLayout.CENTER);

            JPanel rod = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            rod.setBackground(UIFactory.XP_BG);
            rod.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
            JButton btnPrint = UIFactory.bigActionButton("🖨 Imprimir", null);
            btnPrint.setBackground(new Color(0, 84, 166)); btnPrint.setForeground(Color.WHITE);
            btnPrint.addActionListener(e -> imprimir(content, "Nota #" + nota.getId()));
            rod.add(btnPrint);
            rod.add(UIFactory.bigActionButton("Fechar", e -> dlg.dispose()));
            dlg.add(rod, BorderLayout.SOUTH);
            dlg.setVisible(true);

        } catch (Exception ex) { ex.printStackTrace(); showError("Erro ao abrir nota: " + ex.getMessage()); }
        finally { close(em); }
    }

    private JPanel buildConteudoRelatorio(NotaTransferencia nota) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        Font fTit  = new Font("Tahoma", Font.BOLD, 14);
        Font fSub  = new Font("Tahoma", Font.BOLD, 11);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 10);
        Font fBold = new Font("Tahoma", Font.BOLD, 10);

        boolean isEntrada = nota.getTipo() == TipoNota.ENTRADA;
        Color corTipo = isEntrada ? new Color(0, 110, 0) : new Color(180, 60, 0);

        addLinha(p, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0, 60, 140), SwingConstants.CENTER);
        addLinha(p, "NOTA DE TRANSFERÊNCIA DE ESTOQUE", fSub, Color.BLACK, SwingConstants.CENTER);
        addLinha(p, (isEntrada ? "▼ ENTRADA DE MERCADORIA" : "▲ SAÍDA DE MERCADORIA"), fSub, corTipo, SwingConstants.CENTER);
        p.add(Box.createRigidArea(new Dimension(0, 6)));
        p.add(sep());

        addLinha(p, "Nota Nº: " + nota.getId()
                + (nota.getNumeroNota() != null && !nota.getNumeroNota().isEmpty()
                ? "  |  Ref.: " + nota.getNumeroNota() : "")
                + "  |  Data: " + nota.getDataNota().format(FMT), fNorm, Color.BLACK, SwingConstants.LEFT);

        if (nota.getFornecedor() != null) {
            Fornecedor f = nota.getFornecedor();
            addLinha(p, "Fornecedor: " + f.getRazaoSocial()
                            + (f.getCnpj() != null && !f.getCnpj().isEmpty() ? "  CNPJ: " + f.getCnpj() : ""),
                    fNorm, Color.BLACK, SwingConstants.LEFT);
        }
        if (nota.getObservacoes() != null && !nota.getObservacoes().isEmpty())
            addLinha(p, "Obs.: " + nota.getObservacoes(), fNorm, Color.GRAY, SwingConstants.LEFT);

        p.add(Box.createRigidArea(new Dimension(0, 8)));

        // Cabeçalho da tabela de itens
        addLinha(p, "ITENS", fSub, new Color(0, 84, 166), SwingConstants.LEFT);
        p.add(sep());

        JPanel header = new JPanel(new GridLayout(1, 4));
        header.setBackground(new Color(10, 36, 106));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        for (String h : new String[]{"Produto", "Qtd", "Custo Unit.", "Subtotal"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);

        BigDecimal totalGeral = BigDecimal.ZERO;
        int idx = 0;
        for (ItemNota item : nota.getItens()) {
            JPanel row = new JPanel(new GridLayout(1, 4));
            row.setBackground(idx++ % 2 == 0 ? Color.WHITE : new Color(248, 248, 252));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

            BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
            BigDecimal cu  = item.getPrecoUnitario() != null ? item.getPrecoUnitario() : BigDecimal.ZERO;
            totalGeral = totalGeral.add(sub);

            addCell(row, item.getProduto().getNome(), fNorm);
            addCell(row, String.valueOf(item.getQuantidade()), fNorm);
            addCell(row, String.format("R$ %.2f", cu), fNorm);
            addCell(row, String.format("R$ %.2f", sub), fNorm);
            p.add(row);
        }

        p.add(sep());
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lTot = new JLabel("TOTAL GERAL:"); lTot.setFont(fBold);
        JLabel vTot = new JLabel(String.format("R$ %.2f", totalGeral));
        vTot.setFont(fBold); vTot.setForeground(corTipo); vTot.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(lTot, BorderLayout.WEST); totalRow.add(vTot, BorderLayout.EAST);
        p.add(totalRow);

        p.add(Box.createRigidArea(new Dimension(0, 20)));
        addLinha(p, "_____________________________          _____________________________", fNorm, Color.BLACK, SwingConstants.CENTER);
        addLinha(p, "       Responsável / Emissor                    Recebedor", fNorm, Color.GRAY, SwingConstants.CENTER);

        return p;
    }

    private void imprimir(JPanel panel, String titulo) {
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

    // ── Helpers visuais ───────────────────────────────────────────────────────

    private void addFormRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight(label), c);
        c.gridx = 1; c.weightx = 1;
        form.add(field, c);
    }

    private void addLinha(JPanel p, String text, Font font, Color color, int align) {
        JLabel l = new JLabel(text, align);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private void addCell(JPanel row, String text, Font font) {
        JLabel l = new JLabel("  " + text); l.setFont(font); row.add(l);
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