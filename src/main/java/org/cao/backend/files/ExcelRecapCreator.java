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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Classe permettant de créer un fichier Excel servant à répetorier des informations du logiciel ainsi que des logs d'erreurs.
 *
 */
public class ExcelRecapCreator extends FileCreator {

    public static List<String> errorNoSAP = new ArrayList<>();
    public static List<String> errorNoFolders = new ArrayList<>();
    public static List<String> errorBadName = new ArrayList<>();
    public static List<String> errorRevision = new ArrayList<>();
    public static List<String> errorDescription = new ArrayList<>();

    public static void startCreation() {
        startDateLog = String.valueOf(LocalDate.now());
        startHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

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
                {"Nombre d'Articles Non SAP:", errorNoSAP.size()},
                {"Nombre d'Articles Non Dossiers:", errorNoFolders.size()},
                {"Nombre d'Articles Nom Incorrect:", errorBadName.size()},
                {"Nombre d'Articles Erreur Révision:", errorRevision.size()},
                {"Nombre d'Articles Sans Description:", errorDescription.size()},
                {"", ""},
                {"Nombre total d'erreurs:", errorNoSAP.size() + errorNoFolders.size() + errorBadName.size() + errorRevision.size() + errorDescription.size()}
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

                    if (rowNum > 8 && (Integer) rowData[1] > 0) {
                        valueCell.setCellStyle(errorStyle);
                    } else {
                        valueCell.setCellStyle(valueStyle);
                    }
                }
            }
        }

        resultSheet.setColumnWidth(0, 11000);
        resultSheet.setColumnWidth(1, 4000);

        String errorTitle = "Rapport d'erreurs | Articles non-présents dans SAP mais présents dans les dossiers";
        String error2Title = "Rapport d'erreurs | Articles présents dans SAP mais non-présents dans les dossiers";
        String error3Title = "Rapport d'erreurs | Articles avec un nom incorrect (ne commence pas par \"AF\")";
        String error4Title = "Rapport d'erreurs | Articles avec une version plus récente dans SAP que les dossiers";
        String error5Title = "Rapport d'erreurs | Articles enregistrés sans description";

        createSheet(workbook, "Articles Non SAP", errorTitle, errorNoSAP);
        createSheet(workbook, "Articles Non Dossiers", error2Title, errorNoFolders);
        createSheet(workbook, "Articles Nom Incorrect", error3Title, errorBadName);
        createSheet(workbook, "Articles Mauvaise Revision", error4Title, errorRevision);
        createSheet(workbook, "Articles Sans Description", error5Title, errorDescription);


        String fileExlName = "MAJBasePlans-[RECAP]" + ".xlsx";
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
        font.setFontHeightInPoints((short) 20);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle createValueStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createErrorStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);

        java.awt.Color errorColor = RowErrorBackgroundColor.ERROR.getColor();
        XSSFColor xssfColor = new XSSFColor(new byte[]{
                (byte) errorColor.getRed(),
                (byte) errorColor.getGreen(),
                (byte) errorColor.getBlue()
        }, null);
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

    /**
     * Crée une nouvelle feuille dans le classeur Excel
     * @param workbook Le classeur Excel
     * @param sheetName Le nom de la feuille
     * @param title Le titre à afficher en haut de la feuille
     * @param data Les données à insérer (une seule colonne)
     */
    private void createSheet(Workbook workbook, String sheetName, String title, List<String> data) {
        Sheet sheet = workbook.createSheet(sheetName);

        CellStyle headerStyle = createHeaderStyle(workbook);

        CellStyle boldLabelStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setFontHeightInPoints((short) 14);
        boldLabelStyle.setFont(boldFont);
        boldLabelStyle.setBorderTop(BorderStyle.THIN);
        boldLabelStyle.setBorderBottom(BorderStyle.THIN);
        boldLabelStyle.setBorderLeft(BorderStyle.THIN);
        boldLabelStyle.setBorderRight(BorderStyle.THIN);
        boldLabelStyle.setAlignment(HorizontalAlignment.LEFT);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(headerStyle);

        sheet.createRow(1);

        int rowNum = 2;
        for (String rowData : data) {
            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue(rowData);
            cell.setCellStyle(boldLabelStyle);
        }

        sheet.setColumnWidth(0, 11000);
    }
}