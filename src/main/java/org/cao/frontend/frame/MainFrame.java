package org.cao.frontend.frame;

import org.cao.backend.TableBuilder;
import org.cao.backend.TableRow;
import org.cao.backend.logs.LogsBuilder;
import org.cao.frontend.renderer.TableRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

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
        File logsDirectory = new File(LogsBuilder.LOGS_DIRECTORY);

        if (Objects.requireNonNull(logsDirectory.listFiles()).length > 0) {
            for (File logFile : logsDirectory.listFiles()) {
                try (Scanner scanner = new Scanner(logFile)) {
                    TableRow row;
                    String line = scanner.nextLine();
                    String[] allLineDatas = line.split(";");

                    String startDate = allLineDatas[0];
                    String startHour = allLineDatas[1];
                    String endDate = allLineDatas[2];
                    String endHour = allLineDatas[3];
                    String task = allLineDatas[4];
                    String operation = allLineDatas[5];
                    boolean isError = Boolean.parseBoolean(allLineDatas[6]);
                    boolean isWarning = Boolean.parseBoolean(allLineDatas[7]);

                    // ===== Création et remplissage d'une ligne =====

                    row = new TableRow.TableRowBuilder()
                            .withStartDate(startDate)
                            .withStartHour(startHour)
                            .withEndDate(endDate)
                            .withEndHour(endHour)
                            .withTask(task)
                            .withOperation(operation)
                            .withError(isError)
                            .withWarning(isWarning)
                            .build();

                    // ===== Affctation de cette ligne au constructeur du futur tableau principal =====
                    tableBuilder.addRow(row);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
        File logsDirectory = new File(LogsBuilder.LOGS_DIRECTORY);
        if (Objects.requireNonNull(logsDirectory.listFiles()).length <= 0) {
            System.err.println("Erreur : le dossier des logs est vide.");
            return;
        }

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
