package org.cao.backend.files;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.cao.backend.db.DatabaseManager;
import org.cao.frontend.renderer.RowErrorBackgroundColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ExcelRecapCreator extends FileCreator {

    static void main() {
        startDateLog = String.valueOf(LocalDate.now());
        startHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));

        ExcelRecapCreator erc = new ExcelRecapCreator();
        erc.createNewFile();
    }

    private String[] returnFilesNumber() {
        DatabaseManager.startConnectionWithDatabase();

        String[] filesNumber = {};

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD); Statement statement = con.createStatement()) {

            String query = "SELECT * FROM Compteurs";

            ResultSet resultSet = statement.executeQuery(query);


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return filesNumber;
    }

    @Override
    public void createNewFile() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Récapitulatif");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle labelStyle = createLabelStyle(workbook);
        CellStyle valueStyle = createValueStyle(workbook);
        CellStyle errorStyle = createErrorStyle(workbook);

        String title = "Mise à jour du " + translateDayOfWeek(LocalDate.now().getDayOfWeek().toString()) + " " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " à " +
                LocalTime.now().getHour() + "h" + LocalTime.now().getMinute();

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);

        sheet.createRow(1);

        Object[][] data = {
                {"Nombre de Plans:", 60697},
                {"Nombre d'Eclatés:", 1055},
                {"Nombre de Scan:", 14537},
                {"Nombre de Schémas Electriques:", 647},
                {"Nombre de Configurations Froid:", 861},
                {"", ""},
                {"Nombre d'Erreurs Fichier:", 10},
                {"Nombre d'Erreurs Révision:", 2},
                {"Nombre d'Articles Non SAP:", 0},
                {"Nombre d'Articles Sans Descriptions:", 21079}
        };

        int rowNum = 2;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowNum++);

            Cell labelCell = row.createCell(0);
            labelCell.setCellValue((String) rowData[0]);
            labelCell.setCellStyle(labelStyle);

            if (rowData[1] != null && !rowData[1].equals("")) {
                Cell valueCell = row.createCell(1);

                if (rowData[1] instanceof Integer) {
                    valueCell.setCellValue((Integer) rowData[1]);

                    String label = (String) rowData[0];
                    if (label.contains("Erreur") && (Integer) rowData[1] > 0) {
                        valueCell.setCellStyle(errorStyle);
                    } else {
                        valueCell.setCellStyle(valueStyle);
                    }
                }
            }
        }

        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 3000);

        try (FileOutputStream fileOut = new FileOutputStream("GeneratedExcel.xlsx")) {
            workbook.write(fileOut);
            System.out.println("Excel file generated successfully: GeneratedExcel.xlsx");
        } catch (IOException e) {
            System.err.println("Error writing Excel file: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                System.err.println("Error closing workbook: " + e.getMessage());
            }
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createErrorStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.RIGHT);

        java.awt.Color errorColor = RowErrorBackgroundColor.ERROR.getColor();
        XSSFColor xssfColor = new XSSFColor(errorColor, null);
        style.setFillForegroundColor(xssfColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private String translateDayOfWeek(String dayOfWeekEN) {
        return switch (dayOfWeekEN) {
            case "MONDAY" -> "Lundi";
            case "TUESDAY" -> "Mardi";
            case "WEDNESDAY" -> "Mercredi";
            case "THURSDAY" -> "Jeudi";
            case "FRIDAY" -> "Vendredi";
            case "SATURDAY" -> "Samedi";
            case "SUNDAY" -> "Dimanche";
            default -> "X";
        };
    }
}