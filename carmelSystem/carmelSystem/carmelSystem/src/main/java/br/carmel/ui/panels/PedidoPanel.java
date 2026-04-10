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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Painel de Pedidos - visual Windows XP.
 * Parte superior: criar pedido. Parte inferior: lista de todos os pedidos + itens.
 */
public class PedidoPanel extends JPanel {

    private final EntityManagerFactory emf;
    private CaixaPanel caixaPanel; // referência para registrar venda no caixa

    private final JComboBox<Cliente> cbClientes = new JComboBox<>();
    private final JComboBox<Produto> cbProdutos = new JComboBox<>();
    private final DefaultListModel<ItensPedido> orderItemsModel = new DefaultListModel<>();

    // Tabela de pedidos salvos
    private DefaultTableModel pedidosModel;
    private JTable pedidosTable;

    // Tabela de itens do pedido selecionado
    private DefaultTableModel itensModel;

    // Campos de busca
    private JTextField tfBuscaCliente;
    private JTextField tfBuscaDataDe;
    private JTextField tfBuscaDataAte;
    private JTextField tfBuscaValorMin;
    private JTextField tfBuscaValorMax;

    public void setCaixaPanel(CaixaPanel caixaPanel) { this.caixaPanel = caixaPanel; }

    public PedidoPanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);


        build();
        // dados carregados sob demanda via reloadCombos() e reloadPedidos()
    }

    public void reloadCombos() {
        cbClientes.removeAllItems();
        cbProdutos.removeAllItems();
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList().forEach(cbClientes::addItem);
            em.createQuery("SELECT p FROM Produto p", Produto.class).getResultList().forEach(cbProdutos::addItem);
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (em != null) em.close(); }
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Pedidos"), BorderLayout.NORTH);

        // Dividir: parte superior (criar pedido) + parte inferior (lista de pedidos)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildCriarPedidoPanel(), buildPedidosPanel());
        mainSplit.setDividerLocation(220);
        mainSplit.setDividerSize(5);
        mainSplit.setBackground(UIFactory.XP_BG);
        add(mainSplit, BorderLayout.CENTER);
    }

    // ── Painel superior: criar pedido ─────────────────────────────────────────

    private JPanel buildCriarPedidoPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(6, 8, 4, 8));

        JPanel selecao = new JPanel(new GridBagLayout());
        selecao.setBackground(UIFactory.XP_BG);
        selecao.setBorder(UIFactory.groupBorder("Novo Pedido"));

        styleCombo(cbClientes, Cliente.class);
        styleCombo(cbProdutos, Produto.class);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Cliente
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        selecao.add(UIFactory.labelLight("Cliente:"), g);
        g.gridx = 1; g.weightx = 1.0; g.gridwidth = 3;
        selecao.add(cbClientes, g);

        // Produto (combobox)
        g.gridx = 0; g.gridy = 1; g.weightx = 0; g.gridwidth = 1;
        selecao.add(UIFactory.labelLight("Produto:"), g);
        g.gridx = 1; g.weightx = 1.0;
        selecao.add(cbProdutos, g);
        JTextField tfQtd = new JTextField("1", 4);
        UIFactory.styleSmallField(tfQtd);
        g.gridx = 2; g.weightx = 0;
        selecao.add(UIFactory.labelLight("Qtd:"), g);
        g.gridx = 3;
        selecao.add(tfQtd, g);
        JButton btnAdd = UIFactory.bigSmallButton("+ Adicionar");
        g.gridx = 4; selecao.add(btnAdd, g);

        // Código de barras
        g.gridx = 0; g.gridy = 2; g.weightx = 0; g.gridwidth = 1;
        selecao.add(UIFactory.labelLight("Cód. Barras:"), g);
        JTextField tfCodBarras = UIFactory.styledField("");
        tfCodBarras.setToolTipText("Digite ou escaneie o código de barras e pressione Enter");
        g.gridx = 1; g.weightx = 1.0;
        selecao.add(tfCodBarras, g);
        JTextField tfQtdBarras = new JTextField("1", 4);
        UIFactory.styleSmallField(tfQtdBarras);
        g.gridx = 2; g.weightx = 0;
        selecao.add(UIFactory.labelLight("Qtd:"), g);
        g.gridx = 3;
        selecao.add(tfQtdBarras, g);
        JButton btnAddBarras = UIFactory.bigSmallButton("+ Add");
        g.gridx = 4; selecao.add(btnAddBarras, g);

        // Observações
        g.gridx = 0; g.gridy = 3; g.weightx = 0; g.gridwidth = 1;
        selecao.add(UIFactory.labelLight("Obs:"), g);
        JTextArea taObs = UIFactory.styledTextArea(2, 30);
        g.gridx = 1; g.weightx = 1.0; g.gridwidth = 4;
        selecao.add(new JScrollPane(taObs), g);

        p.add(selecao, BorderLayout.CENTER);

        // Lista de itens + total
        JPanel direita = new JPanel(new BorderLayout(0, 4));
        direita.setBackground(UIFactory.XP_BG);
        direita.setBorder(UIFactory.groupBorder("Itens do Pedido Atual"));
        direita.setPreferredSize(new Dimension(300, 0));

        JList<ItensPedido> lstItens = new JList<>(orderItemsModel);
        lstItens.setFont(UIFactory.FONT_NORMAL);
        lstItens.setBackground(Color.WHITE);
        lstItens.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String s = value.getProduto().getNome() + "  x" + value.getQuantidade()
                    + "  = R$ " + String.format("%.2f", value.getSubtotal());
            JLabel lbl = new JLabel("  " + s);
            lbl.setFont(UIFactory.FONT_NORMAL);
            lbl.setOpaque(true);
            lbl.setBackground(isSelected ? UIFactory.XP_TABLE_SEL : Color.WHITE);
            lbl.setForeground(isSelected ? Color.WHITE : Color.BLACK);
            lbl.setBorder(new EmptyBorder(2, 4, 2, 4));
            return lbl;
        });

        JLabel lblTotal = new JLabel("Total: R$ 0,00");
        lblTotal.setFont(UIFactory.FONT_BOLD);
        lblTotal.setForeground(new Color(0, 84, 166));
        lblTotal.setBorder(new EmptyBorder(2, 6, 2, 6));

        JPanel botoesDir = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        botoesDir.setOpaque(false);
        botoesDir.add(UIFactory.bigActionButton("Remover Item",  ev -> removerItem(lstItens, lblTotal)));
        botoesDir.add(UIFactory.bigActionButton("Salvar Pedido", ev -> salvarPedido(taObs, lblTotal)));
        botoesDir.add(UIFactory.bigActionButton("Limpar",        ev -> { orderItemsModel.clear(); updateTotal(lblTotal); }));

        direita.add(new JScrollPane(lstItens), BorderLayout.CENTER);
        direita.add(lblTotal, BorderLayout.NORTH);
        direita.add(botoesDir, BorderLayout.SOUTH);

        p.add(direita, BorderLayout.EAST);

        // ── Listener: adicionar por combobox ──────────────────────────────────
        btnAdd.addActionListener(ev -> {
            Produto prod = (Produto) cbProdutos.getSelectedItem();
            if (prod == null) { JOptionPane.showMessageDialog(this, "Selecione um produto."); return; }
            if (cbClientes.getSelectedItem() == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente."); return; }
            Validator.Result v = Validator.validarItem(tfQtd.getText());
            if (!v.isValido()) { JOptionPane.showMessageDialog(this, v.getMensagem(), "Atenção", JOptionPane.WARNING_MESSAGE); return; }
            int qtd = Integer.parseInt(tfQtd.getText().trim());
            adicionarItemPorProduto(prod, qtd, lblTotal);
            tfQtd.setText("1");
        });

        // ── Listener: adicionar por código de barras ──────────────────────────
        Runnable addPorBarras = () -> {
            String cod = tfCodBarras.getText().trim();
            if (Validator.isBlank(cod)) { tfCodBarras.requestFocus(); return; }
            int qtd;
            try {
                qtd = Integer.parseInt(tfQtdBarras.getText().trim());
                if (qtd <= 0) { JOptionPane.showMessageDialog(this, "Quantidade inválida."); return; }
            } catch (Exception e) { JOptionPane.showMessageDialog(this, "Quantidade inválida."); return; }

            EntityManager em = null;
            try {
                em = emf.createEntityManager();
                List<Produto> prods = em.createQuery(
                                "SELECT p FROM Produto p WHERE p.codBarras = :c", Produto.class)
                        .setParameter("c", cod).getResultList();
                if (prods.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Produto não encontrado: " + cod, "Atenção", JOptionPane.WARNING_MESSAGE);
                    tfCodBarras.selectAll(); tfCodBarras.requestFocus(); return;
                }
                adicionarItemPorProduto(prods.get(0), qtd, lblTotal);
                tfCodBarras.setText(""); tfQtdBarras.setText("1");
                tfCodBarras.requestFocus();
            } catch (Exception ex) { showError("Erro: " + ex.getMessage()); }
            finally { if (em != null) em.close(); }
        };

        btnAddBarras.addActionListener(ev -> addPorBarras.run());
        tfCodBarras.addActionListener(ev -> addPorBarras.run()); // Enter dispara

        return p;
    }

    private void adicionarItemPorProduto(Produto prod, int qtd, JLabel lblTotal) {
        // Se já existe na lista, soma quantidade
        for (int i = 0; i < orderItemsModel.size(); i++) {
            if (orderItemsModel.get(i).getProduto().getId().equals(prod.getId())) {
                ItensPedido exist = orderItemsModel.get(i);
                exist.setQuantidade(exist.getQuantidade() + qtd);
                exist.calcularSubtotal();
                orderItemsModel.set(i, exist);
                updateTotal(lblTotal);
                return;
            }
        }
        // Novo item
        ItensPedido it = new ItensPedido();
        it.setProduto(prod); it.setQuantidade(qtd);
        it.setPrecoUnitario(prod.getValor()); it.calcularSubtotal();
        orderItemsModel.addElement(it);
        updateTotal(lblTotal);
    }

    // ── Painel inferior: busca + lista de pedidos + detalhes ─────────────────

    private JPanel buildPedidosPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(4, 8, 8, 8));

        // ── Painel de busca ───────────────────────────────────────────────────
        JPanel buscaPanel = new JPanel(new GridBagLayout());
        buscaPanel.setBackground(UIFactory.XP_PANEL_BG);
        buscaPanel.setBorder(UIFactory.groupBorder("Consultar Pedidos"));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets  = new Insets(4, 6, 4, 6);
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.anchor  = GridBagConstraints.WEST;

        // Linha 1 — Cliente
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        buscaPanel.add(UIFactory.labelLight("Cliente:"), gc);
        tfBuscaCliente = UIFactory.styledField("");
        tfBuscaCliente.setToolTipText("Digite parte do nome do cliente");
        tfBuscaCliente.setPreferredSize(new Dimension(200, 22));
        gc.gridx = 1; gc.weightx = 1.0;
        buscaPanel.add(tfBuscaCliente, gc);

        // Linha 1 — Período (na mesma linha)
        gc.gridx = 2; gc.weightx = 0;
        buscaPanel.add(UIFactory.labelLight("Data de:"), gc);
        tfBuscaDataDe = UIFactory.styledField("");
        tfBuscaDataDe.setToolTipText("dd/MM/yyyy");
        tfBuscaDataDe.setPreferredSize(new Dimension(100, 22));
        gc.gridx = 3; gc.weightx = 0.5;
        buscaPanel.add(tfBuscaDataDe, gc);

        gc.gridx = 4; gc.weightx = 0;
        buscaPanel.add(UIFactory.labelLight("até:"), gc);
        tfBuscaDataAte = UIFactory.styledField("");
        tfBuscaDataAte.setToolTipText("dd/MM/yyyy");
        tfBuscaDataAte.setPreferredSize(new Dimension(100, 22));
        gc.gridx = 5; gc.weightx = 0.5;
        buscaPanel.add(tfBuscaDataAte, gc);

        // Linha 2 — Valor
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        buscaPanel.add(UIFactory.labelLight("Valor mín (R$):"), gc);
        tfBuscaValorMin = UIFactory.styledField("");
        tfBuscaValorMin.setToolTipText("Ex: 50.00");
        tfBuscaValorMin.setPreferredSize(new Dimension(100, 22));
        gc.gridx = 1; gc.weightx = 0.5;
        buscaPanel.add(tfBuscaValorMin, gc);

        gc.gridx = 2; gc.weightx = 0;
        buscaPanel.add(UIFactory.labelLight("Valor máx (R$):"), gc);
        tfBuscaValorMax = UIFactory.styledField("");
        tfBuscaValorMax.setToolTipText("Ex: 500.00");
        tfBuscaValorMax.setPreferredSize(new Dimension(100, 22));
        gc.gridx = 3; gc.weightx = 0.5;
        buscaPanel.add(tfBuscaValorMax, gc);

        // Botões de busca
        JPanel btnBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnBusca.setOpaque(false);
        JButton btnPesquisar = UIFactory.bigActionButton("🔍 Buscar", e -> buscarPedidos());
        btnPesquisar.setBackground(new Color(0, 84, 166));
        btnPesquisar.setForeground(Color.WHITE);
        JButton btnLimpar = UIFactory.bigActionButton("Limpar", e -> limparBusca());
        btnBusca.add(btnPesquisar);
        btnBusca.add(btnLimpar);
        gc.gridx = 4; gc.gridy = 1; gc.gridwidth = 2; gc.weightx = 0;
        buscaPanel.add(btnBusca, gc);

        // Enter nos campos dispara busca
        java.awt.event.KeyAdapter enterBusca = new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) buscarPedidos();
            }
        };
        tfBuscaCliente.addKeyListener(enterBusca);
        tfBuscaDataDe.addKeyListener(enterBusca);
        tfBuscaDataAte.addKeyListener(enterBusca);
        tfBuscaValorMin.addKeyListener(enterBusca);
        tfBuscaValorMax.addKeyListener(enterBusca);

        p.add(buscaPanel, BorderLayout.NORTH);

        // ── Tabela de pedidos ─────────────────────────────────────────────────
        pedidosModel = new DefaultTableModel(
                new String[]{"ID", "Cliente", "Data/Hora", "Status", "Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pedidosTable = UIFactory.styledTable();
        pedidosTable.setModel(pedidosModel);
        pedidosTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        pedidosTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        pedidosTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        pedidosTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        pedidosTable.getColumnModel().getColumn(4).setPreferredWidth(90);

        // ── Tabela de itens ───────────────────────────────────────────────────
        itensModel = new DefaultTableModel(
                new String[]{"Produto", "Cód. Barras", "Preço Unit.", "Qtd", "Subtotal"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable itensTable = UIFactory.styledTable();
        itensTable.setModel(itensModel);
        itensTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        itensTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        itensTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        itensTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        itensTable.getColumnModel().getColumn(4).setPreferredWidth(90);

        pedidosTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pedidosTable.getSelectedRow() >= 0) {
                Long id = (Long) pedidosModel.getValueAt(pedidosTable.getSelectedRow(), 0);
                loadItensDoPedido(id);
            }
        });

        // Duplo clique abre o relatório
        pedidosTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && pedidosTable.getSelectedRow() >= 0) {
                    abrirRelatorioPedidoSelecionado();
                }
            }
        });

        // Label resultado
        JLabel lblResultado = new JLabel("Todos os pedidos");
        lblResultado.setFont(UIFactory.FONT_BOLD);
        lblResultado.setForeground(new Color(0, 84, 166));
        lblResultado.setName("lblResultado");

        JPanel topoTabela = new JPanel(new BorderLayout());
        topoTabela.setBackground(UIFactory.XP_BG);
        topoTabela.setBorder(new EmptyBorder(4, 0, 2, 0));
        topoTabela.add(lblResultado, BorderLayout.WEST);

        JPanel lblItens = new JPanel(new BorderLayout());
        lblItens.setBackground(UIFactory.XP_BG);
        JLabel lblI = new JLabel("Itens do Pedido Selecionado");
        lblI.setFont(UIFactory.FONT_BOLD);
        lblI.setForeground(new Color(0, 84, 166));
        lblI.setBorder(new EmptyBorder(4, 0, 2, 0));
        lblItens.add(lblI, BorderLayout.NORTH);
        lblItens.add(new JScrollPane(itensTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(pedidosTable), lblItens);
        split.setDividerLocation(130);
        split.setDividerSize(4);
        split.setBackground(UIFactory.XP_BG);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBackground(UIFactory.XP_BG);
        centro.add(topoTabela, BorderLayout.NORTH);
        centro.add(split, BorderLayout.CENTER);
        p.add(centro, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        rodape.setBackground(new Color(212, 208, 200));
        rodape.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));
        rodape.add(UIFactory.bigActionButton("Atualizar Lista", e -> { limparBusca(); reloadPedidos(); }));

        JButton btnVerPedido = UIFactory.bigActionButton("🖨 Ver / Imprimir Pedido", e -> abrirRelatorioPedidoSelecionado());
        btnVerPedido.setBackground(new Color(0, 84, 166));
        btnVerPedido.setForeground(Color.WHITE);
        btnVerPedido.setPreferredSize(new Dimension(180, 26));
        rodape.add(btnVerPedido);

        JLabel dica = new JLabel("  Dica: clique duplo na linha para abrir o relatório");
        dica.setFont(new Font("Tahoma", Font.ITALIC, 10));
        dica.setForeground(new Color(100, 100, 100));
        rodape.add(dica);

        p.add(rodape, BorderLayout.SOUTH);

        return p;
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    private void removerItem(JList<ItensPedido> lst, JLabel lblTotal) {
        int sel = lst.getSelectedIndex();
        if (sel >= 0) { orderItemsModel.remove(sel); updateTotal(lblTotal); }
        else JOptionPane.showMessageDialog(this, "Selecione um item.");
    }

    private void salvarPedido(JTextArea taObs, JLabel lblTotal) {
        Cliente cli = (Cliente) cbClientes.getSelectedItem();
        if (cli == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }
        if (orderItemsModel.isEmpty()) { JOptionPane.showMessageDialog(this, "Adicione pelo menos um item ao pedido.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        String obs = taObs.getText().trim();
        if (obs.length() > 500) { JOptionPane.showMessageDialog(this, "Observações devem ter no máximo 500 caracteres.", "Atenção", JOptionPane.WARNING_MESSAGE); return; }

        // ── Persiste pedido como PENDENTE (sem pagamento, sem débito de estoque) ──
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            Cliente managed = em.find(Cliente.class, cli.getId());
            if (managed == null) { em.getTransaction().rollback(); JOptionPane.showMessageDialog(this, "Cliente não encontrado no BD."); return; }

            Pedido ped = new Pedido();
            ped.setCliente(managed);
            ped.setObservacoes(obs);
            ped.setDataPedido(LocalDateTime.now());
            ped.setStatus(StatusPedido.PENDENTE);
            em.persist(ped);

            for (int i = 0; i < orderItemsModel.size(); i++) {
                ItensPedido orig = orderItemsModel.get(i);
                Produto pm = em.find(Produto.class, orig.getProduto().getId());
                if (pm == null) { em.getTransaction().rollback(); JOptionPane.showMessageDialog(this, "Produto não encontrado."); return; }
                ItensPedido item = new ItensPedido();
                item.setPedido(ped); item.setProduto(pm);
                item.setQuantidade(orig.getQuantidade());
                item.setPrecoUnitario(orig.getPrecoUnitario());
                item.calcularSubtotal();
                ped.adicionarItem(item);
                em.persist(item);
            }
            ped.calcularValorTotal();
            em.merge(ped);
            em.getTransaction().commit();

            orderItemsModel.clear(); updateTotal(lblTotal);
            taObs.setText("");
            reloadPedidos();

            // Imprime folha A4 do pedido pendente
            EntityManager em2 = emf.createEntityManager();
            try {
                Pedido pedidoCompleto = em2.createQuery(
                        "SELECT p FROM Pedido p LEFT JOIN FETCH p.itensPedidos i LEFT JOIN FETCH i.produto LEFT JOIN FETCH p.cliente WHERE p.id = :id",
                        Pedido.class).setParameter("id", ped.getId()).getSingleResult();
                imprimirPedidoPendente(pedidoCompleto);
            } finally { em2.close(); }

        } catch (Exception ex) {
            rollback(em); showError("Erro ao salvar pedido: " + ex.getMessage()); ex.printStackTrace();
        } finally { close(em); }
    }

    private void imprimirPedidoPendente(Pedido ped) {
        JPanel content = buildFolhaPedido(ped);
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setJobName("Pedido #" + ped.getId());
        // Configura para A4
        java.awt.print.PageFormat pf = job.defaultPage();
        java.awt.print.Paper paper = new java.awt.print.Paper();
        double w = 595, h = 842; // A4 em pontos (72dpi)
        paper.setSize(w, h);
        paper.setImageableArea(36, 36, w - 72, h - 72);
        pf.setPaper(paper);
        pf.setOrientation(java.awt.print.PageFormat.PORTRAIT);

        job.setPrintable((g, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return java.awt.print.Printable.NO_SUCH_PAGE;
            content.setSize((int)(pageFormat.getImageableWidth()), (int)(pageFormat.getImageableHeight()));
            content.doLayout();
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            content.printAll(g2);
            return java.awt.print.Printable.PAGE_EXISTS;
        }, pf);

        // Mostra preview antes de imprimir
        JDialog preview = new JDialog(SwingUtilities.getWindowAncestor(this), "Pedido #" + ped.getId() + " — PENDENTE", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        preview.setSize(620, 880);
        preview.setLocationRelativeTo(this);
        preview.setLayout(new BorderLayout());
        preview.add(new JScrollPane(content), BorderLayout.CENTER);

        JPanel rod = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rod.setBackground(UIFactory.XP_BG);
        JButton btnImpr = UIFactory.bigActionButton("🖨 Imprimir", null);
        btnImpr.setBackground(new Color(0, 84, 166)); btnImpr.setForeground(Color.WHITE);
        btnImpr.addActionListener(e -> {
            if (job.printDialog()) {
                try { job.print(); } catch (Exception ex) { showError("Erro ao imprimir: " + ex.getMessage()); }
            }
        });
        rod.add(btnImpr);
        rod.add(UIFactory.bigActionButton("Fechar", e -> preview.dispose()));
        preview.add(rod, BorderLayout.SOUTH);
        preview.setVisible(true);
    }

    private JPanel buildFolhaPedido(Pedido ped) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 30, 20, 30));

        Font fTit  = new Font("Tahoma", Font.BOLD, 16);
        Font fSub  = new Font("Tahoma", Font.BOLD, 12);
        Font fNorm = new Font("Tahoma", Font.PLAIN, 11);
        Font fBold = new Font("Tahoma", Font.BOLD, 11);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        addLinhaPedido(p, "CARMEL SISTEMA DE GESTÃO", fTit, new Color(0,60,140), SwingConstants.CENTER);
        addLinhaPedido(p, "PEDIDO DE VENDA", fSub, Color.BLACK, SwingConstants.CENTER);
        addLinhaPedido(p, "⚠ PEDIDO PENDENTE — AGUARDANDO ATENDIMENTO", new Font("Tahoma", Font.BOLD, 11), new Color(180,60,0), SwingConstants.CENTER);
        p.add(Box.createRigidArea(new Dimension(0, 8)));
        p.add(sepPedido());

        addLinhaPedido(p, "Pedido Nº: " + ped.getId() + "   |   Data: " + ped.getDataPedido().format(fmt), fNorm, Color.BLACK, SwingConstants.LEFT);
        addLinhaPedido(p, "Cliente: " + ped.getCliente().getNome(), fBold, Color.BLACK, SwingConstants.LEFT);
        if (ped.getCliente().getCpf() != null && !ped.getCliente().getCpf().isEmpty())
            addLinhaPedido(p, "CPF: " + ped.getCliente().getCpf(), fNorm, Color.BLACK, SwingConstants.LEFT);
        if (ped.getObservacoes() != null && !ped.getObservacoes().isEmpty())
            addLinhaPedido(p, "Obs: " + ped.getObservacoes(), fNorm, Color.GRAY, SwingConstants.LEFT);

        p.add(Box.createRigidArea(new Dimension(0, 10)));
        addLinhaPedido(p, "ITENS DO PEDIDO", fSub, new Color(0,84,166), SwingConstants.LEFT);
        p.add(sepPedido());

        // Cabeçalho tabela
        JPanel header = new JPanel(new GridLayout(1, 4));
        header.setBackground(new Color(10, 36, 106));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        for (String h : new String[]{"Produto", "Qtd", "Preço Unit.", "Subtotal"}) {
            JLabel l = new JLabel("  " + h); l.setFont(fBold); l.setForeground(Color.WHITE); header.add(l);
        }
        p.add(header);

        BigDecimal total = BigDecimal.ZERO;
        int idx = 0;
        for (ItensPedido item : ped.getItensPedidos()) {
            JPanel row = new JPanel(new GridLayout(1, 4));
            row.setBackground(idx++ % 2 == 0 ? Color.WHITE : new Color(245,245,250));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
            total = total.add(sub);
            addCelula(row, item.getProduto().getNome(), fNorm);
            addCelula(row, String.valueOf(item.getQuantidade()), fNorm);
            addCelula(row, String.format("R$ %.2f", item.getPrecoUnitario()), fNorm);
            addCelula(row, String.format("R$ %.2f", sub), fNorm);
            p.add(row);
        }
        p.add(sepPedido());

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lTot = new JLabel("TOTAL DO PEDIDO:"); lTot.setFont(fBold);
        JLabel vTot = new JLabel(String.format("R$ %.2f", total));
        vTot.setFont(new Font("Tahoma", Font.BOLD, 13));
        vTot.setForeground(new Color(0,84,166));
        vTot.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(lTot, BorderLayout.WEST); totalRow.add(vTot, BorderLayout.EAST);
        p.add(totalRow);

        p.add(Box.createRigidArea(new Dimension(0, 30)));
        addLinhaPedido(p, "_________________________________          _________________________________", fNorm, Color.BLACK, SwingConstants.CENTER);
        addLinhaPedido(p, "          Atendente                                   Cliente / Responsável", fNorm, Color.GRAY, SwingConstants.CENTER);

        return p;
    }

    private void addLinhaPedido(JPanel p, String text, Font font, Color color, int align) {
        JLabel l = new JLabel(text, align);
        l.setFont(font); l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        p.add(l);
    }

    private void addCelula(JPanel row, String text, Font font) {
        JLabel l = new JLabel("  " + text); l.setFont(font); row.add(l);
    }

    private JSeparator sepPedido() {
        JSeparator s = new JSeparator();
        s.setForeground(new Color(180,180,180));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    public void reloadPedidos() {
        pedidosModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Pedido> list = em.createQuery(
                    "SELECT p FROM Pedido p ORDER BY p.dataPedido DESC", Pedido.class).getResultList();
            popularTabela(list);
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (em != null) em.close(); }
    }

    private void buscarPedidos() {
        // Lê e valida filtros
        String cliente  = tfBuscaCliente.getText().trim();
        String dataDe   = tfBuscaDataDe.getText().trim();
        String dataAte  = tfBuscaDataAte.getText().trim();
        String valorMin = tfBuscaValorMin.getText().trim().replace(",", ".");
        String valorMax = tfBuscaValorMax.getText().trim().replace(",", ".");

        DateTimeFormatter fmtIn = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDate de  = null;
        LocalDate ate = null;
        BigDecimal vMin = null;
        BigDecimal vMax = null;

        // Valida datas
        if (!dataDe.isEmpty()) {
            try { de = LocalDate.parse(dataDe, fmtIn); }
            catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this,
                        "Data 'de' inválida. Use o formato dd/MM/yyyy (ex: 01/03/2026).",
                        "Atenção", JOptionPane.WARNING_MESSAGE);
                tfBuscaDataDe.requestFocus(); return;
            }
        }
        if (!dataAte.isEmpty()) {
            try { ate = LocalDate.parse(dataAte, fmtIn); }
            catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this,
                        "Data 'até' inválida. Use o formato dd/MM/yyyy (ex: 31/03/2026).",
                        "Atenção", JOptionPane.WARNING_MESSAGE);
                tfBuscaDataAte.requestFocus(); return;
            }
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            JOptionPane.showMessageDialog(this,
                    "A data inicial não pode ser maior que a data final.",
                    "Atenção", JOptionPane.WARNING_MESSAGE); return;
        }

        // Valida valores
        if (!valorMin.isEmpty()) {
            try { vMin = new BigDecimal(valorMin); if (vMin.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Valor mínimo inválido.", "Atenção", JOptionPane.WARNING_MESSAGE);
                tfBuscaValorMin.requestFocus(); return;
            }
        }
        if (!valorMax.isEmpty()) {
            try { vMax = new BigDecimal(valorMax); if (vMax.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Valor máximo inválido.", "Atenção", JOptionPane.WARNING_MESSAGE);
                tfBuscaValorMax.requestFocus(); return;
            }
        }
        if (vMin != null && vMax != null && vMin.compareTo(vMax) > 0) {
            JOptionPane.showMessageDialog(this,
                    "O valor mínimo não pode ser maior que o valor máximo.",
                    "Atenção", JOptionPane.WARNING_MESSAGE); return;
        }

        // Monta query dinâmica
        StringBuilder jpql = new StringBuilder("SELECT p FROM Pedido p WHERE 1=1");
        if (!cliente.isEmpty())  jpql.append(" AND LOWER(p.cliente.nome) LIKE :nome");
        if (de  != null)         jpql.append(" AND p.dataPedido >= :de");
        if (ate != null)         jpql.append(" AND p.dataPedido <= :ate");
        if (vMin != null)        jpql.append(" AND p.valorTotal >= :vMin");
        if (vMax != null)        jpql.append(" AND p.valorTotal <= :vMax");
        jpql.append(" ORDER BY p.dataPedido DESC");

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            var query = em.createQuery(jpql.toString(), Pedido.class);

            if (!cliente.isEmpty())  query.setParameter("nome", "%" + cliente.toLowerCase() + "%");
            if (de  != null)         query.setParameter("de",  de.atStartOfDay());
            if (ate != null)         query.setParameter("ate", ate.atTime(23, 59, 59));
            if (vMin != null)        query.setParameter("vMin", vMin);
            if (vMax != null)        query.setParameter("vMax", vMax);

            List<Pedido> result = query.getResultList();
            pedidosModel.setRowCount(0);
            popularTabela(result);

            // Atualiza label de resultado
            atualizarLabelResultado(result.size());

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao buscar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } finally { if (em != null) em.close(); }
    }

    private void limparBusca() {
        tfBuscaCliente.setText("");
        tfBuscaDataDe.setText("");
        tfBuscaDataAte.setText("");
        tfBuscaValorMin.setText("");
        tfBuscaValorMax.setText("");
        atualizarLabelResultado(-1);
    }

    private void popularTabela(List<Pedido> list) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        pedidosModel.setRowCount(0);
        for (Pedido p : list) {
            pedidosModel.addRow(new Object[]{
                    p.getId(),
                    p.getCliente().getNome(),
                    p.getDataPedido().format(fmt),
                    p.getStatus(),
                    p.getValorTotal() != null ? String.format("R$ %.2f", p.getValorTotal()) : "R$ 0,00"
            });
        }
        if (pedidosModel.getRowCount() > 0) {
            pedidosTable.setRowSelectionInterval(0, 0);
            loadItensDoPedido((Long) pedidosModel.getValueAt(0, 0));
        } else {
            itensModel.setRowCount(0);
        }
    }

    private void abrirRelatorioPedidoSelecionado() {
        int row = pedidosTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um pedido na tabela primeiro.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long pedidoId = (Long) pedidosModel.getValueAt(row, 0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Pedido pedido = em.createQuery(
                            "SELECT p FROM Pedido p " +
                                    "LEFT JOIN FETCH p.itensPedidos i " +
                                    "LEFT JOIN FETCH i.produto " +
                                    "LEFT JOIN FETCH p.cliente " +
                                    "WHERE p.id = :id", Pedido.class)
                    .setParameter("id", pedidoId)
                    .getSingleResult();

            // Busca pagamento associado (pode não existir em pedidos antigos)
            List<br.carmel.model.Pagamento> pags = em.createQuery(
                            "SELECT pag FROM Pagamento pag WHERE pag.pedido.id = :id",
                            br.carmel.model.Pagamento.class)
                    .setParameter("id", pedidoId)
                    .getResultList();
            br.carmel.model.Pagamento pagamento = pags.isEmpty() ? null : pags.get(0);

            new br.carmel.ui.dialogs.RelatorioPedidoDialog(
                    SwingUtilities.getWindowAncestor(this), pedido, pagamento);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao abrir relatório: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } finally { if (em != null) em.close(); }
    }

    private void atualizarLabelResultado(int count) {
        findAndSetLabel(this, count);
    }

    private void findAndSetLabel(Container container, int count) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel l && "lblResultado".equals(l.getName())) {
                if (count < 0) l.setText("Todos os pedidos");
                else l.setText("Resultado da busca: " + count + " pedido(s) encontrado(s)");
                return;
            }
            if (c instanceof Container sub) findAndSetLabel(sub, count);
        }
    }

    private void loadItensDoPedido(Long pedidoId) {
        itensModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<ItensPedido> itens = em.createQuery(
                            "SELECT i FROM ItensPedido i WHERE i.pedido.id = :pid", ItensPedido.class)
                    .setParameter("pid", pedidoId).getResultList();
            for (ItensPedido i : itens) {
                itensModel.addRow(new Object[]{
                        i.getProduto().getNome(),
                        i.getProduto().getCodBarras() != null ? i.getProduto().getCodBarras() : "-",
                        String.format("R$ %.2f", i.getPrecoUnitario()),
                        i.getQuantidade(),
                        String.format("R$ %.2f", i.getSubtotal())
                });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (em != null) em.close(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateTotal(JLabel lbl) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < orderItemsModel.size(); i++) {
            ItensPedido it = orderItemsModel.get(i);
            if (it.getSubtotal() != null) total = total.add(it.getSubtotal());
        }
        lbl.setText("Total: R$ " + total);
    }

    private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
    private void rollback(EntityManager em) { if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback(); }
    private void close(EntityManager em)    { if (em != null) em.close(); }

    @SuppressWarnings("unchecked")
    private <T> void styleCombo(JComboBox<T> cb, Class<T> clazz) {
        cb.setFont(UIFactory.FONT_NORMAL);
        cb.setBackground(Color.WHITE);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(UIFactory.FONT_NORMAL);
                if (value instanceof Cliente c) setText(c.getId() + " - " + c.getNome());
                else if (value instanceof Produto p) setText(p.getId() + " - " + p.getNome() + "  (R$ " + p.getValor() + ")");
                return this;
            }
        });
    }
}