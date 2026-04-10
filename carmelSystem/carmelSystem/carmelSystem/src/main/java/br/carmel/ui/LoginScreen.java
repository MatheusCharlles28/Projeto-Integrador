package br.carmel.ui;

import br.carmel.model.Usuario;
import br.carmel.util.SenhaUtil;
import br.carmel.util.UIFactory;
import br.carmel.util.Validator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Tela de Login do sistema Carmel.
 * Bloqueia o acesso ao sistema até autenticação válida.
 * Permite cadastrar, alterar senha e excluir usuários (requer senha admin).
 */
public class LoginScreen extends JFrame {

    private final EntityManagerFactory emf;

    /** Callback disparado após login bem-sucedido. */
    public interface OnLoginSuccess {
        void onSuccess(Usuario usuario);
    }

    private final OnLoginSuccess onSuccess;

    public LoginScreen(EntityManagerFactory emf, OnLoginSuccess onSuccess) {
        super("Carmel Sistema - Login");
        this.emf = emf;
        this.onSuccess = onSuccess;

        // Ícone da janela
        try {
            java.net.URL iconUrl = getClass().getResource("/icone.png");
            if (iconUrl != null)
                setIconImage(new javax.swing.ImageIcon(iconUrl).getImage());
        } catch (Exception ignored) {}

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 460);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        build();
        setVisible(true);
    }

    // ── Montagem da UI ────────────────────────────────────────────────────────

    private void build() {
        // Painel raiz com imagem de fundo
        JPanel fundo = new JPanel(new BorderLayout()) {
            private java.awt.image.BufferedImage bgImage;
            {
                try {
                    java.net.URL url = getClass().getResource("/fundo_login.jpg");
                    if (url != null) bgImage = javax.imageio.ImageIO.read(url);
                } catch (Exception ignored) {}
            }
            @Override protected void paintComponent(Graphics g) {
                if (bgImage != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(58, 110, 165));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        // Cabeçalho sólido com gradiente
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 60, 140),
                        getWidth(), 0, new Color(58, 110, 165)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 70));
        JLabel logo = new JLabel("  CARMEL SISTEMA");
        logo.setFont(new Font("Tahoma", Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        JLabel sub = new JLabel("  Acesso ao Sistema");
        sub.setFont(new Font("Tahoma", Font.PLAIN, 11));
        sub.setForeground(new Color(180, 210, 240));
        JPanel headerText = new JPanel(new GridLayout(2, 1));
        headerText.setOpaque(false);
        headerText.add(logo); headerText.add(sub);
        header.add(headerText, BorderLayout.CENTER);
        fundo.add(header, BorderLayout.NORTH);

        // Card central sólido por cima da imagem
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(new Color(236, 233, 216, 230));
        card.setOpaque(true);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 84, 166), 2),
                new EmptyBorder(20, 24, 20, 24)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Login
        c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.weightx = 0;
        card.add(UIFactory.labelLight("Login:"), c);
        JTextField tfLogin = UIFactory.styledField("");
        tfLogin.setPreferredSize(new Dimension(200, 24));
        c.gridx = 1; c.weightx = 1.0; card.add(tfLogin, c);

        // Senha
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        card.add(UIFactory.labelLight("Senha:"), c);
        JPasswordField tfSenha = UIFactory.styledPasswordField("");
        tfSenha.setPreferredSize(new Dimension(200, 24));
        c.gridx = 1; c.weightx = 1.0; card.add(tfSenha, c);

        // Botão Entrar
        JButton btnEntrar = UIFactory.bigActionButton("Entrar", e ->
                realizarLogin(tfLogin.getText().trim(), new String(tfSenha.getPassword())));
        btnEntrar.setBackground(new Color(0, 120, 0));
        btnEntrar.setForeground(Color.WHITE);
        btnEntrar.setPreferredSize(new Dimension(200, 36));
        btnEntrar.setFont(new Font("Tahoma", Font.BOLD, 13));
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.insets = new Insets(14, 4, 4, 4);
        card.add(btnEntrar, c);

        // Separador
        c.gridy = 3; c.insets = new Insets(10, 0, 10, 0);
        card.add(new JSeparator(), c);

        // Botões gestão
        JPanel gestao = new JPanel(new GridLayout(1, 3, 6, 0));
        gestao.setOpaque(false);
        JButton btnCadastrar    = UIFactory.bigActionButton("Novo Usuário",    ev -> dlgCadastrarUsuario());
        JButton btnAlterarSenha = UIFactory.bigActionButton("Alterar Senha",   ev -> dlgAlterarSenha());
        JButton btnExcluir      = UIFactory.bigActionButton("Excluir Usuário", ev -> dlgExcluirUsuario());
        gestao.add(btnCadastrar); gestao.add(btnAlterarSenha); gestao.add(btnExcluir);
        c.gridy = 4; c.insets = new Insets(0, 4, 4, 4);
        card.add(gestao, c);

        // Centraliza o card no fundo
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, new GridBagConstraints());
        fundo.add(wrapper, BorderLayout.CENTER);

        // Rodapé
        JLabel rodape = new JLabel("v1.0 • Carmel Sistema de Gestão", SwingConstants.CENTER);
        rodape.setFont(new Font("Tahoma", Font.PLAIN, 9));
        rodape.setForeground(Color.WHITE);
        rodape.setOpaque(false);
        rodape.setBorder(new EmptyBorder(4, 0, 4, 0));
        fundo.add(rodape, BorderLayout.SOUTH);

        add(fundo);

        // Enter dispara login
        KeyAdapter enterLogin = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    realizarLogin(tfLogin.getText().trim(), new String(tfSenha.getPassword()));
            }
        };
        tfLogin.addKeyListener(enterLogin);
        tfSenha.addKeyListener(enterLogin);
    }

    // ── Ações de login ────────────────────────────────────────────────────────

    private void realizarLogin(String login, String senha) {
        if (Validator.isBlank(login)) { showAviso("Informe o login."); return; }
        if (Validator.isBlank(senha)) { showAviso("Informe a senha."); return; }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Usuario> lista = em.createQuery(
                            "SELECT u FROM Usuario u WHERE u.login = :login", Usuario.class)
                    .setParameter("login", login.toLowerCase())
                    .getResultList();

            if (lista.isEmpty()) {
                showErro("Usuário não encontrado.");
                return;
            }
            Usuario u = lista.get(0);
            if (!SenhaUtil.verificar(senha, u.getSenha())) {
                showErro("Senha incorreta.");
                return;
            }
            // Login OK
            dispose();
            onSuccess.onSuccess(u);

        } catch (Exception ex) {
            ex.printStackTrace();
            showErro("Erro ao autenticar: " + ex.getMessage());
        } finally { if (em != null) em.close(); }
    }

    // ── Diálogo: Cadastrar Usuário ────────────────────────────────────────────

    private void dlgCadastrarUsuario() {
        JDialog dlg = new JDialog(this, "Novo Usuário", true);
        dlg.setSize(360, 290);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints c = gbc();

        JPasswordField tfAdmin  = UIFactory.styledPasswordField("");
        JTextField tfNome       = UIFactory.styledField("");
        JTextField tfLoginField = UIFactory.styledField("");
        JPasswordField tfSenha  = UIFactory.styledPasswordField("");
        JPasswordField tfConf   = UIFactory.styledPasswordField("");

        row(form, c, 0, "Senha do administrador:", tfAdmin);
        row(form, c, 1, "Nome completo:",           tfNome);
        row(form, c, 2, "Login:",                   tfLoginField);
        row(form, c, 3, "Senha:",                   tfSenha);
        row(form, c, 4, "Confirmar senha:",          tfConf);

        dlg.add(form, BorderLayout.CENTER);

        // Botões
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] confirmado = {false};
        JButton btnOk     = UIFactory.bigActionButton("Cadastrar", e -> { confirmado[0] = true; dlg.dispose(); });
        JButton btnCancel = UIFactory.bigActionButton("Cancelar",  e -> dlg.dispose());
        btns.add(btnOk);
        btns.add(btnCancel);
        dlg.add(btns, BorderLayout.SOUTH);

        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfAdmin::requestFocusInWindow);
        dlg.setVisible(true);

        if (!confirmado[0]) return;

        // Validações após fechar
        String admin   = new String(tfAdmin.getPassword());
        String nome    = tfNome.getText().trim();
        String login   = tfLoginField.getText().trim().toLowerCase();
        String senha   = new String(tfSenha.getPassword());
        String confirm = new String(tfConf.getPassword());

        if (!SenhaUtil.isAdmin(admin))       { showErro("Senha de administrador incorreta."); return; }
        if (Validator.isBlank(nome))         { showAviso("Nome completo é obrigatório."); return; }
        if (Validator.isBlank(login))        { showAviso("Login é obrigatório."); return; }
        if (login.length() > 60)             { showAviso("Login deve ter no máximo 60 caracteres."); return; }
        if (Validator.isBlank(senha))        { showAviso("Senha é obrigatória."); return; }
        if (senha.length() < 4)              { showAviso("Senha deve ter pelo menos 4 caracteres."); return; }
        if (!senha.equals(confirm))          { showErro("As senhas não conferem."); return; }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            long count = em.createQuery("SELECT COUNT(u) FROM Usuario u WHERE u.login = :l", Long.class)
                    .setParameter("l", login).getSingleResult();
            if (count > 0) { showErro("Já existe um usuário com esse login."); return; }

            em.getTransaction().begin();
            Usuario u = new Usuario();
            u.setLogin(login);
            u.setNomeCompleto(nome);
            u.setSenha(SenhaUtil.hash(senha));
            em.persist(u);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Usuário \"" + login + "\" cadastrado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback();
            showErro("Erro ao cadastrar: " + ex.getMessage());
            ex.printStackTrace();
        } finally { if (em != null) em.close(); }
    }

    // ── Diálogo: Alterar Senha ────────────────────────────────────────────────

    private void dlgAlterarSenha() {
        JDialog dlg = new JDialog(this, "Alterar Senha", true);
        dlg.setSize(340, 230);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints c = gbc();

        JTextField tfLoginField  = UIFactory.styledField("");
        JPasswordField tfAtual   = UIFactory.styledPasswordField("");
        JPasswordField tfNova    = UIFactory.styledPasswordField("");
        JPasswordField tfConf    = UIFactory.styledPasswordField("");

        row(form, c, 0, "Login:",          tfLoginField);
        row(form, c, 1, "Senha atual:",    tfAtual);
        row(form, c, 2, "Nova senha:",     tfNova);
        row(form, c, 3, "Confirmar nova:", tfConf);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] confirmado = {false};
        JButton btnOk     = UIFactory.bigActionButton("Alterar",  e -> { confirmado[0] = true; dlg.dispose(); });
        JButton btnCancel = UIFactory.bigActionButton("Cancelar", e -> dlg.dispose());
        btns.add(btnOk);
        btns.add(btnCancel);
        dlg.add(btns, BorderLayout.SOUTH);

        dlg.getRootPane().setDefaultButton(btnOk);
        SwingUtilities.invokeLater(tfLoginField::requestFocusInWindow);
        dlg.setVisible(true);

        if (!confirmado[0]) return;

        String loginVal  = tfLoginField.getText().trim().toLowerCase();
        String atualVal  = new String(tfAtual.getPassword());
        String novaVal   = new String(tfNova.getPassword());
        String confVal   = new String(tfConf.getPassword());

        if (Validator.isBlank(loginVal))  { showAviso("Informe o login."); return; }
        if (Validator.isBlank(novaVal))   { showAviso("Nova senha é obrigatória."); return; }
        if (novaVal.length() < 4)         { showAviso("Senha deve ter pelo menos 4 caracteres."); return; }
        if (!novaVal.equals(confVal))     { showErro("As senhas não conferem."); return; }

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Usuario> lista = em.createQuery("SELECT u FROM Usuario u WHERE u.login = :l", Usuario.class)
                    .setParameter("l", loginVal).getResultList();

            if (lista.isEmpty()) { showErro("Usuário não encontrado."); return; }
            Usuario u = lista.get(0);
            if (!SenhaUtil.verificar(atualVal, u.getSenha())) { showErro("Senha atual incorreta."); return; }

            em.getTransaction().begin();
            u.setSenha(SenhaUtil.hash(novaVal));
            em.merge(u);
            em.getTransaction().commit();
            JOptionPane.showMessageDialog(this, "Senha alterada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback();
            showErro("Erro ao alterar senha: " + ex.getMessage());
            ex.printStackTrace();
        } finally { if (em != null) em.close(); }
    }

    // ── Diálogo: Excluir Usuário ──────────────────────────────────────────────

    private void dlgExcluirUsuario() {
        EntityManager em = null;
        List<Usuario> usuarios;
        try {
            em = emf.createEntityManager();
            usuarios = em.createQuery("SELECT u FROM Usuario u ORDER BY u.login", Usuario.class).getResultList();
        } catch (Exception ex) { showErro("Erro ao carregar usuários."); return; }
        finally { if (em != null) em.close(); }

        if (usuarios.isEmpty()) { showAviso("Nenhum usuário cadastrado."); return; }

        JDialog dlg = new JDialog(this, "Excluir Usuário", true);
        dlg.setSize(400, 310);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);
        dlg.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIFactory.XP_BG);
        form.setBorder(new EmptyBorder(12, 14, 8, 14));
        GridBagConstraints c = gbc();

        // Tabela
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Login", "Nome"}, 0) {
            public boolean isCellEditable(int r, int col) { return false; }
        };
        for (Usuario u : usuarios) model.addRow(new Object[]{ u.getId(), u.getLogin(), u.getNomeCompleto() });
        JTable table = UIFactory.styledTable();
        table.setModel(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(350, 110));

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.insets = new Insets(0, 0, 4, 0);
        form.add(UIFactory.labelLight("Selecione o usuário a excluir:"), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 10, 0);
        form.add(scroll, c);

        JPasswordField tfAdmin = UIFactory.styledPasswordField("");
        c.insets = new Insets(4, 4, 4, 4);
        row(form, c, 2, "Senha do administrador:", tfAdmin);

        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UIFactory.XP_BG);
        boolean[] confirmado = {false};
        JButton btnOk     = UIFactory.bigActionButton("Excluir",  e -> { confirmado[0] = true; dlg.dispose(); });
        JButton btnCancel = UIFactory.bigActionButton("Cancelar", e -> dlg.dispose());
        btnOk.setBackground(new Color(180, 0, 0));
        btnOk.setForeground(Color.WHITE);
        btns.add(btnOk);
        btns.add(btnCancel);
        dlg.add(btns, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(tfAdmin::requestFocusInWindow);
        dlg.setVisible(true);

        if (!confirmado[0]) return;

        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) { showAviso("Selecione um usuário na tabela."); return; }

        String adminVal = new String(tfAdmin.getPassword());
        if (!SenhaUtil.isAdmin(adminVal)) { showErro("Senha de administrador incorreta."); return; }

        Long idExcluir    = (Long)   model.getValueAt(selectedRow, 0);
        String loginExcluir = (String) model.getValueAt(selectedRow, 1);

        int conf = JOptionPane.showConfirmDialog(this,
                "Confirma a exclusão do usuário \"" + loginExcluir + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        EntityManager em2 = null;
        try {
            em2 = emf.createEntityManager();
            em2.getTransaction().begin();
            Usuario u = em2.find(Usuario.class, idExcluir);
            if (u != null) { em2.remove(u); em2.getTransaction().commit(); }
            JOptionPane.showMessageDialog(this, "Usuário \"" + loginExcluir + "\" excluído.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            if (em2 != null && em2.getTransaction().isActive()) em2.getTransaction().rollback();
            showErro("Erro ao excluir: " + ex.getMessage());
            ex.printStackTrace();
        } finally { if (em2 != null) em2.close(); }
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    private void row(JPanel p, GridBagConstraints c, int rowIdx, String label, JComponent field) {
        c.gridx = 0; c.gridy = rowIdx; c.gridwidth = 1; c.weightx = 0;
        p.add(UIFactory.labelLight(label), c);
        c.gridx = 1; c.weightx = 1.0;
        field.setPreferredSize(new Dimension(180, 24));
        p.add(field, c);
    }

    private void showErro(String msg)  { JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE); }
    private void showAviso(String msg) { JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE); }
}