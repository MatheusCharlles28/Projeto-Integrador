package br.carmel.ui.panels;


import br.carmel.model.Produto;
import br.carmel.util.SenhaUtil;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Painel de Produtos com preço de custo, preço de venda e controle de estoque.
 * Entrada de estoque exige senha do administrador.
 */
public class ProdutoPanel extends JPanel {

    private final EntityManagerFactory emf;

    // Formulário
    private final JTextField tfNome   = UIFactory.styledField("");
    private final JTextField tfVenda  = UIFactory.styledField("");
    private final JTextField tfCusto  = UIFactory.styledField("");
    private final JTextField tfCod    = UIFactory.styledField("");
    private final JTextField tfSerie  = UIFactory.styledField("");
    private final JTextArea  taDesc   = UIFactory.styledTextArea(3, 18);

    // Estoque (somente leitura no form — alterado via botão admin)
    private final JLabel lblEstoque   = new JLabel("0");
    // Preço médio (somente leitura — calculado automaticamente)
    private final JLabel lblPrecoMedio = new JLabel("—");

    private DefaultTableModel tableModel;
    private JTable table;
    private Long idSelecionado = null;

    public ProdutoPanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);


        build();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Cadastro de Produtos"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildFormPanel(), buildTablePanel());
        split.setDividerLocation(340);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);
        add(split, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
    }

    // ── Formulário ────────────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);
        outer.setBorder(new EmptyBorder(8, 8, 8, 4));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(UIFactory.groupBorder("Dados do Produto"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(4, 6, 4, 6);
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.anchor  = GridBagConstraints.NORTHWEST;

        int row = 0;

        // Nome
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Nome:"), c);
        c.gridx = 1; c.weightx = 1; form.add(tfNome, c); row++;

        // Preço de venda
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Preço de Venda (R$):"), c);
        c.gridx = 1; c.weightx = 1; form.add(tfVenda, c); row++;

        // Preço de custo
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Preço de Custo (R$):"), c);
        c.gridx = 1; c.weightx = 1; form.add(tfCusto, c); row++;

        // Preço médio ponderado (somente leitura)
        lblPrecoMedio.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblPrecoMedio.setForeground(new Color(0, 84, 166));
        JLabel lblPrecoMedioTit = UIFactory.labelLight("Preço Médio (R$):");
        lblPrecoMedioTit.setFont(new Font("Tahoma", Font.ITALIC, 10));
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(lblPrecoMedioTit, c);
        c.gridx = 1; c.weightx = 1; form.add(lblPrecoMedio, c); row++;

        // Margem calculada (label dinâmico)
        JLabel lblMargemTit = UIFactory.labelLight("Margem:");
        lblMargemTit.setFont(new Font("Tahoma", Font.ITALIC, 10));
        JLabel lblMargem = new JLabel("—");
        lblMargem.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblMargem.setForeground(new Color(0, 110, 0));

        // Atualiza margem ao sair dos campos de preço
        Runnable calcMargem = () -> {
            try {
                BigDecimal v = Validator.parseBigDecimal(tfVenda.getText());
                BigDecimal cu = Validator.parseBigDecimal(tfCusto.getText());
                if (cu.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal margem = v.subtract(cu)
                            .divide(cu, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    lblMargem.setText(String.format("%.1f%%", margem));
                    lblMargem.setForeground(margem.compareTo(BigDecimal.ZERO) >= 0
                            ? new Color(0, 110, 0) : new Color(180, 0, 0));
                } else { lblMargem.setText("—"); }
            } catch (Exception e) { lblMargem.setText("—"); }
        };
        tfVenda.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { calcMargem.run(); }});
        tfCusto.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { calcMargem.run(); }});

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(lblMargemTit, c);
        c.gridx = 1; c.weightx = 1; form.add(lblMargem, c); row++;

        // Estoque (somente leitura)
        JPanel estoquePanel = new JPanel(new BorderLayout(6, 0));
        estoquePanel.setOpaque(false);
        lblEstoque.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblEstoque.setForeground(new Color(0, 84, 166));
        JButton btnAddEstoque = UIFactory.bigActionButton("+ Adicionar Estoque", e -> adicionarEstoque());
        btnAddEstoque.setPreferredSize(new Dimension(150, 24));
        estoquePanel.add(lblEstoque, BorderLayout.WEST);
        estoquePanel.add(btnAddEstoque, BorderLayout.EAST);

        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Estoque atual:"), c);
        c.gridx = 1; c.weightx = 1; form.add(estoquePanel, c); row++;

        // Descrição
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Descrição:"), c);
        c.gridx = 1; c.weightx = 1; form.add(new JScrollPane(taDesc), c); row++;

        // Cód de barras
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Cód. de Barras:"), c);
        c.gridx = 1; c.weightx = 1; form.add(tfCod, c); row++;

        // Nº de série
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(UIFactory.labelLight("Nº de Série:"), c);
        c.gridx = 1; c.weightx = 1; form.add(tfSerie, c); row++;

        // Glue
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    // ── Tabela ────────────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        JLabel lbl = new JLabel("Produtos Cadastrados");
        lbl.setFont(UIFactory.FONT_BOLD);
        lbl.setForeground(new Color(0, 84, 166));
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        p.add(lbl, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Nome", "Venda (R$)", "Custo (R$)", "Médio (R$)", "Estoque", "Cód."}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIFactory.styledTable();
        table.setModel(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);

        // Colorir estoque baixo em vermelho
        table.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 4, 1, 4));
            if (isSelected) {
                cell.setBackground(UIFactory.XP_TABLE_SEL);
                cell.setForeground(Color.WHITE);
            } else {
                cell.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 252));
                // Coluna estoque: vermelho se <= 0
                if (col == 5) {
                    try {
                        int est = Integer.parseInt(value.toString());
                        cell.setForeground(est <= 0 ? new Color(180, 0, 0) : new Color(0, 110, 0));
                        cell.setFont(UIFactory.FONT_BOLD);
                    } catch (Exception e) { cell.setForeground(Color.BLACK); }
                } else {
                    cell.setForeground(Color.BLACK);
                }
            }
            return cell;
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                Long id = (Long) tableModel.getValueAt(table.getSelectedRow(), 0);
                preencherFormularioPorId(id);
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── Botões ────────────────────────────────────────────────────────────────

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        bar.add(UIFactory.bigActionButton("Novo",            e -> limpar()));
        bar.add(UIFactory.bigActionButton("Salvar",          e -> salvar()));
        bar.add(UIFactory.bigActionButton("Atualizar",       e -> atualizar()));
        bar.add(UIFactory.bigActionButton("Excluir",         e -> excluir()));
        bar.add(UIFactory.bigActionButton("Atualizar Lista", e -> reloadTable()));
        return bar;
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    private void limpar() {
        idSelecionado = null;
        tfNome.setText(""); tfVenda.setText(""); tfCusto.setText("");
        tfCod.setText(""); tfSerie.setText(""); taDesc.setText("");
        lblEstoque.setText("0");
        lblPrecoMedio.setText("—");
        lblPrecoMedio.setForeground(Color.GRAY);
        table.clearSelection();
    }

    private void salvar() {
        Validator.Result v = Validator.validarProduto(
                tfNome.getText(), tfVenda.getText(), tfCusto.getText(),
                tfCod.getText(), tfSerie.getText(), taDesc.getText());
        if (!v.isValido()) { showError("Corrija os campos:\n\n" + v.getMensagem()); return; }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Produto pr = new Produto();
            applyForm(pr);
            em.persist(pr);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Produto salvo!");
            limpar(); reloadTable();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe um produto com esse Nome, Código de Barras ou Número de Série.");
            else showError("Erro ao salvar: " + msg);
            ex.printStackTrace();
        } finally { close(em); }
    }

    private void atualizar() {
        if (idSelecionado == null) { JOptionPane.showMessageDialog(this, "Selecione um produto na lista."); return; }
        Validator.Result v = Validator.validarProduto(
                tfNome.getText(), tfVenda.getText(), tfCusto.getText(),
                tfCod.getText(), tfSerie.getText(), taDesc.getText());
        if (!v.isValido()) { showError("Corrija os campos:\n\n" + v.getMensagem()); return; }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Produto managed = em.find(Produto.class, idSelecionado);
            if (managed == null) { em.getTransaction().rollback(); return; }

            // ── Calcula preço médio ponderado se o custo mudou ────────────────
            BigDecimal novoCusto = Validator.isBlank(tfCusto.getText())
                    ? null : Validator.parseBigDecimal(tfCusto.getText());

            if (novoCusto != null && novoCusto.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal custoAnterior = managed.getPrecoCusto();
                BigDecimal medioAnterior = managed.getPrecoMedio();
                BigDecimal qtdHist       = managed.getQtdHistoricoCusto();
                int estoqueAtual         = managed.getEstoque() != null ? managed.getEstoque() : 0;

                boolean custoMudou = custoAnterior == null
                        || novoCusto.compareTo(custoAnterior) != 0;

                if (custoMudou) {
                    BigDecimal pesoEntrada = BigDecimal.valueOf(Math.max(estoqueAtual, 1));
                    if (medioAnterior == null || qtdHist == null
                            || qtdHist.compareTo(BigDecimal.ZERO) == 0) {
                        managed.setPrecoMedio(novoCusto);
                        managed.setQtdHistoricoCusto(pesoEntrada);
                    } else {
                        BigDecimal novaQtd   = qtdHist.add(pesoEntrada);
                        BigDecimal novoMedio = qtdHist.multiply(medioAnterior)
                                .add(pesoEntrada.multiply(novoCusto))
                                .divide(novaQtd, 2, java.math.RoundingMode.HALF_UP);
                        managed.setPrecoMedio(novoMedio);
                        managed.setQtdHistoricoCusto(novaQtd);
                    }
                }
            }

            applyForm(managed);
            em.merge(managed);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Produto atualizado.");
            reloadTable();
            preencherFormularioPorId(idSelecionado);
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe outro produto com esse Nome, Código de Barras ou Número de Série.");
            else showError("Erro ao atualizar: " + msg);
            ex.printStackTrace();
        } finally { close(em); }
    }

    private void excluir() {
        if (idSelecionado == null) { JOptionPane.showMessageDialog(this, "Selecione um produto na lista."); return; }
        int conf = JOptionPane.showConfirmDialog(this,
                "Confirma exclusão do produto ID " + idSelecionado + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Produto pr = em.find(Produto.class, idSelecionado);
            if (pr != null) { em.remove(pr); em.getTransaction().commit(); JOptionPane.showMessageDialog(this, "Produto removido."); }
            else { em.getTransaction().rollback(); JOptionPane.showMessageDialog(this, "Produto não encontrado."); }
            limpar(); reloadTable();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); ex.printStackTrace(); }
        finally { close(em); }
    }

    // ── Adicionar estoque (requer senha admin) ────────────────────────────────

    private void adicionarEstoque() {
        if (idSelecionado == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um produto na lista primeiro.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Diálogo próprio com JDialog para JPasswordField funcionar corretamente
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Adicionar Estoque", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(340, 210);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblProduto = UIFactory.labelLight("Produto: " + tfNome.getText().trim());
        lblProduto.setFont(UIFactory.FONT_BOLD);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2;
        form.add(lblProduto, gc);

        JTextField tfQtd = UIFactory.styledField("");
        tfQtd.setToolTipText("Quantidade a adicionar");
        gc.gridwidth = 1;
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; form.add(UIFactory.labelLight("Quantidade a adicionar:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(tfQtd, gc);

        JPasswordField tfSenha = UIFactory.styledPasswordField("");
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0; form.add(UIFactory.labelLight("Senha do administrador:"), gc);
        gc.gridx = 1; gc.weightx = 1; form.add(tfSenha, gc);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] ok = {false};
        JButton btnOk = UIFactory.bigActionButton("Confirmar", e -> { ok[0] = true; dlg.dispose(); });
        btnOk.setBackground(new Color(0, 100, 0));
        btnOk.setForeground(Color.WHITE);
        JButton btnCancelar = UIFactory.bigActionButton("Cancelar", e -> dlg.dispose());
        btns.add(btnOk);
        btns.add(btnCancelar);
        dlg.add(btns, BorderLayout.SOUTH);

        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfQtd::requestFocusInWindow);
        dlg.setVisible(true);

        if (!ok[0]) return;

        // Valida senha
        String senhaDigitada = new String(tfSenha.getPassword());
        if (!SenhaUtil.isAdmin(senhaDigitada)) {
            showError("Senha de administrador incorreta.");
            return;
        }

        // Valida quantidade
        String qtdStr = tfQtd.getText().trim();
        if (Validator.isBlank(qtdStr)) { showError("Informe a quantidade."); return; }
        int qtd;
        try {
            qtd = Integer.parseInt(qtdStr);
            if (qtd <= 0) { showError("A quantidade deve ser maior que zero."); return; }
        } catch (NumberFormatException e) { showError("Quantidade inválida. Digite um número inteiro."); return; }

        // Atualiza estoque no banco
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Produto managed = em.find(Produto.class, idSelecionado);
            if (managed == null) { em.getTransaction().rollback(); return; }
            int estoqueAntes = managed.getEstoque() != null ? managed.getEstoque() : 0;
            managed.setEstoque(estoqueAntes + qtd);
            em.merge(managed);
            em.getTransaction().commit();
            lblEstoque.setText(String.valueOf(estoqueAntes + qtd));
            JOptionPane.showMessageDialog(this,
                    "Estoque atualizado!\n" +
                            "Antes: " + estoqueAntes + " un.\n" +
                            "Adicionado: " + qtd + " un.\n" +
                            "Novo total: " + (estoqueAntes + qtd) + " un.",
                    "Estoque Atualizado", JOptionPane.INFORMATION_MESSAGE);
            reloadTable();
        } catch (Exception ex) {
            rollback(em); showError("Erro ao atualizar estoque: " + ex.getMessage()); ex.printStackTrace();
        } finally { close(em); }
    }

    // ── Reload e preenchimento ────────────────────────────────────────────────

    public void reloadTable() {
        tableModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Produto> list = em.createQuery("SELECT p FROM Produto p ORDER BY p.nome", Produto.class).getResultList();
            for (Produto p : list) {
                String custo  = p.getPrecoCusto()  != null ? String.format("R$ %.2f", p.getPrecoCusto())  : "—";
                String medio  = p.getPrecoMedio()  != null ? String.format("R$ %.2f", p.getPrecoMedio())  : "—";
                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getNome(),
                        String.format("R$ %.2f", p.getValor()),
                        custo,
                        medio,
                        p.getEstoque() != null ? p.getEstoque() : 0,
                        p.getCodBarras()
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    private void preencherFormularioPorId(Long id) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Produto p = em.find(Produto.class, id);
            if (p == null) return;
            idSelecionado = id;
            tfNome.setText(p.getNome());
            tfVenda.setText(p.getValor().toString());
            tfCusto.setText(p.getPrecoCusto() != null ? p.getPrecoCusto().toString() : "");
            taDesc.setText(p.getDescricao() != null ? p.getDescricao() : "");
            tfCod.setText(p.getCodBarras());
            tfSerie.setText(p.getNumeroSerie());
            lblEstoque.setText(String.valueOf(p.getEstoque() != null ? p.getEstoque() : 0));
            if (p.getPrecoMedio() != null) {
                lblPrecoMedio.setText(String.format("R$ %.2f", p.getPrecoMedio()));
                lblPrecoMedio.setForeground(new Color(0, 84, 166));
            } else {
                lblPrecoMedio.setText("— (sem histórico)");
                lblPrecoMedio.setForeground(Color.GRAY);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { close(em); }
    }

    private void applyForm(Produto p) {
        p.setNome(tfNome.getText().trim());
        p.setDescricao(taDesc.getText().trim());
        p.setValor(Validator.parseBigDecimal(tfVenda.getText()));
        if (!Validator.isBlank(tfCusto.getText()))
            p.setPrecoCusto(Validator.parseBigDecimal(tfCusto.getText()));
        else
            p.setPrecoCusto(null);
        p.setCodBarras(Validator.isBlank(tfCod.getText()) ? null : tfCod.getText().trim());
        p.setNumeroSerie(Validator.isBlank(tfSerie.getText()) ? null : tfSerie.getText().trim());
        if (p.getEstoque() == null) p.setEstoque(0);
    }

    private void rollback(EntityManager em) { if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback(); }
    private void close(EntityManager em)    { if (em != null) em.close(); }
    private void showError(String msg)      { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}