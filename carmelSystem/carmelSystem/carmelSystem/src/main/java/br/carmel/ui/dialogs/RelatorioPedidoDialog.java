package br.carmel.ui.dialogs;

import br.carmel.model.ItensPedido;
import br.carmel.model.Pagamento;
import br.carmel.model.Pedido;
import br.carmel.util.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo com relatório do pedido finalizado.
 * Exibe os dados do pedido e permite impressão.
 */
public class RelatorioPedidoDialog extends JDialog implements Printable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font FONT_NORMAL  = new Font("Tahoma", Font.PLAIN,  11);
    private static final Font FONT_BOLD    = new Font("Tahoma", Font.BOLD,   11);
    private static final Font FONT_TITLE   = new Font("Tahoma", Font.BOLD,   14);
    private static final Font FONT_SMALL   = new Font("Tahoma", Font.PLAIN,   9);

    private final Pedido    pedido;
    private final Pagamento pagamento;

    // Painel que será impresso
    private JPanel printPanel;

    public RelatorioPedidoDialog(Window owner, Pedido pedido, Pagamento pagamento) {
        super(owner, "Relatório do Pedido #" + pedido.getId(), ModalityType.APPLICATION_MODAL);
        this.pedido    = pedido;
        this.pagamento = pagamento;

        setSize(480, 620);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        // Área de visualização com scroll
        printPanel = buildReportPanel();
        JScrollPane scroll = new JScrollPane(printPanel);
        scroll.setBorder(null);
        scroll.setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        setVisible(true);
    }

    // ── Monta o conteúdo do relatório ─────────────────────────────────────────

    private JPanel buildReportPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 28, 20, 28));

        // ── Cabeçalho ──────────────────────────────────────────────────────────
        p.add(centeredLabel("CARMEL SISTEMA DE GESTÃO", FONT_TITLE, new Color(0, 60, 140)));
        p.add(centeredLabel("COMPROVANTE DE PEDIDO", new Font("Tahoma", Font.BOLD, 12), Color.BLACK));
        p.add(vgap(6));
        p.add(separator());
        p.add(vgap(8));

        // ── Info do pedido ─────────────────────────────────────────────────────
        p.add(infoRow("Pedido Nº:", "#" + pedido.getId()));
        p.add(infoRow("Data/Hora:", pedido.getDataPedido().format(FMT)));
        p.add(infoRow("Status:", pedido.getStatus().toString()));
        p.add(vgap(4));
        p.add(separator());
        p.add(vgap(8));

        // ── Cliente ────────────────────────────────────────────────────────────
        p.add(sectionLabel("CLIENTE"));
        p.add(vgap(4));
        p.add(infoRow("Nome:", pedido.getCliente().getNome()));
        p.add(infoRow("CPF:", pedido.getCliente().getCpf()));
        if (pedido.getCliente().getTelefone() != null)
            p.add(infoRow("Telefone:", pedido.getCliente().getTelefone()));
        p.add(vgap(4));
        p.add(separator());
        p.add(vgap(8));

        // ── Itens ──────────────────────────────────────────────────────────────
        p.add(sectionLabel("ITENS DO PEDIDO"));
        p.add(vgap(6));

        // Cabeçalho da tabela de itens
        p.add(itemHeader());

        for (ItensPedido item : pedido.getItensPedidos()) {
            p.add(itemRow(
                    item.getProduto().getNome(),
                    item.getQuantidade(),
                    item.getPrecoUnitario() != null
                            ? String.format("R$ %.2f", item.getPrecoUnitario())
                            : "-",
                    item.getSubtotal() != null
                            ? String.format("R$ %.2f", item.getSubtotal())
                            : "-"
            ));
        }

        p.add(vgap(4));
        p.add(separator());
        p.add(vgap(4));

        // Observações do pedido
        if (pedido.getObservacoes() != null && !pedido.getObservacoes().isBlank()) {
            p.add(infoRow("Obs.:", pedido.getObservacoes()));
            p.add(vgap(4));
            p.add(separator());
            p.add(vgap(4));
        }

        // ── Totais ─────────────────────────────────────────────────────────────
        p.add(sectionLabel("PAGAMENTO"));
        p.add(vgap(6));

        if (pedido.getValorTotal() != null)
            p.add(totalRow("Total do Pedido:", String.format("R$ %.2f", pedido.getValorTotal()), false));

        if (pagamento != null) {
            p.add(infoRow("Forma de Pagamento:", pagamento.getFormaPagamento().toString()));
            p.add(totalRow("Valor Recebido:",    String.format("R$ %.2f", pagamento.getValorPago()), false));
            if (pagamento.getTroco() != null && pagamento.getTroco().doubleValue() > 0)
                p.add(totalRow("Troco:", String.format("R$ %.2f", pagamento.getTroco()), true));
        }

        p.add(vgap(8));
        p.add(separator());
        p.add(vgap(10));

        // ── Rodapé ─────────────────────────────────────────────────────────────
        p.add(centeredLabel("Obrigado pela preferência!", FONT_BOLD, new Color(0, 60, 140)));
        p.add(vgap(4));
        p.add(centeredLabel("Carmel Sistema v1.0", FONT_SMALL, Color.GRAY));

        return p;
    }

    // ── Barra de botões ───────────────────────────────────────────────────────

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(UIFactory.XP_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(172, 168, 153)));

        JButton btnImprimir = UIFactory.bigActionButton("🖨 Imprimir", e -> imprimir());
        btnImprimir.setBackground(new Color(0, 80, 160));
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.setPreferredSize(new Dimension(120, 28));

        JButton btnFechar = UIFactory.bigActionButton("Fechar", e -> dispose());
        btnFechar.setPreferredSize(new Dimension(90, 28));

        bar.add(btnImprimir);
        bar.add(btnFechar);
        return bar;
    }

    // ── Impressão ─────────────────────────────────────────────────────────────

    private void imprimir() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Pedido #" + pedido.getId());
        job.setPrintable(this);

        // Abre o diálogo de impressão do sistema
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                        "Erro ao imprimir: " + ex.getMessage(),
                        "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        if (pageIndex > 0) return NO_SUCH_PAGE;

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Traduz para a área imprimível
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        double pageWidth  = pageFormat.getImageableWidth();
        double pageHeight = pageFormat.getImageableHeight();

        // Escala o painel para caber na página
        Dimension panelSize = printPanel.getPreferredSize();
        double scaleX = pageWidth  / panelSize.getWidth();
        double scaleY = pageHeight / panelSize.getHeight();
        double scale  = Math.min(scaleX, scaleY);
        if (scale < 1.0) g2.scale(scale, scale);

        // Força layout e pinta
        printPanel.setSize(panelSize);
        printPanel.doLayout();
        printPanel.printAll(g2);

        return PAGE_EXISTS;
    }

    // ── Componentes de layout do relatório ───────────────────────────────────

    private JLabel centeredLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
        return l;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Tahoma", Font.BOLD, 11));
        l.setForeground(new Color(0, 60, 140));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_BOLD);
        lbl.setPreferredSize(new Dimension(140, 18));

        JLabel val = new JLabel(value);
        val.setFont(FONT_NORMAL);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JPanel totalRow(String label, String value, boolean destaque) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl = new JLabel(label);
        lbl.setFont(destaque ? new Font("Tahoma", Font.BOLD, 12) : FONT_BOLD);
        lbl.setForeground(destaque ? new Color(0, 120, 0) : Color.BLACK);
        lbl.setPreferredSize(new Dimension(140, 20));

        JLabel val = new JLabel(value);
        val.setFont(destaque ? new Font("Tahoma", Font.BOLD, 12) : FONT_BOLD);
        val.setForeground(destaque ? new Color(0, 120, 0) : Color.BLACK);
        val.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel itemHeader() {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setBackground(new Color(10, 36, 106));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        for (String h : new String[]{"Produto", "Qtd", "Unit.", "Subtotal"}) {
            JLabel l = new JLabel("  " + h);
            l.setFont(FONT_BOLD);
            l.setForeground(Color.WHITE);
            row.add(l);
        }
        return row;
    }

    private JPanel itemRow(String nome, int qtd, String unit, String sub) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel lNome = new JLabel("  " + nome); lNome.setFont(FONT_NORMAL);
        JLabel lQtd  = new JLabel("  " + qtd);  lQtd.setFont(FONT_NORMAL);
        JLabel lUnit = new JLabel("  " + unit);  lUnit.setFont(FONT_NORMAL);
        JLabel lSub  = new JLabel("  " + sub);   lSub.setFont(FONT_BOLD);

        row.add(lNome); row.add(lQtd); row.add(lUnit); row.add(lSub);
        return row;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(180, 180, 180));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private Component vgap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }
}