package br.carmel;

import br.carmel.model.Usuario;
import br.carmel.ui.LoginScreen;
import br.carmel.ui.SideMenu;
import br.carmel.ui.panels.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import java.awt.*;

/**
 * Janela principal do sistema Carmel.
 * Só é exibida após login bem-sucedido na LoginScreen.
 */
public class MainFrame extends JFrame {

    private static final String PU_NAME = "dadoscarmelPU";

    private final EntityManagerFactory emf;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pnlCards = new JPanel(cardLayout);

    private PedidoPanel  pedidoPanel;
    private ClientePanel clientePanel;
    private ProdutoPanel produtoPanel;
    private CaixaPanel   caixaPanel;
    private br.carmel.ui.panels.RelatorioVendasPanel   relatorioVendasPanel;
    private br.carmel.ui.panels.RelatorioEstoquePanel  relatorioEstoquePanel;
    private br.carmel.ui.panels.NotaTransferenciaPanel notaTransferenciaPanel;
    private br.carmel.ui.panels.EmitirNotaPanel        emitirNotaPanel;

    // Usuário logado atualmente
    private Usuario usuarioAtual;

    // Label de usuário logado na barra de status
    private JLabel statusBar;

    private MainFrame(EntityManagerFactory emf, Usuario usuario) {
        super("Carmel Sistema de Gestão");
        this.emf = emf;
        this.usuarioAtual = usuario;

        // Ícone da janela e barra de tarefas
        try {
            java.net.URL iconUrl = getClass().getResource("/icone.png");
            if (iconUrl != null)
                setIconImage(new javax.swing.ImageIcon(iconUrl).getImage());
        } catch (Exception ignored) {}

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildMenu(), BorderLayout.WEST);
        add(buildCards(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        cardLayout.show(pnlCards, "home");
        setVisible(true);
        toFront();
        requestFocus();
        setState(JFrame.NORMAL);
    }

    // ── Barra de status ───────────────────────────────────────────────────────

    private JLabel buildStatusBar() {
        statusBar = new JLabel("  Usuário: " + usuarioAtual.getNomeCompleto()
                + " (" + usuarioAtual.getLogin() + ")  |  Carmel Sistema v1.0");
        statusBar.setFont(new Font("Tahoma", Font.PLAIN, 10));
        statusBar.setBackground(new Color(212, 208, 200));
        statusBar.setOpaque(true);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)),
                new EmptyBorder(2, 6, 2, 6)));
        return statusBar;
    }

    // ── Look and Feel ─────────────────────────────────────────────────────────

    private static void applyLookAndFeel() {
        try {
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            UIManager.put("Panel.background",      new Color(236, 233, 216));
            UIManager.put("OptionPane.background", new Color(236, 233, 216));
            UIManager.put("Button.background",     new Color(236, 233, 216));
            UIManager.put("TextField.background",  Color.WHITE);
            UIManager.put("TextArea.background",   Color.WHITE);
            UIManager.put("ComboBox.background",   Color.WHITE);
            UIManager.put("Table.background",      Color.WHITE);
            UIManager.put("ScrollPane.background", new Color(236, 233, 216));
            UIManager.put("SplitPane.background",  new Color(236, 233, 216));
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
        }
    }

    // ── Menu lateral ──────────────────────────────────────────────────────────

    private SideMenu buildMenu() {
        return new SideMenu(new SideMenu.NavActions() {
            public void goHome()       { showCard("home"); }
            public void goClientes()   { clientePanel.reloadTable(); showCard("cliente"); }
            public void goProdutos()   { produtoPanel.reloadTable(); showCard("produto"); }
            public void goPedidos()    { pedidoPanel.reloadCombos(); pedidoPanel.reloadPedidos(); showCard("pedido"); }
            public void goCaixa()      { caixaPanel.reloadCaixa(); showCard("caixa"); }
            public void goRelatorios() { showCard("relatorios"); }
            public void goEstoque()    { relatorioEstoquePanel.carregarDados(false); showCard("estoque"); }
            public void goNotas()      { notaTransferenciaPanel.carregarFornecedores(); showCard("notas"); }
            public void goEmitirNota() { emitirNotaPanel.reloadPendentes(); showCard("emitirNota"); }
            public void logout()       { fazerLogout(); }
        });
    }

    private void fazerLogout() {
        int conf = JOptionPane.showConfirmDialog(this,
                "Deseja sair do sistema?", "Logout", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;
        dispose();
        // Reabre a tela de login
        new LoginScreen(emf, usuario -> new MainFrame(emf, usuario));
    }

    // ── Cards ─────────────────────────────────────────────────────────────────

    private JPanel buildCards() {
        pnlCards.setBackground(new Color(236, 233, 216));

        clientePanel          = new ClientePanel(emf);
        produtoPanel          = new ProdutoPanel(emf);
        pedidoPanel           = new PedidoPanel(emf);
        caixaPanel            = new CaixaPanel(emf);
        relatorioVendasPanel  = new br.carmel.ui.panels.RelatorioVendasPanel(emf);
        relatorioEstoquePanel = new br.carmel.ui.panels.RelatorioEstoquePanel(emf);
        notaTransferenciaPanel = new br.carmel.ui.panels.NotaTransferenciaPanel(emf);
        emitirNotaPanel        = new br.carmel.ui.panels.EmitirNotaPanel(emf);
        pedidoPanel.setCaixaPanel(caixaPanel);
        emitirNotaPanel.setCaixaPanel(caixaPanel);

        pnlCards.add(new HomePanel(emf,
                e -> { clientePanel.reloadTable(); showCard("cliente"); },
                e -> { produtoPanel.reloadTable(); showCard("produto"); },
                e -> { pedidoPanel.reloadCombos(); pedidoPanel.reloadPedidos(); showCard("pedido"); },
                e -> { caixaPanel.reloadCaixa(); showCard("caixa"); }
        ), "home");
        pnlCards.add(clientePanel,          "cliente");
        pnlCards.add(produtoPanel,          "produto");
        pnlCards.add(pedidoPanel,           "pedido");
        pnlCards.add(caixaPanel,            "caixa");
        pnlCards.add(relatorioVendasPanel,   "relatorios");
        pnlCards.add(relatorioEstoquePanel,  "estoque");
        pnlCards.add(notaTransferenciaPanel, "notas");
        pnlCards.add(emitirNotaPanel,        "emitirNota");

        return pnlCards;
    }

    private void showCard(String name) {
        cardLayout.show(pnlCards, name);
    }

    private static void criarUsuarioPadraoSeNecessario(EntityManagerFactory emf) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Long total = em.createQuery("SELECT COUNT(u) FROM Usuario u", Long.class)
                    .getSingleResult();
            if (total == 0) {
                em.getTransaction().begin();
                br.carmel.model.Usuario u = new br.carmel.model.Usuario();
                u.setLogin("admin");
                // Senha: admin (hash SHA-256)
                u.setSenha("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
                u.setNomeCompleto("Administrador");
                em.persist(u);
                em.getTransaction().commit();
                System.out.println("[Sistema] Usuário padrão criado: admin / admin");
            }
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) em.getTransaction().rollback();
            System.err.println("[Sistema] Erro ao criar usuário padrão: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (em != null) em.close();
        }
    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();

            // Inicializa JPA
            EntityManagerFactory emf;
            try {
                emf = Persistence.createEntityManagerFactory(PU_NAME);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erro ao inicializar banco de dados:\n" + e.getMessage(),
                        "Erro Crítico", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                System.exit(1);
                return;
            }

            // Cria usuário padrão se o banco estiver vazio
            criarUsuarioPadraoSeNecessario(emf);

            // Abre a tela de login — o MainFrame só abre após login OK
            new LoginScreen(emf, usuario -> new MainFrame(emf, usuario));
        });
    }
}