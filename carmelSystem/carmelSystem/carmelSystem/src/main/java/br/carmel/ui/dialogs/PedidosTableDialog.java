package br.carmel.ui.dialogs;

import br.carmel.model.ItensPedido;
import br.carmel.model.Pedido;
import br.carmel.util.UIFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Diálogo que lista todos os Pedidos e exibe os itens do pedido
 * selecionado em um painel de detalhes.
 */
public class PedidosTableDialog extends JDialog {

    private final EntityManagerFactory emf;

    // Tabela de detalhes (itens do pedido selecionado)
    private final DefaultTableModel detailModel = new DefaultTableModel(
            new String[]{"Produto", "Código de Barras", "Preço Unit.", "Quantidade", "Subtotal"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JLabel detailTitle = new JLabel(
            "Itens do Pedido - Selecione um pedido acima", SwingConstants.LEFT);

    public PedidosTableDialog(Component parent, EntityManagerFactory emf) {
        super(SwingUtilities.getWindowAncestor(parent), "Lista de Pedidos", ModalityType.APPLICATION_MODAL);
        this.emf = emf;
        setSize(1100, 600);
        setLocationRelativeTo(parent);
        build();
    }

    private void build() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(new Color(8, 8, 10));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));

        // ── Título ────────────────────────────────────────────────────────────
        JLabel title = new JLabel("Pedidos Realizados", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(200, 220, 255));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        main.add(title, BorderLayout.NORTH);

        // ── Tabela principal de pedidos ───────────────────────────────────────
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Cliente", "Data", "Status", "Valor Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        List<Pedido> pedidos = loadPedidos(model);

        JTable table = buildStyledTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
        main.add(scrollPane, BorderLayout.CENTER);

        // ── Painel de detalhes ────────────────────────────────────────────────
        main.add(buildDetailPanel(), BorderLayout.SOUTH);

        // ── Carregar primeiro pedido automaticamente ──────────────────────────
        if (pedidos != null && !pedidos.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            loadDetailForPedido(pedidos.get(0).getId());
        }

        // ── Listener de seleção ───────────────────────────────────────────────
        final List<Pedido> finalPedidos = pedidos;
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && finalPedidos != null) {
                    loadDetailForPedido((Long) model.getValueAt(row, 0));
                }
            }
        });

        // ── Botão fechar ──────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(UIFactory.bigActionButton("Fechar", e -> dispose()));
        main.add(btnPanel, BorderLayout.PAGE_END);

        add(main);
    }

    // ── Carga de dados ────────────────────────────────────────────────────────

    private List<Pedido> loadPedidos(DefaultTableModel model) {
        List<Pedido> pedidos = null;
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            pedidos = em.createQuery("SELECT p FROM Pedido p ORDER BY p.dataPedido DESC", Pedido.class)
                    .getResultList();
            for (Pedido p : pedidos) {
                model.addRow(new Object[]{
                        p.getId(),
                        p.getCliente().getNome(),
                        p.getDataPedido().toString().substring(0, 16).replace("T", " "),
                        p.getStatus(),
                        String.format("R$ %.2f", p.getValorTotal())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (em != null) em.close();
        }
        return pedidos;
    }

    private void loadDetailForPedido(Long pedidoId) {
        detailModel.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Pedido ped = em.find(Pedido.class, pedidoId);
            if (ped == null) return;

            List<ItensPedido> itens = em.createQuery(
                            "SELECT i FROM ItensPedido i WHERE i.pedido.id = :pid", ItensPedido.class)
                    .setParameter("pid", pedidoId)
                    .getResultList();

            if (itens != null && !itens.isEmpty()) {
                for (ItensPedido item : itens) {
                    detailModel.addRow(new Object[]{
                            item.getProduto().getNome(),
                            item.getProduto().getCodBarras() != null ? item.getProduto().getCodBarras() : "N/A",
                            String.format("R$ %.2f", item.getPrecoUnitario()),
                            item.getQuantidade(),
                            String.format("R$ %.2f", item.getSubtotal())
                    });
                }
                String obs = ped.getObservacoes();
                if (obs != null && !obs.isEmpty()) {
                    detailTitle.setText("📦 Itens do Pedido #" + pedidoId + " - Obs: " + obs);
                } else {
                    detailTitle.setText("📦 Itens do Pedido #" + pedidoId + " (" + itens.size() + " item(ns))");
                }
            } else {
                detailModel.addRow(new Object[]{"Nenhum item encontrado", "", "", "", ""});
                detailTitle.setText("📦 Pedido #" + pedidoId + " - sem itens");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar detalhes: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (em != null) em.close();
        }
    }

    // ── Construtores de componentes ───────────────────────────────────────────

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(14, 14, 16));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 70), 2),
                new EmptyBorder(12, 12, 12, 12)
        ));
        panel.setPreferredSize(new Dimension(1070, 220));

        detailTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        detailTitle.setForeground(new Color(200, 220, 255));
        detailTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        panel.add(detailTitle, BorderLayout.NORTH);

        JTable detailTable = buildStyledTable(detailModel);
        detailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        detailTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        detailTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        detailTable.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 70)));
        scroll.getViewport().setBackground(new Color(18, 18, 20));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(new Color(18, 18, 20));
        t.setForeground(Color.WHITE);
        t.setGridColor(new Color(60, 60, 70));
        t.setSelectionBackground(new Color(30, 100, 180));
        t.setSelectionForeground(Color.WHITE);
        t.setRowHeight(30);
        t.getTableHeader().setBackground(new Color(25, 25, 30));
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }
}