package br.carmel.ui.dialogs;

import br.carmel.model.Produto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Diálogo que exibe a listagem de Produtos em tabela.
 */
public class ProdutosTableDialog extends JDialog {

    public ProdutosTableDialog(Component parent, EntityManagerFactory emf) {
        super(SwingUtilities.getWindowAncestor(parent), "Produtos", ModalityType.APPLICATION_MODAL);
        setSize(900, 400);
        setLocationRelativeTo(parent);

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID", "Nome", "Preço", "Código", "Série"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            List<Produto> list = em.createQuery("SELECT p FROM Produto p", Produto.class).getResultList();
            for (Produto p : list) {
                model.addRow(new Object[]{ p.getId(), p.getNome(), p.getValor(), p.getCodBarras(), p.getNumeroSerie() });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (em != null) em.close();
        }

        JTable table = new JTable(model);
        table.setPreferredScrollableViewportSize(new Dimension(860, 300));
        add(new JScrollPane(table));
        pack();
    }
}