package org.cao.backend;

import org.cao.frontend.renderer.RowErrorBackgroundColor;

import java.awt.*;

public class TableRow {

    private String startDate;
    private String startHour;
    private String endDate;
    private String endHour;
    private String task;
    private String operation;
    private boolean isError;
    private boolean isWarning;

    private Color backgroundColor = Color.WHITE;

    public static class TableRowBuilder {

        // ================ Attributs ===============

        private String startDate;
        private String startHour;
        private String endDate;
        private String endHour;
        private String task;
        private String operation;
        private boolean isError;
        private boolean isWarning;

        // ================ Méhodes ===============

        public TableRowBuilder withStartDate(String startDate) {
            this.startDate = startDate;
            return this;
        }

        public TableRowBuilder withStartHour(String startHour) {
            this.startHour = startHour;
            return this;
        }

        public TableRowBuilder withEndDate(String endDate) {
            this.endDate = endDate;
            return this;
        }

        public TableRowBuilder withEndHour(String endHour) {
            this.endHour = endHour;
            return this;
        }

        public TableRowBuilder withTask(String task) {
            this.task = task;
            return this;
        }

        public TableRowBuilder withOperation(String operation) {
            this.operation = operation;
            return this;
        }

        public TableRowBuilder withError(boolean isError) {
            this.isError = isError;
            return this;
        }

        public TableRowBuilder withWarning(boolean isWarning) {
            this.isWarning = isWarning;
            return this;
        }

        public TableRow build() {
            TableRow tableRow = new TableRow();
            tableRow.startDate = startDate;
            tableRow.startHour = startHour;
            tableRow.endDate = endDate;
            tableRow.endHour = endHour;
            tableRow.task = task;
            tableRow.operation = operation;
            tableRow.isError = isError;
            tableRow.isWarning = isWarning;

            if (isError) {
                tableRow.backgroundColor = RowErrorBackgroundColor.ERROR.getColor();
            } else if (isWarning) {
                tableRow.backgroundColor = RowErrorBackgroundColor.WARNING.getColor();
            } else {
                tableRow.backgroundColor = Color.WHITE;
            }

            return tableRow;
        }
    }

    // ================ Accesseurs ===============

    public String getStartDate() {
        return startDate;
    }

    public String getStartHour() {
        return startHour;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getEndHour() {
        return endHour;
    }

    public String getTask() {
        return task;
    }

    public String getOperation() {
        return operation;
    }

    public boolean isError() {
        return isError;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }


}
