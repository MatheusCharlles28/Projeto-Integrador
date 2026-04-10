package br.carmel.ui.panels;

import br.carmel.util.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Tela de login estilo Windows XP.
 */
public class LoginPanel extends JPanel {

    private static final String DEFAULT_USER = "sistema";
    private static final String DEFAULT_PASS = "1234";

    public interface OnLoginSuccess { void onSuccess(); }

    public LoginPanel(OnLoginSuccess onSuccess) {
        setLayout(new GridBagLayout());
        setBackground(new Color(58, 110, 165)); // fundo azul XP
        build(onSuccess);
    }

    private void build(OnLoginSuccess onSuccess) {
        // Card central branco
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UIFactory.XP_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 84, 166), 2),
                new EmptyBorder(0, 0, 16, 0)
        ));
        card.setPreferredSize(new Dimension(340, 240));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 12, 6, 12);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Barra azul do topo
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.insets = new Insets(0, 0, 14, 0);
        card.add(UIFactory.xpTitleBar("Acesso ao Sistema"), c);

        c.insets = new Insets(6, 16, 6, 16);
        c.gridwidth = 1;

        c.gridx = 0; c.gridy = 1; card.add(UIFactory.labelLight("Usuário:"), c);
        JTextField tfUser = UIFactory.styledField(DEFAULT_USER);
        tfUser.setPreferredSize(new Dimension(180, 22));
        c.gridx = 1; card.add(tfUser, c);

        c.gridx = 0; c.gridy = 2; card.add(UIFactory.labelLight("Senha:"), c);
        JPasswordField pf = UIFactory.styledPasswordField(DEFAULT_PASS);
        pf.setPreferredSize(new Dimension(180, 22));
        c.gridx = 1; card.add(pf, c);

        JButton btn = UIFactory.bigActionButton("Entrar", e -> {
            String u = tfUser.getText().trim();
            String s = new String(pf.getPassword());
            if (DEFAULT_USER.equals(u) && DEFAULT_PASS.equals(s)) {
                onSuccess.onSuccess();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Usuário ou senha inválidos.", "Erro de Acesso", JOptionPane.ERROR_MESSAGE);
            }
        });
        btn.setPreferredSize(new Dimension(100, 26));

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(btn, c);

        add(card);
    }
}