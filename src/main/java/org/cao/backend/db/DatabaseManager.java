package org.cao.backend.db;

import org.cao.backend.BackendLogic;
import org.cao.backend.FileHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private static final String URL = readProperty("db.url");
    private static final String USER = readProperty("db.user");
    private static final String PASSWORD = readProperty("db.password");

    public static final String ARTICLES_OUT_BONG_PATH = readProperty("file.out.path");
    public static final String ARTICLES_IN_BONG_PATH = readProperty("file.in.path");
    public static final String DIRECTORY_PATH = readProperty("directory.path");

    public static final List<String> AUTHORIZED_FOLDERS_NAMES = separateAllAuthorizedFolders(readProperty("authorized.explore.folders"));

    static void main() {
        updateDatabase();
    }

    private static void updateDatabase() {
        startConnectionWithDatabase();

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD); Statement statement = con.createStatement()) {

            //fillFichierTable(con, statement);
            fillArticleCANTable(con, statement);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void startConnectionWithDatabase() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void fillArticleCANTable(Connection con, Statement statement) throws SQLException {
        File fileOut = new File(ARTICLES_OUT_BONG_PATH);
        FileHelper fileHelper = new FileHelper(fileOut);

        List<String> lines = fileHelper.readAllLines();
        int i = 0;
        int maxLines = lines.size();

        for (String line : lines) {
            String codeCAN = line.split(";")[0];
            String descCAN = line.split(";")[1];
            String query;

            if (descCAN.isEmpty() || descCAN.equals(" ")) continue;

            boolean alreadyInserted = codeCANAlreadyExist(con, codeCAN);

            if (alreadyInserted) {
                query = "UPDATE ArticleCAN SET DescCAN = ? WHERE CodeCAN = ?";
            } else {
                query = "INSERT INTO ArticleCAN(CodeCAN, LP, AchFab, Statut, DescCAN, DescCANUK, UM, Matiere) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            }

            try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
                if (alreadyInserted) {
                    preparedStatement.setString(1, descCAN);
                    preparedStatement.setString(2, codeCAN);
                } else {
                    preparedStatement.setString(1, codeCAN);
                    preparedStatement.setString(2, ".");
                    preparedStatement.setString(3, ".");
                    preparedStatement.setString(4, ".");
                    preparedStatement.setString(5, descCAN);
                    preparedStatement.setString(6, ".");
                    preparedStatement.setString(7, ".");
                    preparedStatement.setString(8, ".");
                }

                preparedStatement.execute();
            }

            i++;
            int percentage = (int) (((float) i / maxLines) * 100);
            System.out.print("\rProgression: " + percentage + "%");
        }
    }

    private static void fillFichierTable(Connection con, Statement statement) throws SQLException {
        File mainDirectory = new File(BackendLogic.DIRECTORY_PATH);
        List<File> files = returnFilesInDirectory(mainDirectory);
        int i = 0;
        int maxFiles = files.size();

        for (File file : files) {
            String fileName = file.getName();
            String fileType = convertParentToType(file.getParentFile().getName());
            String codeCAN = getCodeCAN(fileName);
            String revision = getRevision(fileName);
            String filePath = file.getAbsolutePath();

            String checkQuery = "SELECT COUNT(*) FROM Fichier WHERE CodeCAN = ? AND Revision = ?";
            boolean exists = false;

            try (PreparedStatement checkStmt = con.prepareStatement(checkQuery)) {
                checkStmt.setString(1, codeCAN);
                checkStmt.setString(2, revision);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    exists = true;
                }
            }

            String query;
            if (exists) {
                query = "UPDATE Fichier SET Nom = ?, RepertoireNom = ?, Type = ?, Chemin = ?, Taille = ?, Page = ?, Lien = ?, DernRev = ?, IncoRev = ?, CodeCAN_Parent = ?, Revision_Parent = ? WHERE CodeCAN = ? AND Revision = ?";
            } else {
                query = "INSERT INTO Fichier(Nom, RepertoireNom, Type, Chemin, Taille, CodeCAN, Revision, Page, Lien, DernRev, IncoRev, CodeCAN_Parent, Revision_Parent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }

            try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
                preparedStatement.setString(1, fileName);
                preparedStatement.setString(2, ".");
                preparedStatement.setString(3, fileType);
                preparedStatement.setString(4, ".");
                preparedStatement.setInt(5, 0);

                if (exists) {
                    preparedStatement.setInt(6, 0);
                    preparedStatement.setString(7, filePath);
                    preparedStatement.setString(8, "0");
                    preparedStatement.setString(9, "0");
                    preparedStatement.setString(10, ".");
                    preparedStatement.setString(11, ".");
                    preparedStatement.setString(12, codeCAN);
                    preparedStatement.setString(13, revision);
                } else {
                    preparedStatement.setString(6, codeCAN);
                    preparedStatement.setString(7, revision);
                    preparedStatement.setInt(8, 0);
                    preparedStatement.setString(9, filePath);
                    preparedStatement.setString(10, "0");
                    preparedStatement.setString(11, "0");
                    preparedStatement.setString(12, ".");
                    preparedStatement.setString(13, ".");
                }

                preparedStatement.execute();
            }

            i++;
            int percentage = (int) (((float) i / maxFiles) * 100);
            System.out.print("\rProgression: " + percentage + "%");
        }
    }

    private static boolean codeCANAlreadyExist(Connection con, String codeCAN) throws SQLException {
        String sql = "SELECT 1 FROM ArticleCAN WHERE CodeCAN = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codeCAN);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
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
        List<String> foldersNameAuthorized = BackendLogic.AUTHORIZED_FOLDERS_NAMES;
        String folderName = file.getName();
        return foldersNameAuthorized.contains(folderName);
    }

    public static String convertParentToType(String parentName) {
        return switch(parentName) {
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

        try (InputStream fis = BackendLogic.class.getClassLoader().getResourceAsStream("config.properties")) {
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
