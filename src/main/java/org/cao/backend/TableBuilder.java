package org.cao.backend;

import java.util.ArrayList;
import java.util.List;

public class TableBuilder {

    // =============== Attributs ===============

    private String[] columnsNames;
    private Object[][] datas;

    private List<TableRow> rows = new ArrayList<>();

    // =============== Constructeur ===============

    public TableBuilder(String[] columnsNames) {
        this.columnsNames = columnsNames;
    }

    // =============== Méthodes ===============

    public void addRow(TableRow tableRow) {
        rows.add(tableRow);

        Object[] newRow = {
                tableRow.getStartDate(),
                tableRow.getStartHour(),
                tableRow.getEndDate(),
                tableRow.getEndHour(),
                tableRow.getTask(),
                tableRow.getOperation(),
                tableRow.isError(),
                tableRow.isWarning()
        };

        if (datas == null) {
            datas = new Object[1][];
            datas[0] = newRow;
        } else {
            Object[][] newDatas = new Object[datas.length + 1][];
            System.arraycopy(datas, 0, newDatas, 0, datas.length);
            newDatas[datas.length] = newRow;
            datas = newDatas;
        }
    }



    // =============== Accesseurs ===============

    public String[] getColumnsNames() {
        return columnsNames;
    }

    public Object[][] getDatas() {
        return datas;
    }

    public List<TableRow> getRows() {
        return rows;
    }

    public void setColumnsNames(String[] columnsNames) {
        this.columnsNames = columnsNames;
    }
}
