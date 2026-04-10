package br.carmel.ui.dialogs;

import br.carmel.model.Cliente;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Diálogo que exibe a listagem de Clientes em tabela.
 */
public class ClientesTableDialog extends JDialog {

    public ClientesTableDialog(Component parent, EntityManagerFactory emf) {
        super(SwingUtilities.getWindowAncestor(parent), "Clientes", ModalityType.APPLICATION_MODAL);
        setSize(800, 400);
        setLocationRelativeTo(parent);

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Nome", "CPF", "Telefone", "Email"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Cliente> list = em.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList();
            for (Cliente c : list) {
                model.addRow(new Object[]{ c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), c.getEmail() });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (em != null) em.close();
        }

        JTable table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(760, 300));
        add(new JScrollPane(table));
        pack();
    }
}