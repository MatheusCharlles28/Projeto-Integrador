package br.carmel.ui.panels;


import br.carmel.model.*;
import br.carmel.util.CnpjClient;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;
import br.carmel.model.viaCepClient;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ClientePanel extends JPanel {

    private final EntityManagerFactory emf;

    // ── Campos Cliente PF ─────────────────────────────────────────────────────
    private final JTextField tfNome   = UIFactory.styledField("");
    private final JTextField tfCpf    = UIFactory.styledField("");
    private final JTextField tfTel    = UIFactory.styledField("");
    private final JTextField tfEmail  = UIFactory.styledField("");
    private final JTextField tfNumero = UIFactory.styledField("");
    private final JTextField tfCep    = UIFactory.styledField("");
    private final JTextField tfLog    = UIFactory.styledField("");
    private final JTextField tfComp   = UIFactory.styledField("");
    private final JTextField tfBairro = UIFactory.styledField("");
    private final JTextField tfCidade = UIFactory.styledField("");
    private final JTextField tfUf     = UIFactory.styledField("");

    // ── Campos Cliente PJ ─────────────────────────────────────────────────────
    private final JTextField tfPJRazao    = UIFactory.styledField("");
    private final JTextField tfPJFantasia = UIFactory.styledField("");
    private final JTextField tfPJCnpj     = UIFactory.styledField("");
    private final JTextField tfPJIE       = UIFactory.styledField("");
    private final JTextField tfPJTel      = UIFactory.styledField("");
    private final JTextField tfPJEmail    = UIFactory.styledField("");
    private final JTextField tfPJContato  = UIFactory.styledField("");
    private final JTextField tfPJCep      = UIFactory.styledField("");
    private final JTextField tfPJLog      = UIFactory.styledField("");
    private final JTextField tfPJNumero   = UIFactory.styledField("");
    private final JTextField tfPJBairro   = UIFactory.styledField("");
    private final JTextField tfPJCidade   = UIFactory.styledField("");
    private final JTextField tfPJUf       = UIFactory.styledField("");

    // ── Campos Fornecedor ─────────────────────────────────────────────────────
    private final JTextField tfFRazao    = UIFactory.styledField("");
    private final JTextField tfFFantasia = UIFactory.styledField("");
    private final JTextField tfFCnpj     = UIFactory.styledField("");
    private final JTextField tfFIE       = UIFactory.styledField("");
    private final JTextField tfFTel      = UIFactory.styledField("");
    private final JTextField tfFEmail    = UIFactory.styledField("");
    private final JTextField tfFContato  = UIFactory.styledField("");
    private final JTextField tfFCep      = UIFactory.styledField("");
    private final JTextField tfFLog      = UIFactory.styledField("");
    private final JTextField tfFNumero   = UIFactory.styledField("");
    private final JTextField tfFBairro   = UIFactory.styledField("");
    private final JTextField tfFCidade   = UIFactory.styledField("");
    private final JTextField tfFUf       = UIFactory.styledField("");

    // ── Estado ────────────────────────────────────────────────────────────────
    private Long idClientePFSel  = null;
    private Long idClientePJSel  = null;
    private Long idFornecedorSel = null;

    // ── Tabelas ───────────────────────────────────────────────────────────────
    private DefaultTableModel pfModel;
    private JTable pfTable;
    private DefaultTableModel pjModel;
    private JTable pjTable;
    private DefaultTableModel fornModel;
    private JTable fornTable;

    // ── Abas de formulário e tabela ───────────────────────────────────────────
    private JTabbedPane formTabs;
    private JTabbedPane tableTabs;

    public ClientePanel(EntityManagerFactory emf) {
        this.emf = emf;
        setLayout(new BorderLayout());
        setBackground(UIFactory.XP_BG);


        build();
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build() {
        add(UIFactory.xpTitleBar("Cadastro de Clientes e Fornecedores"), BorderLayout.NORTH);

        // Abas de formulário (esquerda)
        formTabs = new JTabbedPane();
        formTabs.setFont(UIFactory.FONT_BOLD);
        formTabs.setBackground(UIFactory.XP_BG);
        formTabs.addTab("👤 Pessoa Física",   buildFormPF());
        formTabs.addTab("🏢 Pessoa Jurídica", buildFormPJ());
        formTabs.addTab("🏭 Fornecedor",      buildFormFornecedor());

        // Sincroniza aba do formulário com aba da tabela
        formTabs.addChangeListener(e -> {
            if (tableTabs != null) tableTabs.setSelectedIndex(formTabs.getSelectedIndex());
            limparAtivo();
        });

        // Abas de tabela (direita)
        tableTabs = new JTabbedPane();
        tableTabs.setFont(UIFactory.FONT_BOLD);
        tableTabs.addTab("👤 Pessoa Física",   buildTabelaPF());
        tableTabs.addTab("🏢 Pessoa Jurídica", buildTabelaPJ());
        tableTabs.addTab("🏭 Fornecedor",      buildTabelaFornecedor());

        tableTabs.addChangeListener(e -> {
            if (formTabs != null) formTabs.setSelectedIndex(tableTabs.getSelectedIndex());
            limparAtivo();
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formTabs, tableTabs);
        split.setDividerLocation(370);
        split.setDividerSize(5);
        split.setBackground(UIFactory.XP_BG);

        add(split, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);
    }

    // ── Formulário PF ─────────────────────────────────────────────────────────

    private JPanel buildFormPF() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);
        outer.setBorder(new EmptyBorder(8, 8, 4, 4));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(UIFactory.groupBorder("Cliente — Pessoa Física"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, c, row++, "Nome:",        tfNome);
        addRow(form, c, row++, "CPF:",         tfCpf);
        addRow(form, c, row++, "Telefone:",    tfTel);
        addRow(form, c, row++, "E-mail:",      tfEmail);
        addRow(form, c, row++, "Número:",      tfNumero);

        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("CEP:"), c);
        JButton btnCep = UIFactory.bigSmallButton("Buscar");
        JPanel cepRow = new JPanel(new BorderLayout(4, 0));
        cepRow.setOpaque(false);
        cepRow.add(tfCep, BorderLayout.CENTER);
        cepRow.add(btnCep, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; form.add(cepRow, c); row++;

        addRow(form, c, row++, "Logradouro:",  tfLog);
        addRow(form, c, row++, "Complemento:", tfComp);
        addRow(form, c, row++, "Bairro:",      tfBairro);
        addRow(form, c, row++, "Cidade:",      tfCidade);
        addRow(form, c, row,   "UF:",          tfUf);

        c.gridx = 0; c.gridy = row + 1; c.gridwidth = 2; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        btnCep.addActionListener(e -> buscarCep(tfCep, tfLog, tfBairro, tfCidade, tfUf));
        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    // ── Formulário PJ (Cliente Jurídico) ──────────────────────────────────────

    private JPanel buildFormPJ() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);
        outer.setBorder(new EmptyBorder(8, 8, 4, 4));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(UIFactory.groupBorder("Cliente — Pessoa Jurídica (Empresa)"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, c, row++, "Razão Social:",       tfPJRazao);
        addRow(form, c, row++, "Nome Fantasia:",      tfPJFantasia);

        // CNPJ com botão buscar
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("CNPJ:"), c);
        JButton btnCnpjPJ = UIFactory.bigSmallButton("Buscar");
        btnCnpjPJ.setBackground(new Color(0, 84, 166)); btnCnpjPJ.setForeground(Color.WHITE);
        JPanel cnpjPJRow = new JPanel(new BorderLayout(4, 0));
        cnpjPJRow.setOpaque(false);
        cnpjPJRow.add(tfPJCnpj, BorderLayout.CENTER);
        cnpjPJRow.add(btnCnpjPJ, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; form.add(cnpjPJRow, c); row++;

        addRow(form, c, row++, "Inscrição Estadual:", tfPJIE);
        addRow(form, c, row++, "Telefone:",           tfPJTel);
        addRow(form, c, row++, "E-mail:",             tfPJEmail);
        addRow(form, c, row++, "Responsável:",        tfPJContato);

        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("CEP:"), c);
        JButton btnCepPJ = UIFactory.bigSmallButton("Buscar");
        JPanel cepPJRow = new JPanel(new BorderLayout(4, 0));
        cepPJRow.setOpaque(false);
        cepPJRow.add(tfPJCep, BorderLayout.CENTER);
        cepPJRow.add(btnCepPJ, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; form.add(cepPJRow, c); row++;

        addRow(form, c, row++, "Logradouro:", tfPJLog);
        addRow(form, c, row++, "Número:",     tfPJNumero);
        addRow(form, c, row++, "Bairro:",     tfPJBairro);
        addRow(form, c, row++, "Cidade:",     tfPJCidade);
        addRow(form, c, row,   "UF:",         tfPJUf);

        c.gridx = 0; c.gridy = row + 1; c.gridwidth = 2; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        btnCepPJ.addActionListener(e -> buscarCep(tfPJCep, tfPJLog, tfPJBairro, tfPJCidade, tfPJUf));
        btnCnpjPJ.addActionListener(e -> buscarCnpj(tfPJCnpj, tfPJRazao, tfPJFantasia,
                tfPJTel, tfPJEmail, tfPJCep, tfPJLog, tfPJNumero, tfPJBairro, tfPJCidade, tfPJUf));

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    // ── Formulário Fornecedor ─────────────────────────────────────────────────

    private JPanel buildFormFornecedor() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(UIFactory.XP_BG);
        outer.setBorder(new EmptyBorder(8, 8, 4, 4));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(UIFactory.groupBorder("Fornecedor / Empresa Vendedora"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, c, row++, "Razão Social:",       tfFRazao);
        addRow(form, c, row++, "Nome Fantasia:",      tfFFantasia);

        // CNPJ com botão buscar
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("CNPJ:"), c);
        JButton btnCnpjF = UIFactory.bigSmallButton("Buscar");
        btnCnpjF.setBackground(new Color(140, 60, 0)); btnCnpjF.setForeground(Color.WHITE);
        JPanel cnpjFRow = new JPanel(new BorderLayout(4, 0));
        cnpjFRow.setOpaque(false);
        cnpjFRow.add(tfFCnpj, BorderLayout.CENTER);
        cnpjFRow.add(btnCnpjF, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; form.add(cnpjFRow, c); row++;

        addRow(form, c, row++, "Inscrição Estadual:", tfFIE);
        addRow(form, c, row++, "Telefone:",           tfFTel);
        addRow(form, c, row++, "E-mail:",             tfFEmail);
        addRow(form, c, row++, "Contato:",            tfFContato);

        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight("CEP:"), c);
        JButton btnCepF = UIFactory.bigSmallButton("Buscar");
        JPanel cepFRow = new JPanel(new BorderLayout(4, 0));
        cepFRow.setOpaque(false);
        cepFRow.add(tfFCep, BorderLayout.CENTER);
        cepFRow.add(btnCepF, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1; form.add(cepFRow, c); row++;

        addRow(form, c, row++, "Logradouro:", tfFLog);
        addRow(form, c, row++, "Número:",     tfFNumero);
        addRow(form, c, row++, "Bairro:",     tfFBairro);
        addRow(form, c, row++, "Cidade:",     tfFCidade);
        addRow(form, c, row,   "UF:",         tfFUf);

        c.gridx = 0; c.gridy = row + 1; c.gridwidth = 2; c.weighty = 1.0;
        form.add(Box.createVerticalGlue(), c);

        btnCepF.addActionListener(e -> buscarCep(tfFCep, tfFLog, tfFBairro, tfFCidade, tfFUf));
        btnCnpjF.addActionListener(e -> buscarCnpj(tfFCnpj, tfFRazao, tfFFantasia,
                tfFTel, tfFEmail, tfFCep, tfFLog, tfFNumero, tfFBairro, tfFCidade, tfFUf));

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    // ── Tabelas ───────────────────────────────────────────────────────────────

    private JPanel buildTabelaPF() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        pfModel = new DefaultTableModel(
                new String[]{"ID", "Nome", "CPF", "Telefone", "E-mail"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pfTable = UIFactory.styledTable();
        pfTable.setModel(pfModel);
        pfTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pfTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        pfTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        pfTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        pfTable.getColumnModel().getColumn(4).setPreferredWidth(130);

        pfTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pfTable.getSelectedRow() >= 0)
                preencherPF((Long) pfModel.getValueAt(pfTable.getSelectedRow(), 0));
        });

        JScrollPane scroll = new JScrollPane(pfTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTabelaPJ() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        pjModel = new DefaultTableModel(
                new String[]{"ID", "Razão Social", "CNPJ", "Telefone", "Cidade"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        pjTable = UIFactory.styledTable();
        pjTable.setModel(pjModel);
        pjTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        pjTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        pjTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        pjTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        pjTable.getColumnModel().getColumn(4).setPreferredWidth(110);

        pjTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && pjTable.getSelectedRow() >= 0)
                preencherPJ((Long) pjModel.getValueAt(pjTable.getSelectedRow(), 0));
        });

        JScrollPane scroll = new JScrollPane(pjTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTabelaFornecedor() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(UIFactory.XP_BG);
        p.setBorder(new EmptyBorder(8, 4, 8, 8));

        fornModel = new DefaultTableModel(
                new String[]{"ID", "Razão Social", "CNPJ", "Telefone", "Cidade"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        fornTable = UIFactory.styledTable();
        fornTable.setModel(fornModel);
        fornTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        fornTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        fornTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        fornTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        fornTable.getColumnModel().getColumn(4).setPreferredWidth(110);

        fornTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && fornTable.getSelectedRow() >= 0)
                preencherFornecedor((Long) fornModel.getValueAt(fornTable.getSelectedRow(), 0));
        });

        JScrollPane scroll = new JScrollPane(fornTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(172, 168, 153)));
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── Barra de botões ───────────────────────────────────────────────────────

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(new Color(212, 208, 200));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        // Label mostra qual tipo está ativo
        JLabel lblTipo = new JLabel("Pessoa Física");
        lblTipo.setFont(UIFactory.FONT_BOLD);
        lblTipo.setForeground(new Color(0, 84, 166));
        lblTipo.setBorder(new EmptyBorder(0, 4, 0, 8));
        bar.add(lblTipo);

        // Botões únicos — agem sobre a aba ativa
        bar.add(UIFactory.bigActionButton("Novo",      e -> novo()));
        bar.add(UIFactory.bigActionButton("Salvar",    e -> salvar()));
        bar.add(UIFactory.bigActionButton("Atualizar", e -> atualizar()));
        bar.add(UIFactory.bigActionButton("Excluir",   e -> excluir()));
        bar.add(new JSeparator(JSeparator.VERTICAL));
        bar.add(UIFactory.bigActionButton("↻ Atualizar Lista", e -> reloadTable()));

        // Atualiza label quando aba muda
        formTabs.addChangeListener(e -> {
            int idx = formTabs.getSelectedIndex();
            String[] nomes = {"Pessoa Física", "Pessoa Jurídica", "Fornecedor"};
            Color[] cores  = {new Color(0, 84, 166), new Color(0, 120, 60), new Color(140, 60, 0)};
            if (idx >= 0 && idx < nomes.length) {
                lblTipo.setText(nomes[idx]);
                lblTipo.setForeground(cores[idx]);
            }
        });

        return bar;
    }

    // ── Botões únicos que delegam para a aba ativa ────────────────────────────

    private void novo() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> limparPF();
            case 1 -> limparPJ();
            case 2 -> limparFornecedor();
        }
    }

    private void salvar() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> salvarPF();
            case 1 -> salvarPJ();
            case 2 -> salvarFornecedor();
        }
    }

    private void atualizar() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> atualizarPF();
            case 1 -> atualizarPJ();
            case 2 -> atualizarFornecedor();
        }
    }

    private void excluir() {
        switch (formTabs.getSelectedIndex()) {
            case 0 -> excluirPF();
            case 1 -> excluirPJ();
            case 2 -> excluirFornecedor();
        }
    }

    // ── Ações PF ──────────────────────────────────────────────────────────────

    private void limparPF() {
        idClientePFSel = null;
        tfNome.setText(""); tfCpf.setText(""); tfTel.setText(""); tfEmail.setText("");
        tfNumero.setText(""); tfCep.setText(""); tfLog.setText(""); tfComp.setText("");
        tfBairro.setText(""); tfCidade.setText(""); tfUf.setText("");
        pfTable.clearSelection();
        formTabs.setSelectedIndex(0);
    }

    private void salvarPF() {
        Validator.Result v = Validator.validarCliente(
                tfNome.getText(), tfCpf.getText(), tfTel.getText(), tfEmail.getText(),
                tfCep.getText(), tfLog.getText(), tfBairro.getText(),
                tfCidade.getText(), tfUf.getText(), tfNumero.getText());
        if (!v.isValido()) { showError("Corrija os campos:\n\n" + v.getMensagem()); return; }

        tfCpf.setText(Validator.formatarCpf(tfCpf.getText()));
        tfTel.setText(Validator.formatarTelefone(tfTel.getText()));
        tfCep.setText(Validator.formatarCep(tfCep.getText()));
        tfUf.setText(tfUf.getText().trim().toUpperCase());

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Cliente obj = new Cliente();
            applyFormToPF(obj);
            Endereco end = buildEndereco();
            end.setCliente(obj);
            obj.getEnderecos().add(end);
            em.persist(obj);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Cliente (PF) salvo!");
            limparPF(); reloadPF();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe um cliente com esse Nome ou CPF.");
            else showError("Erro ao salvar: " + msg);
        } finally { close(em); }
    }

    private void atualizarPF() {
        if (idClientePFSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente na lista."); return; }
        Validator.Result v = Validator.validarCliente(
                tfNome.getText(), tfCpf.getText(), tfTel.getText(), tfEmail.getText(),
                tfCep.getText(), tfLog.getText(), tfBairro.getText(),
                tfCidade.getText(), tfUf.getText(), tfNumero.getText());
        if (!v.isValido()) { showError("Corrija os campos:\n\n" + v.getMensagem()); return; }

        tfCpf.setText(Validator.formatarCpf(tfCpf.getText()));
        tfTel.setText(Validator.formatarTelefone(tfTel.getText()));
        tfCep.setText(Validator.formatarCep(tfCep.getText()));
        tfUf.setText(tfUf.getText().trim().toUpperCase());

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Cliente managed = em.find(Cliente.class, idClientePFSel);
            if (managed == null) { em.getTransaction().rollback(); return; }
            applyFormToPF(managed);
            if (managed.getEnderecos() == null || managed.getEnderecos().isEmpty()) {
                Endereco e = new Endereco(); e.setCliente(managed); managed.getEnderecos().add(e);
            }
            applyFormToEndereco(managed.getEnderecos().get(0));
            em.merge(managed);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Cliente (PF) atualizado.");
            reloadPF();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe outro cliente com esse Nome ou CPF.");
            else showError("Erro ao atualizar: " + msg);
        } finally { close(em); }
    }

    private void excluirPF() {
        if (idClientePFSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente."); return; }
        if (JOptionPane.showConfirmDialog(this, "Confirma exclusão?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Cliente cc = em.find(Cliente.class, idClientePFSel);
            if (cc != null) { em.remove(cc); em.getTransaction().commit(); JOptionPane.showMessageDialog(this, "Cliente removido."); }
            else em.getTransaction().rollback();
            limparPF(); reloadPF();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); }
        finally { close(em); }
    }

    // ── Ações PJ ──────────────────────────────────────────────────────────────

    private void limparPJ() {
        idClientePJSel = null;
        tfPJRazao.setText(""); tfPJFantasia.setText(""); tfPJCnpj.setText(""); tfPJIE.setText("");
        tfPJTel.setText(""); tfPJEmail.setText(""); tfPJContato.setText("");
        tfPJCep.setText(""); tfPJLog.setText(""); tfPJNumero.setText("");
        tfPJBairro.setText(""); tfPJCidade.setText(""); tfPJUf.setText("");
        pjTable.clearSelection();
        formTabs.setSelectedIndex(1);
    }

    private void salvarPJ() {
        if (Validator.isBlank(tfPJRazao.getText())) { showError("Razão Social é obrigatória."); return; }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            ClienteJuridico obj = new ClienteJuridico();
            applyFormToPJ(obj);
            em.persist(obj);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Cliente (PJ) salvo!");
            limparPJ(); reloadPJ();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe um cliente PJ com esse CNPJ.");
            else showError("Erro ao salvar: " + msg);
        } finally { close(em); }
    }

    private void atualizarPJ() {
        if (idClientePJSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente PJ na lista."); return; }
        if (Validator.isBlank(tfPJRazao.getText())) { showError("Razão Social é obrigatória."); return; }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            ClienteJuridico managed = em.find(ClienteJuridico.class, idClientePJSel);
            if (managed == null) { em.getTransaction().rollback(); return; }
            applyFormToPJ(managed);
            em.merge(managed);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Cliente (PJ) atualizado.");
            reloadPJ();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe outro cliente PJ com esse CNPJ.");
            else showError("Erro ao atualizar: " + msg);
        } finally { close(em); }
    }

    private void excluirPJ() {
        if (idClientePJSel == null) { JOptionPane.showMessageDialog(this, "Selecione um cliente PJ."); return; }
        if (JOptionPane.showConfirmDialog(this, "Confirma exclusão?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            ClienteJuridico obj = em.find(ClienteJuridico.class, idClientePJSel);
            if (obj != null) { em.remove(obj); em.getTransaction().commit(); JOptionPane.showMessageDialog(this, "Cliente PJ removido."); }
            else em.getTransaction().rollback();
            limparPJ(); reloadPJ();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); }
        finally { close(em); }
    }

    // ── Ações Fornecedor ──────────────────────────────────────────────────────

    private void limparFornecedor() {
        idFornecedorSel = null;
        tfFRazao.setText(""); tfFFantasia.setText(""); tfFCnpj.setText(""); tfFIE.setText("");
        tfFTel.setText(""); tfFEmail.setText(""); tfFContato.setText("");
        tfFCep.setText(""); tfFLog.setText(""); tfFNumero.setText("");
        tfFBairro.setText(""); tfFCidade.setText(""); tfFUf.setText("");
        fornTable.clearSelection();
        formTabs.setSelectedIndex(2);
    }

    private void salvarFornecedor() {
        if (Validator.isBlank(tfFRazao.getText())) { showError("Razão Social é obrigatória."); return; }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Fornecedor f = new Fornecedor();
            applyFormToFornecedor(f);
            em.persist(f);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Fornecedor salvo!");
            limparFornecedor(); reloadFornecedor();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe um fornecedor com esse CNPJ.");
            else showError("Erro ao salvar: " + msg);
        } finally { close(em); }
    }

    private void atualizarFornecedor() {
        if (idFornecedorSel == null) { JOptionPane.showMessageDialog(this, "Selecione um fornecedor na lista."); return; }
        if (Validator.isBlank(tfFRazao.getText())) { showError("Razão Social é obrigatória."); return; }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Fornecedor managed = em.find(Fornecedor.class, idFornecedorSel);
            if (managed == null) { em.getTransaction().rollback(); return; }
            applyFormToFornecedor(managed);
            em.merge(managed);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Fornecedor atualizado.");
            reloadFornecedor();
        } catch (Exception ex) {
            rollback(em);
            String msg = ex.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate")))
                showError("Já existe outro fornecedor com esse CNPJ.");
            else showError("Erro ao atualizar: " + msg);
        } finally { close(em); }
    }

    private void excluirFornecedor() {
        if (idFornecedorSel == null) { JOptionPane.showMessageDialog(this, "Selecione um fornecedor."); return; }
        if (JOptionPane.showConfirmDialog(this, "Confirma exclusão?", "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Fornecedor f = em.find(Fornecedor.class, idFornecedorSel);
            if (f != null) { em.remove(f); em.getTransaction().commit(); JOptionPane.showMessageDialog(this, "Fornecedor removido."); }
            else em.getTransaction().rollback();
            limparFornecedor(); reloadFornecedor();
        } catch (Exception ex) { rollback(em); showError("Erro: " + ex.getMessage()); }
        finally { close(em); }
    }

    // ── Reload ────────────────────────────────────────────────────────────────

    public void reloadTable() { reloadPF(); reloadPJ(); reloadFornecedor(); }

    private void reloadPF() {
        pfModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Cliente> list = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nome", Cliente.class).getResultList();
            for (Cliente c : list)
                pfModel.addRow(new Object[]{ c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), c.getEmail() });
        } catch (Exception ex) { ex.printStackTrace(); } finally { close(em); }
    }

    private void reloadPJ() {
        pjModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<ClienteJuridico> list = em.createQuery("SELECT c FROM ClienteJuridico c ORDER BY c.razaoSocial", ClienteJuridico.class).getResultList();
            for (ClienteJuridico c : list)
                pjModel.addRow(new Object[]{ c.getId(), c.getRazaoSocial(), c.getCnpj(), c.getTelefone(), c.getCidade() });
        } catch (Exception ex) { ex.printStackTrace(); } finally { close(em); }
    }

    private void reloadFornecedor() {
        fornModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Fornecedor> list = em.createQuery("SELECT f FROM Fornecedor f ORDER BY f.razaoSocial", Fornecedor.class).getResultList();
            for (Fornecedor f : list)
                fornModel.addRow(new Object[]{ f.getId(), f.getRazaoSocial(), f.getCnpj(), f.getTelefone(), f.getCidade() });
        } catch (Exception ex) { ex.printStackTrace(); } finally { close(em); }
    }

    // ── Preencher formulários ─────────────────────────────────────────────────

    private void preencherPF(Long id) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Cliente c = em.find(Cliente.class, id);
            if (c == null) return;
            idClientePFSel = id;
            tfNome.setText(c.getNome()); tfCpf.setText(c.getCpf());
            tfTel.setText(c.getTelefone()); tfEmail.setText(c.getEmail() != null ? c.getEmail() : "");
            if (c.getEnderecos() != null && !c.getEnderecos().isEmpty()) {
                Endereco e0 = c.getEnderecos().get(0);
                tfNumero.setText(String.valueOf(e0.getNumero()));
                tfCep.setText(e0.getCep()); tfLog.setText(e0.getLogradouro());
                tfComp.setText(e0.getComplemento() != null ? e0.getComplemento() : "");
                tfBairro.setText(e0.getBairro()); tfCidade.setText(e0.getLocalidade()); tfUf.setText(e0.getUf());
            }
        } catch (Exception ex) { ex.printStackTrace(); } finally { close(em); }
    }

    private void preencherPJ(Long id) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            ClienteJuridico c = em.find(ClienteJuridico.class, id);
            if (c == null) return;
            idClientePJSel = id;
            tfPJRazao.setText(n(c.getRazaoSocial()));   tfPJFantasia.setText(n(c.getNomeFantasia()));
            tfPJCnpj.setText(n(c.getCnpj()));            tfPJIE.setText(n(c.getInscricaoEstadual()));
            tfPJTel.setText(n(c.getTelefone()));         tfPJEmail.setText(n(c.getEmail()));
            tfPJContato.setText(n(c.getContato()));      tfPJCep.setText(n(c.getCep()));
            tfPJLog.setText(n(c.getLogradouro()));       tfPJNumero.setText(n(c.getNumero()));
            tfPJBairro.setText(n(c.getBairro()));        tfPJCidade.setText(n(c.getCidade()));
            tfPJUf.setText(n(c.getUf()));
        } catch (Exception ex) { ex.printStackTrace(); } finally { close(em); }
    }

    private void preencherFornecedor(Long id) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Fornecedor f = em.find(Fornecedor.class, id);
            if (f == null) return;
            idFornecedorSel = id;
            tfFRazao.setText(n(f.getRazaoSocial()));    tfFFantasia.setText(n(f.getNomeFantasia()));
            tfFCnpj.setText(n(f.getCnpj()));             tfFIE.setText(n(f.getInscricaoEstadual()));
            tfFTel.setText(n(f.getTelefone()));          tfFEmail.setText(n(f.getEmail()));
            tfFContato.setText(n(f.getContato()));       tfFCep.setText(n(f.getCep()));
            tfFLog.setText(n(f.getLogradouro()));        tfFNumero.setText(n(f.getNumero()));
            tfFBairro.setText(n(f.getBairro()));         tfFCidade.setText(n(f.getCidade()));
            tfFUf.setText(n(f.getUf()));
        } catch (Exception ex) { ex.printStackTrace(); } finally { close(em); }
    }

    // ── Apply form helpers ────────────────────────────────────────────────────

    private void applyFormToPF(Cliente c) {
        c.setNome(tfNome.getText().trim()); c.setCpf(tfCpf.getText().trim());
        c.setTelefone(tfTel.getText().trim()); c.setEmail(tfEmail.getText().trim());
    }

    private void applyFormToPJ(ClienteJuridico c) {
        c.setRazaoSocial(tfPJRazao.getText().trim());     c.setNomeFantasia(tfPJFantasia.getText().trim());
        c.setCnpj(tfPJCnpj.getText().trim());             c.setInscricaoEstadual(tfPJIE.getText().trim());
        c.setTelefone(tfPJTel.getText().trim());          c.setEmail(tfPJEmail.getText().trim());
        c.setContato(tfPJContato.getText().trim());       c.setCep(tfPJCep.getText().trim());
        c.setLogradouro(tfPJLog.getText().trim());        c.setNumero(tfPJNumero.getText().trim());
        c.setBairro(tfPJBairro.getText().trim());         c.setCidade(tfPJCidade.getText().trim());
        c.setUf(tfPJUf.getText().trim().toUpperCase());
    }

    private void applyFormToFornecedor(Fornecedor f) {
        f.setRazaoSocial(tfFRazao.getText().trim());      f.setNomeFantasia(tfFFantasia.getText().trim());
        f.setCnpj(tfFCnpj.getText().trim());              f.setInscricaoEstadual(tfFIE.getText().trim());
        f.setTelefone(tfFTel.getText().trim());           f.setEmail(tfFEmail.getText().trim());
        f.setContato(tfFContato.getText().trim());        f.setCep(tfFCep.getText().trim());
        f.setLogradouro(tfFLog.getText().trim());         f.setNumero(tfFNumero.getText().trim());
        f.setBairro(tfFBairro.getText().trim());          f.setCidade(tfFCidade.getText().trim());
        f.setUf(tfFUf.getText().trim().toUpperCase());
    }

    private Endereco buildEndereco() {
        Endereco end = new Endereco();
        applyFormToEndereco(end);
        return end;
    }

    private void applyFormToEndereco(Endereco end) {
        try { end.setNumero(Integer.parseInt(tfNumero.getText().trim())); } catch (Exception e) { end.setNumero(0); }
        end.setCep(tfCep.getText().trim()); end.setLogradouro(tfLog.getText().trim());
        end.setComplemento(tfComp.getText().trim()); end.setBairro(tfBairro.getText().trim());
        end.setLocalidade(tfCidade.getText().trim()); end.setUf(tfUf.getText().trim());
    }

    private void limparAtivo() {
        int tab = formTabs != null ? formTabs.getSelectedIndex() : 0;
        if (tab == 0) limparPF();
        else if (tab == 1) limparPJ();
        else limparFornecedor();
    }

    // ── Busca CEP e CNPJ ─────────────────────────────────────────────────────

    private void buscarCep(JTextField tfC, JTextField tfL, JTextField tfB,
                           JTextField tfCid, JTextField tfU) {
        String cep = tfC.getText().trim().replaceAll("[^0-9]", "");
        if (cep.isBlank()) { JOptionPane.showMessageDialog(this, "Informe o CEP."); return; }
        try {
            Endereco via = viaCepClient.buscarCep(cep);
            if (via != null) {
                tfL.setText(via.getLogradouro()); tfB.setText(via.getBairro());
                tfCid.setText(via.getLocalidade()); tfU.setText(via.getUf()); tfC.setText(via.getCep());
            } else JOptionPane.showMessageDialog(this, "CEP não encontrado.");
        } catch (Exception ex) { showError("Erro ao buscar CEP: " + ex.getMessage()); }
    }

    private void buscarCnpj(JTextField tfCnpjField,
                            JTextField tfRazaoField, JTextField tfFantasiaField,
                            JTextField tfTelField,   JTextField tfEmailField,
                            JTextField tfCepField,   JTextField tfLogField,
                            JTextField tfNumeroField, JTextField tfBairroField,
                            JTextField tfCidField,   JTextField tfUfField) {
        String cnpj = tfCnpjField.getText().trim();
        if (Validator.isBlank(cnpj)) {
            JOptionPane.showMessageDialog(this, "Informe o CNPJ antes de buscar.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JDialog aguarde = new JDialog(SwingUtilities.getWindowAncestor(this), "Consultando...", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        aguarde.setSize(260, 70);
        aguarde.setLocationRelativeTo(this);
        aguarde.setLayout(new BorderLayout());
        JLabel lblAg = new JLabel("  Consultando CNPJ na BrasilAPI...", SwingConstants.LEFT);
        lblAg.setFont(UIFactory.FONT_NORMAL);
        aguarde.add(lblAg, BorderLayout.CENTER);

        new Thread(() -> {
            try {
                CnpjClient.DadosEmpresa d = CnpjClient.consultar(cnpj);
                SwingUtilities.invokeLater(() -> {
                    aguarde.dispose();
                    if (d == null) { JOptionPane.showMessageDialog(this, "CNPJ não encontrado.", "Não encontrado", JOptionPane.WARNING_MESSAGE); return; }
                    if (d.razaoSocial  != null && !d.razaoSocial.isEmpty())  tfRazaoField.setText(d.razaoSocial);
                    if (d.nomeFantasia != null && !d.nomeFantasia.isEmpty()) tfFantasiaField.setText(d.nomeFantasia);
                    if (d.telefone     != null && !d.telefone.isEmpty())     tfTelField.setText(d.telefone);
                    if (d.email        != null && !d.email.isEmpty())        tfEmailField.setText(d.email);
                    if (d.cep          != null && !d.cep.isEmpty())          tfCepField.setText(d.cep);
                    if (d.logradouro   != null && !d.logradouro.isEmpty())   tfLogField.setText(d.logradouro);
                    if (d.numero       != null && !d.numero.isEmpty())       tfNumeroField.setText(d.numero);
                    if (d.bairro       != null && !d.bairro.isEmpty())       tfBairroField.setText(d.bairro);
                    if (d.cidade       != null && !d.cidade.isEmpty())       tfCidField.setText(d.cidade);
                    if (d.uf           != null && !d.uf.isEmpty())           tfUfField.setText(d.uf);

                    String sit = d.situacao != null ? d.situacao : "—";
                    boolean ativa = sit.toUpperCase().contains("ATIVA");
                    JOptionPane.showMessageDialog(this,
                            (ativa ? "✅" : "⚠") + " Dados preenchidos!\nSituação: " + sit,
                            "CNPJ Consultado", ativa ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> { aguarde.dispose(); showError("Erro ao consultar CNPJ:\n" + ex.getMessage()); });
            }
        }).start();
        aguarde.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        form.add(UIFactory.labelLight(label), c);
        c.gridx = 1; c.weightx = 1.0;
        form.add(field, c);
    }

    private String n(String s) { return s != null ? s : ""; }

    private void rollback(EntityManager em) { if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback(); }
    private void close(EntityManager em)    { if (em != null) em.close(); }
    private void showError(String msg)      { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
}