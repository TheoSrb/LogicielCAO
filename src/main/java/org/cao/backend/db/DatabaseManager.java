package org.cao.backend.db;

import org.cao.backend.files.FileCreator;
import org.cao.backend.errors.ErrorBuilder;
import org.cao.backend.errors.ErrorRegistry;
import org.cao.backend.files.PDFReader;
import org.cao.backend.helper.FileHelper;
import org.cao.backend.logs.LogsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DatabaseManager {

    public static final String URL = readProperty("db.url");
    public static final String USER = readProperty("db.user");
    public static final String PASSWORD = readProperty("db.password");

    public static final String ARTICLES_OUT_BONG_PATH = readProperty("file.out.path");
    public static final String DIRECTORY_PATH = readProperty("directory.path");

    public static final List<String> AUTHORIZED_FOLDERS_NAMES = separateAllAuthorizedFolders(readProperty("authorized.explore.folders"));


    private static String startDateLog;
    private static String startHourLog;

    private static boolean isErrorLog = false;
    private static boolean isWarningLog = false;

    private static ErrorBuilder potentialError = null;

    static void main() {
        updateDatabase();
    }

    private static void updateDatabase() {
        startConnectionWithDatabase();

        startDateLog = String.valueOf(LocalDate.now());
        startHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD); Statement statement = con.createStatement()) {

            fillFichierTable(con, statement);
            fillArticleCANTable(con);
            fillCompteursTable(con);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createLog(String task, String operation, ErrorBuilder potentialError) {
        String endDateLog = String.valueOf(LocalDate.now());
        String endHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        LogsBuilder logsBuilder = new LogsBuilder(
                startDateLog,
                startHourLog,
                endDateLog,
                endHourLog,
                task,
                operation,
                isErrorLog,
                isWarningLog,
                potentialError
        );

        logsBuilder.updateLogsFile(LogsBuilder.LOGS_DIRECTORY);
    }

    public static void startConnectionWithDatabase() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void fillCompteursTable(Connection con) throws SQLException {
        startConnectionWithDatabase();

        int nbScan = 0;
        int nbPlan = 0;
        int nbSchema = 0;
        int nbEclate = 0;
        int nbConfig = 0;

        String countQuery = "SELECT COUNT(*) FROM Fichier WHERE Type = 'SCAN'";
        try (PreparedStatement ps = con.prepareStatement(countQuery)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nbScan = rs.getInt(1);
            }
        }

        countQuery = "SELECT COUNT(*) FROM Fichier WHERE Type = 'PLAN'";
        try (PreparedStatement ps = con.prepareStatement(countQuery)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nbPlan = rs.getInt(1);
            }
        }

        countQuery = "SELECT COUNT(*) FROM Fichier WHERE Type = 'ELEC'";
        try (PreparedStatement ps = con.prepareStatement(countQuery)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nbSchema = rs.getInt(1);
            }
        }

        countQuery = "SELECT COUNT(*) FROM Fichier WHERE Type = 'PLAN_ECL'";
        try (PreparedStatement ps = con.prepareStatement(countQuery)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nbEclate = rs.getInt(1);
            }
        }

        countQuery = "SELECT COUNT(*) FROM Fichier WHERE Type = 'CONFIG'";
        try (PreparedStatement ps = con.prepareStatement(countQuery)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                nbConfig = rs.getInt(1);
            }
        }

        con.setAutoCommit(false);

        try {
            String deleteQuery = "DELETE FROM Compteurs";
            try (PreparedStatement ps = con.prepareStatement(deleteQuery)) {
                ps.executeUpdate();
            }

            String insertQuery = "INSERT INTO Compteurs (NbScan, NbPlan, Nb3D, NbAss, NbSchema, NbEclate, NbPFEclate, NbConfig) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(insertQuery)) {
                ps.setInt(1, nbScan);
                ps.setInt(2, nbPlan);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.setInt(5, nbSchema);
                ps.setInt(6, nbEclate);
                ps.setInt(7, 0);
                ps.setInt(8, nbConfig);

                ps.executeUpdate();
            }

            con.commit();

        } catch (SQLException e) {
            con.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    private static void fillArticleCANTable(Connection con) throws SQLException {
        File fileOut = new File(ARTICLES_OUT_BONG_PATH);
        FileHelper fileHelper = new FileHelper(fileOut);

        List<String> lines = fileHelper.readAllLines();
        long maxLines = lines.stream()
                .map(l -> l.split(";", -1))
                .filter(p -> p.length >= 2 && !p[1].trim().isEmpty())
                .count();

        int i = 0;

        String mergeQuery =
                "MERGE ArticleCAN AS target " +
                        "USING (SELECT ? AS CodeCAN, ? AS DescCAN) AS source " +
                        "ON target.CodeCAN = source.CodeCAN " +
                        "WHEN MATCHED THEN " +
                        "  UPDATE SET DescCAN = source.DescCAN " +
                        "WHEN NOT MATCHED THEN " +
                        "  INSERT (CodeCAN, LP, AchFab, Statut, DescCAN, DescCANUK, UM, Matiere) " +
                        "  VALUES (source.CodeCAN, '.', '.', '.', source.DescCAN, '.', '.', '.');";

        con.setAutoCommit(false);

        try (PreparedStatement ps = con.prepareStatement(mergeQuery)) {

            for (String line : lines) {
                String[] parts = line.split(";", -1);
                if (parts.length < 2) continue;

                String codeCAN = parts[0].trim();
                String descCAN = parts[1].trim();

                if (descCAN.isEmpty()) continue;

                ps.setString(1, codeCAN);
                ps.setString(2, descCAN);
                ps.addBatch();

                i++;

                if (i % 250 == 0) {
                    ps.executeBatch();
                    con.commit();

                    int percentage = (int) (((float) i / maxLines) * 100);
                    System.out.print("\rRemplissage de la table ArticleCAN en cours: " + percentage + "%");
                }
            }

            ps.executeBatch();
            con.commit();

            System.out.println("\rRemplissage de la table ArticleCAN en cours: 100%");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        }

        String verificationQuery = "SELECT * FROM ArticleCAN WHERE CodeCAN NOT IN (SELECT CodeCAN FROM Fichier);";

        try (PreparedStatement ps = con.prepareStatement(verificationQuery)) {
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                isErrorLog = true;
                ErrorBuilder error = ErrorRegistry.ARTICLES_NOT_IN_SAP;
                potentialError = error;
            }
        }

        createLog("MAJ BDD", "La base de données à été mise à jour.", potentialError);
    }


    private static void fillFichierTable(Connection con, Statement statement) throws SQLException {
        File mainDirectory = new File(DIRECTORY_PATH);
        List<File> files = returnFilesInDirectory(mainDirectory);
        int i = 0;
        int maxFiles = files.size();

        con.setAutoCommit(false);

        Set<String> existingKeys = new HashSet<>();
        String selectQuery = "SELECT CodeCAN, Revision FROM Fichier";
        try (PreparedStatement ps = con.prepareStatement(selectQuery);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString("CodeCAN") + "|" + rs.getString("Revision");
                existingKeys.add(key);
            }
        }

        Map<String, String> latestFileNames = new HashMap<>();
        for (File file : files) {
            String fileName = file.getName();
            String codeCAN = getCodeCAN(fileName);

            if (!latestFileNames.containsKey(codeCAN) || fileName.compareTo(latestFileNames.get(codeCAN)) > 0) {
                latestFileNames.put(codeCAN, fileName);
            }
        }

        String updateQuery = "UPDATE Fichier SET Nom = ?, RepertoireNom = ?, Type = ?, Chemin = ?, Taille = ?, Page = ?, Lien = ?, DernRev = ?, IncoRev = ? WHERE CodeCAN = ? AND Revision = ?";
        String insertQuery = "INSERT INTO Fichier(Nom, RepertoireNom, Type, Chemin, Taille, CodeCAN, Revision, Page, Lien, DernRev, IncoRev) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement updateStmt = con.prepareStatement(updateQuery);
             PreparedStatement insertStmt = con.prepareStatement(insertQuery)) {

            for (File file : files) {
                String fileName = file.getName();
                String fileType = convertParentToType(file.getParentFile().getName());
                String codeCAN = getCodeCAN(fileName);
                String revision = getRevision(fileName);
                String filePath = file.getAbsolutePath();

                String key = codeCAN + "|" + revision;
                boolean exists = existingKeys.contains(key);

                String dernRev = fileName.equals(latestFileNames.get(codeCAN)) ? "1" : "0";

                PreparedStatement ps = exists ? updateStmt : insertStmt;

                ps.setString(1, fileName);
                ps.setString(2, ".");
                ps.setString(3, fileType);
                ps.setString(4, ".");
                ps.setInt(5, 0);
                ps.setString(12, ".");

                if (exists) {
                    ps.setInt(6, 0);
                    ps.setString(7, filePath);
                    ps.setString(8, dernRev);
                    ps.setString(9, "0");
                    ps.setString(10, codeCAN);
                    ps.setString(11, revision);
                } else {
                    ps.setString(6, codeCAN);
                    ps.setString(7, revision);
                    ps.setInt(8, 0);
                    ps.setString(9, filePath);
                    ps.setString(10, dernRev);
                    ps.setString(11, "0");
                    existingKeys.add(key);
                }

                ps.addBatch();

                i++;

                if (i % 100 == 0) {
                    int percentage = (int) (((float) i / maxFiles) * 100);
                    System.out.print("\rRemplissage de la table Fichier en cours: " + percentage + "%");
                }

                if (i % 500 == 0) {
                    updateStmt.executeBatch();
                    insertStmt.executeBatch();
                    con.commit();
                }
            }

            updateStmt.executeBatch();
            insertStmt.executeBatch();
            con.commit();

            System.out.println("\rRemplissage de la table Fichier en cours: 100%");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        }

        fillPDFChilds(con, files);
    }

    private static void fillPDFChilds(Connection con, List<File> files) throws SQLException {
        List<File> pdfFiles = files.stream()
                .filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
                .filter(f -> f.getParentFile().getName().contains("ECLATE_online"))
                .toList();

        int i = 0;
        int maxPdfs = pdfFiles.size();

        String updateQuery = "UPDATE Fichier SET CodeCAN_Enfant = ? WHERE CodeCAN = ? AND Revision = ?";

        con.setAutoCommit(false);

        try (PreparedStatement ps = con.prepareStatement(updateQuery)) {

            for (File file : pdfFiles) {
                String fileName = file.getName();
                String codeCAN = getCodeCAN(fileName);
                String revision = getRevision(fileName);
                String filePath = file.getAbsolutePath();

                try {
                    PDFReader pdfReader = new PDFReader(filePath);
                    List<String> childs = pdfReader.getChilds();

                    if (childs != null && !childs.isEmpty()) {
                        StringBuilder jsonBuilder = new StringBuilder("[");
                        for (int j = 0; j < childs.size(); j++) {
                            jsonBuilder.append("\"").append(childs.get(j).replace("\"", "\\\"")).append("\"");
                            if (j < childs.size() - 1) {
                                jsonBuilder.append(",");
                            }
                        }
                        jsonBuilder.append("]");

                        ps.setString(1, jsonBuilder.toString());
                        ps.setString(2, codeCAN);
                        ps.setString(3, revision);
                        ps.addBatch();
                    }
                } catch (Exception e) {
                }

                i++;

                if (i % 50 == 0) {
                    ps.executeBatch();
                    con.commit();
                    int percentage = (int) (((float) i / maxPdfs) * 100);
                    System.out.print("\rRemplissage des enfants PDF en cours: " + percentage + "%");
                }
            }

            ps.executeBatch();
            con.commit();

            System.out.println("\rRemplissage des enfants PDF en cours: 100%");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        }
    }

    public static List<File> returnFilesInDirectory(File directory) {
        return returnFilesInDirectory(directory, new HashSet<>());
    }

    private static List<File> returnFilesInDirectory(File directory, Set<String> visited) {
        if (!visited.add(directory.getAbsolutePath())) {
            return new ArrayList<>();
        }

        File[] files = directory.listFiles();
        List<File> allSubFiles = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (isFolderAuthorized(file)) {
                        allSubFiles.addAll(returnFilesInDirectory(file, visited));
                    }
                } else {
                    allSubFiles.add(file);
                }
            }
        }

        return allSubFiles;
    }

    private static String getCodeCAN(String fileName) {
        return fileName.split("_")[0];
    }

    private static String getRevision(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        String extension = fileName.substring(lastDot);

        return fileName.split("_")[fileName.split("_").length - 1].split(extension)[0];
    }

    private static boolean isFolderAuthorized(File file) {
        List<String> foldersNameAuthorized = AUTHORIZED_FOLDERS_NAMES;
        String folderName = file.getName();
        return foldersNameAuthorized.contains(folderName);
    }

    public static String convertParentToType(String parentName) {
        return switch (parentName) {
            case "ELEC_online" -> "ELEC";
            case "CONFIG_online" -> "CONFIG";
            case "PDFS_online" -> "PLAN";
            case "ECLATE_online" -> "PLAN_ECL";
            case "SCANNE_online" -> "SCAN";
            default -> "Non-renseigné";
        };
    }

    public static String readProperty(String property) {
        Properties props = new Properties();

        try (InputStream fis = FileCreator.class.getClassLoader().getResourceAsStream("config.properties")) {
            props.load(fis);
            return props.getProperty(property);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Erreur : Propriété introuvable.";
    }

    private static List<String> separateAllAuthorizedFolders(String filesName) {
        return List.of(filesName.split(";"));
    }
}