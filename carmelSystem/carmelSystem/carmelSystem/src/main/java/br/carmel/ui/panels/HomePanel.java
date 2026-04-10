package br.carmel.ui.panels;

import br.carmel.model.StatusPedido;
import br.carmel.util.UIFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Painel inicial (dashboard) - centro mostra rankings do mês.
 */
public class HomePanel extends JPanel {

    private final EntityManagerFactory emf;
    private DefaultTableModel modelProdutos;
    private DefaultTableModel modelClientes;
    private JLabel lblMes;
    private java.awt.image.BufferedImage bgImage;

    public HomePanel(EntityManagerFactory emf,
                     ActionListener onClientes,
                     ActionListener onProdutos,
                     ActionListener onPedidos,
                     ActionListener onCaixa) {
        this.emf = emf;
        // Carrega imagem de fundo uma vez
        try {
            java.net.URL url = getClass().getResource("/fundo.jpg");
            if (url != null) bgImage = javax.imageio.ImageIO.read(url);
        } catch (Exception ignored) {}
        setLayout(new BorderLayout());
        setOpaque(false);
        build(onClientes, onProdutos, onPedidos, onCaixa);
        carregarDados();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (bgImage != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            // Overlay semitransparente para legibilidade
            g2.setColor(new Color(236, 233, 216, 160));
            g2.fillRect(0, 0, getWidth(), getHeight());
        } else {
            setOpaque(true);
            super.paintComponent(g);
        }
    }

    // ── Montagem ──────────────────────────────────────────────────────────────

    private void build(ActionListener onClientes,
                       ActionListener onProdutos,
                       ActionListener onPedidos,
                       ActionListener onCaixa) {

        add(UIFactory.xpTitleBar("Painel Principal - Carmel Sistema"), BorderLayout.NORTH);
        add(buildCentro(), BorderLayout.CENTER);

        JLabel status = new JLabel("  Bem-vindo ao sistema Carmel.");
        status.setFont(UIFactory.FONT_NORMAL);
        status.setBackground(new Color(212, 208, 200));
        status.setOpaque(true);
        status.setBorder(new EmptyBorder(3, 4, 3, 4));
        add(status, BorderLayout.SOUTH);
    }

    // ── Centro: rankings do mês ───────────────────────────────────────────────

    private JPanel buildCentro() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Cabeçalho com mês e botão atualizar
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        topo.setBorder(new EmptyBorder(0, 0, 8, 0));

        lblMes = new JLabel();
        lblMes.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblMes.setForeground(new Color(0, 60, 140));
        atualizarLabelMes();

        JButton btnAtualizar = UIFactory.bigActionButton("↻ Atualizar", e -> carregarDados());
        topo.add(lblMes, BorderLayout.WEST);
        topo.add(btnAtualizar, BorderLayout.EAST);
        p.add(topo, BorderLayout.NORTH);

        // Dois rankings lado a lado
        JPanel rankings = new JPanel(new GridLayout(1, 2, 16, 0));
        rankings.setOpaque(false);
        rankings.add(buildRankingProdutos());
        rankings.add(buildRankingClientes());
        p.add(rankings, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildRankingProdutos() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(UIFactory.groupBorder("🏆 Produtos Mais Vendidos no Mês"));

        modelProdutos = new DefaultTableModel(
                new String[]{"#", "Produto", "Qtd Vendida", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildRankingTable(modelProdutos);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.setOpaque(false);
        table.setBackground(new Color(255, 255, 255, 180));
        JScrollPane scroll1 = new JScrollPane(table);
        scroll1.setOpaque(false);
        scroll1.getViewport().setOpaque(false);
        scroll1.getViewport().setBackground(new Color(255, 255, 255, 150));
        p.add(scroll1, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRankingClientes() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.setBorder(UIFactory.groupBorder("👤 Clientes que Mais Compraram no Mês"));

        modelClientes = new DefaultTableModel(
                new String[]{"#", "Cliente", "Pedidos", "Total (R$)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildRankingTable(modelClientes);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        table.setOpaque(false);
        table.setBackground(new Color(255, 255, 255, 180));
        JScrollPane scroll2 = new JScrollPane(table);
        scroll2.setOpaque(false);
        scroll2.getViewport().setOpaque(false);
        scroll2.getViewport().setBackground(new Color(255, 255, 255, 150));
        p.add(scroll2, BorderLayout.CENTER);
        return p;
    }

    private JTable buildRankingTable(DefaultTableModel model) {
        JTable table = UIFactory.styledTable();
        table.setModel(model);
        table.setRowHeight(22);

        // Renderizador com medalhas nas 3 primeiras posições
        table.setDefaultRenderer(Object.class, (t, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value != null ? value.toString() : "");
            cell.setFont(col == 0 ? UIFactory.FONT_BOLD : UIFactory.FONT_NORMAL);
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(1, 6, 1, 6));

            if (isSelected) {
                cell.setBackground(UIFactory.XP_TABLE_SEL);
                cell.setForeground(Color.WHITE);
            } else {
                cell.setForeground(Color.BLACK);
                cell.setBackground(switch (row) {
                    case 0 -> new Color(255, 248, 200, 200); // ouro
                    case 1 -> new Color(240, 240, 245, 200); // prata
                    case 2 -> new Color(245, 235, 220, 200); // bronze
                    default -> row % 2 == 0 ? new Color(255, 255, 255, 180) : new Color(248, 248, 252, 180);
                });
            }

            // Coluna # mostra medalha
            if (col == 0 && !isSelected) {
                cell.setHorizontalAlignment(SwingConstants.CENTER);
                cell.setText(switch (row) {
                    case 0 -> "🥇";
                    case 1 -> "🥈";
                    case 2 -> "🥉";
                    default -> String.valueOf(row + 1);
                });
            }
            // Total alinhado à direita
            if (col == 3 || col == 2) {
                cell.setHorizontalAlignment(SwingConstants.RIGHT);
                cell.setFont(UIFactory.FONT_BOLD);
            }
            return cell;
        });

        return table;
    }

    // ── Carga de dados ────────────────────────────────────────────────────────

    public void carregarDados() {
        atualizarLabelMes();
        carregarProdutosMaisVendidos();
        carregarClientesQueMaisCompraram();
    }

    private void carregarProdutosMaisVendidos() {
        modelProdutos.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            LocalDate inicio = LocalDate.now().withDayOfMonth(1);
            LocalDate fim    = inicio.plusMonths(1);

            // Apenas pedidos CONFIRMADOS (notas atendidas) do mês
            List<Object[]> rows = em.createQuery(
                            "SELECT i.produto.nome, SUM(i.quantidade), SUM(i.subtotal) " +
                                    "FROM ItensPedido i " +
                                    "WHERE i.pedido.status = :status " +
                                    "AND i.pedido.dataPedido >= :ini AND i.pedido.dataPedido < :fim " +
                                    "GROUP BY i.produto.nome " +
                                    "ORDER BY SUM(i.quantidade) DESC",
                            Object[].class)
                    .setParameter("status", StatusPedido.CONFIRMADO)
                    .setParameter("ini", inicio.atStartOfDay())
                    .setParameter("fim", fim.atStartOfDay())
                    .setMaxResults(10)
                    .getResultList();

            int pos = 1;
            for (Object[] r : rows) {
                modelProdutos.addRow(new Object[]{
                        pos++,
                        r[0],
                        r[1],
                        String.format("R$ %.2f", r[2])
                });
            }
            if (rows.isEmpty())
                modelProdutos.addRow(new Object[]{"—", "Nenhuma venda este mês", "—", "—"});

        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (em != null) em.close(); }
    }

    private void carregarClientesQueMaisCompraram() {
        modelClientes.setRowCount(0);
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            LocalDate inicio = LocalDate.now().withDayOfMonth(1);
            LocalDate fim    = inicio.plusMonths(1);

            // Apenas pedidos CONFIRMADOS (notas atendidas), usando valorFinal (com desconto)
            List<Object[]> rows = em.createQuery(
                            "SELECT p.cliente.nome, COUNT(p), SUM(COALESCE(pag.valorFinal, p.valorTotal)) " +
                                    "FROM Pedido p LEFT JOIN Pagamento pag ON pag.pedido = p " +
                                    "WHERE p.status = :status " +
                                    "AND p.dataPedido >= :ini AND p.dataPedido < :fim " +
                                    "GROUP BY p.cliente.nome " +
                                    "ORDER BY SUM(COALESCE(pag.valorFinal, p.valorTotal)) DESC",
                            Object[].class)
                    .setParameter("status", StatusPedido.CONFIRMADO)
                    .setParameter("ini", inicio.atStartOfDay())
                    .setParameter("fim", fim.atStartOfDay())
                    .setMaxResults(10)
                    .getResultList();

            int pos = 1;
            for (Object[] r : rows) {
                modelClientes.addRow(new Object[]{
                        pos++,
                        r[0],
                        r[1] + " pedido(s)",
                        String.format("R$ %.2f", r[2])
                });
            }
            if (rows.isEmpty())
                modelClientes.addRow(new Object[]{"—", "Nenhum pedido este mês", "—", "—"});

        } catch (Exception ex) { ex.printStackTrace(); }
        finally { if (em != null) em.close(); }
    }

    private void atualizarLabelMes() {
        String mes = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy",
                new java.util.Locale("pt", "BR")));
        if (lblMes != null)
            lblMes.setText("Resumo de " + mes.substring(0, 1).toUpperCase() + mes.substring(1));
    }
}