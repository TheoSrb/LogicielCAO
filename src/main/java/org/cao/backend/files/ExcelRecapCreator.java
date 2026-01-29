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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ExcelRecapCreator extends FileCreator {

    static void main() {
        startDateLog = String.valueOf(LocalDate.now());
        startHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));

        ExcelRecapCreator erc = new ExcelRecapCreator();
        erc.createNewFile();
    }

    private int[] returnFilesNumber() {
        DatabaseManager.startConnectionWithDatabase();

        int[] filesNumber = new int[8];

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD); Statement statement = con.createStatement()) {

            String query = "SELECT * FROM Compteurs";

            ResultSet resultSet = statement.executeQuery(query);

            if (resultSet.next()) {
                filesNumber[0] = resultSet.getInt("NbScan");
                filesNumber[1] = resultSet.getInt("NbPlan");
                filesNumber[2] = resultSet.getInt("Nb3D");
                filesNumber[3] = resultSet.getInt("NbAss");
                filesNumber[4] = resultSet.getInt("NbSchema");
                filesNumber[5] = resultSet.getInt("NbEclate");
                filesNumber[6] = resultSet.getInt("NbPFEclate");
                filesNumber[7] = resultSet.getInt("NbConfig");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return filesNumber;
    }

    @Override
    public void createNewFile() {
        Workbook workbook = new XSSFWorkbook();
        Sheet resultSheet = workbook.createSheet("Résultat");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle labelStyle = createLabelStyle(workbook);
        CellStyle valueStyle = createValueStyle(workbook);
        CellStyle errorStyle = createErrorStyle(workbook);

        String title = "Mise à jour du " + translateDayOfWeek(LocalDate.now().getDayOfWeek().toString()) + " " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " à " +
                LocalTime.now().getHour() + "h" + LocalTime.now().getMinute();

        Row titleRow = resultSheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);

        resultSheet.createRow(1);

        int[] allCompteursDatas = returnFilesNumber();

        Object[][] data = {
                {"Nombre de Plans:", allCompteursDatas[1]},
                {"Nombre d'Eclatés:", allCompteursDatas[5]},
                {"Nombre de Scan:", allCompteursDatas[0]},
                {"Nombre de Schémas Electriques:", allCompteursDatas[4]},
                {"Nombre de Configurations Froid:", allCompteursDatas[7]},
                {"", ""},
                {"Nombre d'Erreurs Fichier:", 0},
                {"Nombre d'Erreurs Révision:", 0},
                {"Nombre d'Articles Non SAP:", 0},
                {"Nombre d'Articles Sans Descriptions:", 0}
        };

        int rowNum = 2;
        for (Object[] rowData : data) {
            Row row = resultSheet.createRow(rowNum++);

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

        resultSheet.setColumnWidth(0, 8000);
        resultSheet.setColumnWidth(1, 3000);

        String fileExlName = "MAJBasePlans" + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".xlsx";
        File fileExl = new File(EXCEL_RECAP_PATH + fileExlName);

        if (fileExl.exists()) {
            fileExl.delete();
        }

        try (FileOutputStream fileOut = new FileOutputStream(fileExl)) {
            workbook.write(fileOut);
            System.out.println("Fichier Excel créé");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
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