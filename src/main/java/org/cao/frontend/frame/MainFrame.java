package org.cao.frontend.frame;

import org.cao.backend.TableBuilder;
import org.cao.backend.TableRow;
import org.cao.frontend.renderer.TableRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

/**
 *
 * Classe permettant d'afficher la fenêtre principale du logiciel (IHM), là où toutes les informations seront affichées.
 *
 */
public class MainFrame extends JFrame implements ActionListener {

    /*
    Initialisation des éléments de la page.
     */

    private static final int FRAME_WIDTH = 1200;
    private static final int FRAME_HEIGHT = 900;

    /*
    Création du constructeur du tableau.
     */
    private TableBuilder tableBuilder = new TableBuilder(
            new String[]{
                    "Date Début", "Heure Début", "Date Fin", "Heure Fin", "Tâche", "Opération", "Erreur", "Avertissement"
            }
    );

    /*
    Initialisation du tableau principal.
     */
    private JTable mainTable;

    /*
    Initialisation du scroll pane du tableau.
     */
    private JScrollPane scrollPane;

    /*
    Création des éléments principaux de la fenêtre.
     */
    public MainFrame() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new FlowLayout());   // La disposition des éléments dans la fenêtre.
        this.setTitle("Plan CAO | Menu principal");
        this.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

        initialiseTables();
    }

    /**
     * Méthdode permettant d'initialiser les lignes du tableau.
     */
    public void initialiseRows() {
        for (int i = 0; i < 100 ; i++) {
            TableRow row;
            Random random = new Random();

            // ===== Création et remplissage d'une ligne =====

            boolean rowWarning = random.nextInt(10) == 0;
            boolean rowError = !rowWarning && random.nextInt(20) == 0;

            row = new TableRow.TableRowBuilder()
                    .withStartDate("[Date Début] " + String.valueOf(i))
                    .withStartHour("[Heure Début]")
                    .withEndDate("[Date Fin]")
                    .withEndHour("[Heure Fin]")
                    .withTask("[Tâche]")
                    .withOperation("[Opération]")
                    .withError(rowError)
                    .withWarning(rowWarning)
                    .build();

            // ===== Affctation de cette ligne au constructeur du futur tableau principal =====
            tableBuilder.addRow(row);
        }
    }

    /**
     * Méthode permettant d'initialiser un tableau dans le bon ordre afin d'éviter les bugs.
     */
    private void initialiseTables() {
        // ===== Création et remplissage de chaque ligne =====
        initialiseRows();

        // ===== Création du tableau à partir du builder =====
        mainTable = new JTable(tableBuilder.getDatas(), tableBuilder.getColumnsNames()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // On désactive le fait que l'utilisateur puisse toucher aux checkbox.
            }
        };

        // Afficher uniquement les lignes verticales et non les ligns horizontales, pour plus de visibilité.
        mainTable.setShowHorizontalLines(false);
        mainTable.setShowVerticalLines(true);

        // On indique que la colonne 6 et 7 contiennent des checkbox avec les valeurs booléennes de la ligne.
        mainTable.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        mainTable.getColumnModel().getColumn(6).setCellRenderer(mainTable.getDefaultRenderer(Boolean.class));

        mainTable.getColumnModel().getColumn(7).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        mainTable.getColumnModel().getColumn(7).setCellRenderer(mainTable.getDefaultRenderer(Boolean.class));


        // ===== Application du rendu =====
        TableRenderer.applyTableRenderer(mainTable, tableBuilder.getRows());

        // ===== Création du scroll pane sur le tableau principal =====
        scrollPane = new JScrollPane(mainTable);

        mainTable.setFillsViewportHeight(true);
        scrollPane.setPreferredSize(new Dimension(FRAME_WIDTH - 25, FRAME_HEIGHT - 100));   // La taille du scroll pane et du tableau prennen la taille de la fenêtre avec une marge.
    }

    /**
     * Méthode permettant d'afficher les éléments de la fenêtre.
     */
    public void startDisplaying() {
        this.add(scrollPane);
        this.pack();
        this.setVisible(true);
    }

    /**
     * Méthode permettant de répartir les différentes actions des différents boutons de la fenêtre.
     * @param e L'évènement qui a été déclenché.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case "":     // Si le bouton marqué par "" est pressé...
                break;
        }
    }
}
