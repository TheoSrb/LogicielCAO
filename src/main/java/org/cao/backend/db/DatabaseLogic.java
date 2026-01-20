package org.cao.backend.db;

import org.cao.backend.BackendLogic;
import org.cao.backend.exception.SQLDataAlreadyExistInTable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseLogic {

    // =============== Initialisation des prérequis pour la base de données ===============

    private static final String URL = BackendLogic.readProperty("db.url");
    private static final String USER = BackendLogic.readProperty("db.user");
    private static final String PASSWORD = BackendLogic.readProperty("db.password");

    static void main() {

        updateDatabase(true);

    }

    /**
     * Méthode qui permet d'actualiser la base de données afin d'y intégrer des données.
     * @param truncate : Booléen permettant de vider la base de données avant d'y insérer les données, utile pour les tests.
     */
    public static void updateDatabase(boolean truncate) {

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement statement = con.createStatement()) {

            if (truncate) {
                truncateArticleCANTable(statement);
                truncateFichierTable(statement);
            }

            List<List<String>> allData = extractDatasOnOneLineInOutFile();
            int maxValue = allData.size();
            int $$0 = 0;

            String queryArticle = "INSERT INTO ArticleCAN(CodeCAN, LP, AchFab, Statut, DescCAN, DescCANUK, UM, Matiere) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            String queryFichier = "INSERT INTO Fichier(Nom, RepertoireNom, Type, Chemin, Taille, CodeCAN, Revision, Page, Lien, DernRev, IncoRev) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement psArticle = con.prepareStatement(queryArticle);
                 PreparedStatement psFichier = con.prepareStatement(queryFichier)) {

                con.setAutoCommit(false);

                for (List<String> line : allData) {
                    String articleCode = line.get(0);
                    String description = line.get(1);
                    String lastModification = line.get(2);

                    File file = findFileAndItLastVersionInDirectory(articleCode);
                    String fileName = file != null ? file.getName() : "";
                    String filePath = file != null ? file.getAbsolutePath() : "";
                    String fileType = file != null ? convertParentToType(file.getParentFile().getName()) : "";

                    psArticle.setString(1, articleCode);
                    psArticle.setString(2, "");
                    psArticle.setString(3, "");
                    psArticle.setString(4, "");
                    psArticle.setString(5, description);
                    psArticle.setString(6, "");
                    psArticle.setString(7, "");
                    psArticle.setString(8, "");
                    psArticle.addBatch();

                    psFichier.setString(1, fileName);
                    psFichier.setString(2, "");
                    psFichier.setString(3, fileType);
                    psFichier.setString(4, filePath);
                    psFichier.setInt(5, 0);
                    psFichier.setString(6, articleCode);
                    psFichier.setString(7, "");
                    psFichier.setInt(8, 0);
                    psFichier.setString(9, "");
                    psFichier.setInt(10, Integer.parseInt(lastModification));
                    psFichier.setString(11, "");
                    psFichier.addBatch();

                    $$0++;

                    psArticle.executeBatch();
                    psFichier.executeBatch();
                    con.commit();

                    int percentage = (int) (((float) $$0 / maxValue) * 100);
                    System.out.print("\rProgression: " + percentage + "%");
                }

                psArticle.executeBatch();
                psFichier.executeBatch();
                con.commit();

                System.out.print("\rProgression: 100%\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Méthode qui permet de voir si une donnée est déjà dans une table.
     * @param table : La table de la base de données, à vérifier.
     * @param columnName : La colonne de la table.
     * @param data : La donnée qu'on veut vérifier.
     * @return Le résultat de si une donnée cherchée est dans une table de la base de données ou non.
     */
    public static boolean dataIsAlreadyExist(String table, String columnName, String data) {
        String query = "SELECT " + columnName + " FROM " + table + " WHERE " + columnName + " = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD)) {
            try (PreparedStatement preparedStatement = con.prepareStatement(query)) {
                preparedStatement.setString(1, data);

                ResultSet resultSet = preparedStatement.executeQuery();
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Méthode permettant de retourner une liste qui contient pleins de petite liste qui symbolisent chaques lignes du fichier
     * ArticlesOutBong.txt afin de récupérer chaques données plus facilement (code article, description, version).
     * @return Une liste contenant des listes correspondant aux lignes du fichier ArticlesOutBong.txt.
     */
    public static List<List<String>> extractDatasOnOneLineInOutFile() {
        File fileOut = new File(BackendLogic.ARTICLES_OUT_BONG_PATH);
        List<List<String>> mainList = new ArrayList<>();
        for (String line : BackendLogic.getAllLinesInOutFile(fileOut)) {
            List<String> childList = new ArrayList<>();

            String articleCode = line.split(";")[0];
            String description = line.split(";")[1];
            String lastModification = line.split(";")[2];

            childList.add(articleCode);
            childList.add(description);
            childList.add(lastModification);

            mainList.add(childList);
        }
        return mainList;
    }

    /**
     * Méthode permettant de vider complètement les données de la table ArticleCAN.
     * @param statement : État de la base de données.
     * @throws SQLException : Évite les erreurs SQL.
     */
    public static void truncateArticleCANTable(Statement statement) throws SQLException {
        statement.execute("TRUNCATE TABLE ArticleCAN");
    }

    /**
     * Méthode permettant de vider complètement les données de la table Fichier.
     * @param statement : État de la base de données.
     * @throws SQLException : Évite les erreurs SQL.
     */
    public static void truncateFichierTable(Statement statement) throws SQLException {
        statement.execute("TRUNCATE TABLE Fichier");
    }

    /**
     * Méthode permettant de retouver un fichier avec sa dernière version dans cao2016 à partir de son code article.
     * @param root : Le chemin du dossier main.
     * @param articleCode : Le code article dont on veut le fichier.
     * @return La dernière version du fichier correspondant au code article donné en entrée.
     * @throws IOException : On évite les erreurs.
     */
    public static File findFileWithLastVersion(Path root, String articleCode) throws IOException {
        File bestFile = null;
        int bestVersion = -1;

        try (var stream = Files.find(root, Integer.MAX_VALUE,
                (path, attr) -> attr.isRegularFile() && path.getFileName().toString().contains(articleCode))) {

            for (Path path : (Iterable<Path>) stream::iterator) {
                File file = path.toFile();
                String name = file.getName();

                if (name.contains("_")) {
                    String afterUnderscore = name.split("_")[1];
                    String versionStr = afterUnderscore.split("\\.")[0];

                    int version = Integer.parseInt(versionStr);

                    if (version > bestVersion) {
                        bestVersion = version;
                        bestFile = file;
                    }
                }
            }
        }

        return bestFile;
    }

    /**
     * Méthode permettant de retouver toutes les versions d'un fichier dans cao2016 grâce à son code article.
     * @param root : Le chemin du dossier main.
     * @param articleCode : Le code article dont on veut le fichier.
     * @return Une liste de tous les fichiers correspondant au code article.
     * @throws IOException : On évite les erreurs.
     */
    public static List<File> findAllFileVersions(Path root, String articleCode) throws IOException {
        List<File> files = new ArrayList<>();

        try (var stream = Files.find(
                root,
                Integer.MAX_VALUE,
                (path, attr) -> attr.isRegularFile()
                        && path.getFileName().toString().contains(articleCode)
        )) {

            for (Path path : (Iterable<Path>) stream::iterator) {
                String name = path.getFileName().toString();

                if (name.contains("_")) {
                    String afterUnderscore = name.split("_")[1];
                    String versionStr = afterUnderscore.split("\\.")[0];

                    try {
                        Integer.parseInt(versionStr);
                        files.add(path.toFile());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return files;
    }

    /**
     * Méthode appliquant findFileWithLastVersion() pour récupérer le fichier le plus récent correspondant au code article.
     * @param articleCode : Le code article recherché.
     * @return Le plus récent correspondant au code article.
     */
    public static File findFileAndItLastVersionInDirectory(String articleCode) {
        Path root = Paths.get(BackendLogic.DIRECTORY_PATH);
        try {
            return findFileWithLastVersion(root, articleCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Méthode appliquant findAllFileVersions() pour récupérer tous les fichiers correspondants au code article.
     * @param articleCode : Le code article recherché.
     * @return Le plus récent correspondant au code article.
     */
    public static List<File> findFileAndAllVersionsInDirectory(String articleCode) {
        Path root = Paths.get(BackendLogic.DIRECTORY_PATH);
        try {
            return findAllFileVersions(root, articleCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Méthode permettant de convertir le nom du dossier parent d'un fichier en une catégorie plus lisible.
     * @param parentName : Nom du dossier parent du fichier .pdf trouvé.
     * @return Une catégorie de fichier.
     */
    public static String convertParentToType(String parentName) {
        return switch(parentName) {
            case "ELEC_online" -> "ELEC";
            case "CONFIG_online" -> "CONFIG";
            case "PLAN_online" -> "PLAN";
            case "ECLATE_online" -> "PLAN_ECL";
            case "SCANNE_online" -> "SCAN";
            default -> "Non-renseigné";
        };
    }
}