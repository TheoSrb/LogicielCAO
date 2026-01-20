package org.cao.frontend.renderer;

import org.cao.backend.TableRow;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class TableRenderer {

    public static void applyTableRenderer(JTable table, List<TableRow> rows) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER); // Centre les textes des lignes.

                if (!isSelected && row < rows.size()) {
                    c.setBackground(rows.get(row).getBackgroundColor());    // Applique la couleur en arrière plan.
                }

                return c;
            }
        });
    }
}
