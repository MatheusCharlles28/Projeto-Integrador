package br.carmel.ui;

import br.carmel.util.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Menu lateral estilo Windows XP (barra azul com botões laranja ao hover).
 */
public class SideMenu extends JPanel {

    public interface NavActions {
        void goHome();
        void goClientes();
        void goProdutos();
        void goPedidos();
        void goCaixa();
        void goRelatorios();
        void goEstoque();
        void goNotas();
        void goEmitirNota();
        void logout();
    }

    public SideMenu(NavActions nav) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));
        setBackground(UIFactory.XP_MENU_BG);
        build(nav);
    }

    private void build(NavActions nav) {
        // Cabeçalho azul escuro
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 60, 140),
                        getWidth(), 0, new Color(58, 110, 165)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(200, 64));

        JLabel logo = new JLabel("CARMEL", SwingConstants.CENTER);
        logo.setFont(new Font("Tahoma", Font.BOLD, 20));
        logo.setForeground(Color.WHITE);
        logo.setBorder(new EmptyBorder(14, 0, 14, 0));
        header.add(logo, BorderLayout.CENTER);

        JLabel sub = new JLabel("Sistema de Gestão", SwingConstants.CENTER);
        sub.setFont(new Font("Tahoma", Font.PLAIN, 9));
        sub.setForeground(new Color(180, 210, 240));
        header.add(sub, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        // Botões de navegação
        JPanel btns = new JPanel(new GridLayout(0, 1, 0, 4));
        btns.setOpaque(false);
        btns.setBorder(new EmptyBorder(14, 10, 14, 10));

        btns.add(UIFactory.bigMenuButton("Painel Inicial",   e -> nav.goHome()));
        btns.add(UIFactory.bigMenuButton("Clientes",         e -> nav.goClientes()));
        btns.add(UIFactory.bigMenuButton("Produtos",         e -> nav.goProdutos()));
        btns.add(UIFactory.bigMenuButton("Pedidos",          e -> nav.goPedidos()));
        btns.add(UIFactory.bigMenuButton("Caixa",            e -> nav.goCaixa()));
        btns.add(UIFactory.bigMenuButton("Rel. de Vendas",   e -> nav.goRelatorios()));
        btns.add(UIFactory.bigMenuButton("Rel. de Estoque",  e -> nav.goEstoque()));
        btns.add(UIFactory.bigMenuButton("Transferências",   e -> nav.goNotas()));
        btns.add(UIFactory.bigMenuButton("Emitir Nota",      e -> nav.goEmitirNota()));
        btns.add(Box.createVerticalStrut(10));
        btns.add(UIFactory.bigMenuButton("Sair do Sistema",  e -> nav.logout()));

        add(btns, BorderLayout.CENTER);

        // Rodapé
        JLabel foot = new JLabel("v1.0 • Carmel © 2025", SwingConstants.CENTER);
        foot.setBorder(new EmptyBorder(8, 0, 8, 0));
        foot.setForeground(new Color(180, 210, 240));
        foot.setFont(new Font("Tahoma", Font.PLAIN, 9));
        add(foot, BorderLayout.SOUTH);
    }
}