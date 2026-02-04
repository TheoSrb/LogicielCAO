package org.cao.backend.db;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.util.stream.Collectors;

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

    private static List<ErrorBuilder> potentialErrors = new ArrayList<>();

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

        createLog("MAJ BDD", "La base de données à été mise à jour.", potentialErrors);
        System.out.println("\n\nLa base de données à été mise à jour !");
    }

    private static void createLog(String task, String operation, List<ErrorBuilder> potentialError) {
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
                    System.out.print("\rTraitement de la table ArticleCAN en cours: " + percentage + "%");
                }
            }

            ps.executeBatch();
            con.commit();

        } catch (SQLException e) {
            con.rollback();
            throw e;
        }

        System.out.print("\rTraitement de la table ArticleCAN en cours: 100%");
        System.out.println("\nVérification et détections des erreurs en cours...");


        File mainDirectory = new File(DatabaseManager.DIRECTORY_PATH);
        List<File> filesList = DatabaseManager.returnFilesInDirectory(mainDirectory);
        List<String> outLines = fileHelper.readAllLines();


        List<String> filesNamesList = new ArrayList<>();
        List<String> codesCANInSAP = new ArrayList<>();

        for (File file : filesList) {
            filesNamesList.add(file.getName().split("_")[0]);
        }

        for (String e : outLines) {
            codesCANInSAP.add(e.split(";")[0]);
        }

        // Les différentes erreurs qui ne sont pas dans SAP, mais dans les dossiers.
        List<String> erreurTry1 = getDifference(filesNamesList, codesCANInSAP);

        if (!erreurTry1.isEmpty()) {
            isErrorLog = true;
            ErrorBuilder error = ErrorRegistry.ARTICLES_NOT_IN_SAP;
            error.setArticlesConcerned(erreurTry1);
            potentialErrors.add(error);
        }

        // Les différentes erreurs qui sont dans SAP, mais qui ne sont pas dans les dossiers;
        List<String> erreurTry2 = getDifference(codesCANInSAP, filesNamesList);

        if (!erreurTry2.isEmpty()) {
            isErrorLog = true;
            ErrorBuilder error = ErrorRegistry.ARTICLES_NOT_IN_FOLDERS;
            error.setArticlesConcerned(erreurTry2);
            potentialErrors.add(error);
        }


        // Les différentes erreurs qui comportent un nom de fichier inorrect.
        List<String> erreurTry3 = getFilesWithIncorrectNames(filesList);

        if (!erreurTry3.isEmpty()) {
            isErrorLog = true;
            ErrorBuilder error = ErrorRegistry.FILE_NAME_ERROR;
            error.setArticlesConcerned(erreurTry3);
            potentialErrors.add(error);
        }


        // Les différentes erreurs qui ont une version antérieure dans les dossiers par rapport à SAP.
        List<String> erreurTry4 = getArticlesWithOutdatedVersion(filesList, outLines);

        if (!erreurTry4.isEmpty()) {
            isErrorLog = true;
            ErrorBuilder error = ErrorRegistry.REVISION_ERROR;
            error.setArticlesConcerned(erreurTry4);
            potentialErrors.add(error);
        }


        // Les différentes erreurs qui n'ont pas de descriptions dans SAP.
        List<String> erreurTry5 = getArticlesWithEmptyDescription(outLines);

        if (!erreurTry5.isEmpty()) {
            isWarningLog = true;
            ErrorBuilder error = ErrorRegistry.ARTICLES_WITHOUT_DESCRIPTION;
            error.setArticlesConcerned(erreurTry5);
            potentialErrors.add(error);
        }
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

        String insertQuery = "INSERT INTO Fichier(Nom, RepertoireNom, Type, Chemin, Taille, CodeCAN, Revision, Page, Lien, DernRev, IncoRev, CodeCAN_Enfant) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement insertStmt = con.prepareStatement(insertQuery)) {

            for (File file : files) {
                String fileName = file.getName();
                String fileType = convertParentToType(file.getParentFile().getName());
                String codeCAN = getCodeCAN(fileName);
                String revision = getRevision(fileName);
                String filePath = file.getAbsolutePath();

                String key = codeCAN + "|" + revision;
                boolean exists = existingKeys.contains(key);

                if (exists) continue;

                String dernRev = fileName.equals(latestFileNames.get(codeCAN)) ? "1" : "0";

                String childsCAN = null;
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    childsCAN = fillPDFChilds(filePath);
                }

                insertStmt.setString(1, fileName);
                insertStmt.setString(2, ".");
                insertStmt.setString(3, fileType);
                insertStmt.setString(4, ".");
                insertStmt.setInt(5, 0);

                insertStmt.setString(6, codeCAN);
                insertStmt.setString(7, revision);
                insertStmt.setInt(8, 0);
                insertStmt.setString(9, filePath);
                insertStmt.setString(10, dernRev);
                insertStmt.setString(11, "0");
                insertStmt.setString(12, childsCAN);
                existingKeys.add(key);

                insertStmt.addBatch();

                i++;

                if (i % 20 == 0) {
                    int percentage = (int) (((float) i / maxFiles) * 100);
                    System.out.print("\rTraitement de la table Fichier en cours: " + percentage + "%");
                }

                if (i % 1000 == 0) {
                    insertStmt.executeBatch();
                    con.commit();
                }
            }

            insertStmt.executeBatch();
            con.commit();

            System.out.println("\rTraitement de la table Fichier en cours: 100%");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        }
    }

    private static String fillPDFChilds(String filePath) {
        PDFReader pdfReader = null;
        try {
            pdfReader = new PDFReader(filePath);
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

                if (isValidCodesCAN(jsonBuilder)) {
                    return jsonBuilder.toString();
                }
            }
        } catch (Exception e) {
        } finally {
            if (pdfReader != null) {
                pdfReader.close();
            }
        }

        return null;
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

    private static boolean isValidCodesCAN(StringBuilder stringBuilder) {
        String str = stringBuilder.toString();

        return (str.startsWith("[\"0") || str.startsWith("[\"AF") || str.startsWith("[\"af"))
                && Character.isLetterOrDigit(str.charAt(3))
                && str.split(",").length >= 10;
    }

    public static List<String> getDifference(List<String> listA, List<String> listB) {
        Set<String> forbiddenFiles = Set.of(
                "go.bat",
                "oli.txt",
                "thumbs.db"
        );

        Set<String> setBLowerCase = listB.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return listA.stream()
                .map(String::trim)
                .filter(e -> !forbiddenFiles.contains(e.toLowerCase()))
                .filter(e -> !setBLowerCase.contains(e.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static List<String> getFilesWithIncorrectNames(List<File> filesList) {
        Set<String> forbiddenFiles = Set.of(
                "go.bat",
                "oli.txt",
                "thumbs.db"
        );

        return filesList.stream()
                .map(File::getName)
                .map(String::trim)
                .filter(e -> !forbiddenFiles.contains(e.toLowerCase()))
                .filter(fileName -> {
                    String upperName = fileName.toUpperCase();
                    return !upperName.startsWith("AF") && !upperName.startsWith("0");
                })
                .collect(Collectors.toList());
    }

    public static List<String> getArticlesWithEmptyDescription(List<String> outLines) {
        return outLines.stream()
                .map(String::trim)
                .filter(line -> {
                    String[] parts = line.split(";", -1);
                    if (parts.length < 2) {
                        return true;
                    }
                    String description = parts[1].trim();
                    String rev = parts[2].trim();
                    return description.isEmpty() && !rev.equals("00");
                })
                .map(line -> line.split(";")[0].trim())
                .collect(Collectors.toList());
    }

    public static List<String> getArticlesWithOutdatedVersion(List<File> filesList, List<String> outLines) {
        Map<String, String> sapRevisions = new HashMap<>();
        for (String line : outLines) {
            String[] parts = line.split(";", -1);
            if (parts.length >= 3) {
                String codeCAN = parts[0].trim().toUpperCase();
                String dernRev = parts[2].trim();
                sapRevisions.put(codeCAN, dernRev);
            }
        }

        Map<String, String> fileRevisions = new HashMap<>();
        for (File file : filesList) {
            String fileName = file.getName();
            String codeCAN = fileName.split("_")[0].trim().toUpperCase();
            String revision = getRevisionFromFileName(fileName);

            if (!fileRevisions.containsKey(codeCAN)) {
                fileRevisions.put(codeCAN, revision);
            } else {
                String currentRev = fileRevisions.get(codeCAN);
                if (compareRevisions(revision, currentRev) > 0) {
                    fileRevisions.put(codeCAN, revision);
                }
            }
        }

        List<String> outdatedArticles = new ArrayList<>();
        for (Map.Entry<String, String> entry : fileRevisions.entrySet()) {
            String codeCAN = entry.getKey();
            String fileRevision = entry.getValue();
            String sapRevision = sapRevisions.get(codeCAN);

            if (sapRevision != null && compareRevisions(sapRevision, fileRevision) > 0) {
                outdatedArticles.add(codeCAN);
                outdatedArticles.add("Version SAP: " + sapRevision);
                outdatedArticles.add("Version Dossiers: " + fileRevision);
            }
        }

        return outdatedArticles;
    }

    private static String getRevisionFromFileName(String fileName) {
        try {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot == -1) {
                return "00";
            }

            String nameWithoutExtension = fileName.substring(0, lastDot);
            String[] parts = nameWithoutExtension.split("_");

            if (parts.length < 2) {
                return "00";
            }

            String revision = parts[parts.length - 1].trim();
            return revision;

        } catch (Exception e) {
            return "00";
        }
    }

    private static int compareRevisions(String rev1, String rev2) {
        try {
            int num1 = Integer.parseInt(rev1);
            int num2 = Integer.parseInt(rev2);
            return Integer.compare(num1, num2);
        } catch (NumberFormatException e) {
            return rev1.compareTo(rev2);
        }
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