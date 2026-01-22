package org.cao.backend.db;

import org.cao.backend.BackendLogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class DatabaseLogic {

    // =============== Initialisation des prérequis pour la base de données ===============

    private static final String URL = BackendLogic.readProperty("db.url")
            + ";sendStringParametersAsUnicode=false"
            + ";rewriteBatchedStatements=true";
    private static final String USER = BackendLogic.readProperty("db.user");
    private static final String PASSWORD = BackendLogic.readProperty("db.password");

    private static final int BATCH_SIZE = 1000;  // Gère la vitesse du traitement.

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
                truncateCompteursTable(statement);
                truncateTabIntTable(statement);
            }

            statement.execute("ALTER INDEX ALL ON ArticleCAN DISABLE");
            statement.execute("ALTER INDEX ALL ON Fichier DISABLE");
            statement.execute("ALTER INDEX ALL ON Compteurs DISABLE");
            statement.execute("ALTER INDEX ALL ON Activites DISABLE");
            statement.execute("ALTER INDEX ALL ON TabInt DISABLE");

            Map<String, File> fileCache = buildFileCache(BackendLogic.DIRECTORY_PATH);

            List<List<String>> allDataInOutFile = extractDatasOnOneLineInOutFile();
            List<File> allFiles = BackendLogic.getAllFilesInDirectory(Paths.get(BackendLogic.DIRECTORY_PATH));

            int maxValue = allDataInOutFile.size() + allFiles.size();
            int $$0 = 0;

            String queryArticle = "INSERT INTO ArticleCAN(CodeCAN, LP, AchFab, Statut, DescCAN, DescCANUK, UM, Matiere) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            String queryFichier = "INSERT INTO Fichier(Nom, RepertoireNom, Type, Chemin, Taille, CodeCAN, Revision, Page, Lien, DernRev, IncoRev) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String queryCompteurs = "INSERT INTO Compteurs(NbScan, NbPlan, Nb3D, NbAss, NbSchema, NbEclate, NbPFEclate, NbConfig) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
            String queryTabInt = "INSERT INTO TabInt(Type, Nom, Chemin, CodeCAN, Revision) VALUES (?, ?, ?, ?, ?);";

            try (PreparedStatement psArticle = con.prepareStatement(queryArticle);
                 PreparedStatement psFichier = con.prepareStatement(queryFichier);
                 PreparedStatement psCompteurs = con.prepareStatement(queryCompteurs);
                 PreparedStatement psTabInt = con.prepareStatement(queryTabInt)) {

                con.setAutoCommit(false);

                // ===== Table Compteurs =====

                /*
                On insère les données dans la table Compteurs ici, car il y a seulement une ligne à remplir, mettre la requête dans la
                boucle plus bas multipirais le temps de traitement.
                 */

                File mainDirectory = new File(BackendLogic.DIRECTORY_PATH);

                String scanNumber = "";
                String planNumber = "";
                String schemaNumber = "";
                String eclateNumber = "";
                String configNumber = "";

                for (File folder : mainDirectory.listFiles()) {
                    for (File subFolder : folder.listFiles()) {
                        String subFolderName = subFolder.getName();
                        int size = subFolder.listFiles() != null ? subFolder.listFiles().length : 0;

                        if (subFolderName.contains("SCAN")) scanNumber = String.valueOf(size);
                        else if (subFolderName.contains("CONFIG")) configNumber = String.valueOf(size);
                        else if (subFolderName.contains("ECLATE")) eclateNumber = String.valueOf(size);
                        else if (subFolderName.contains("ELEC")) schemaNumber = String.valueOf(size);
                        else if (subFolderName.contains("PDFS_online")) planNumber = String.valueOf(size);
                    }
                }

                psCompteurs.setString(1, scanNumber);
                psCompteurs.setString(2, planNumber);
                psCompteurs.setString(3, "");
                psCompteurs.setString(4, "");
                psCompteurs.setString(5, schemaNumber);
                psCompteurs.setString(6, eclateNumber);
                psCompteurs.setString(7, "");
                psCompteurs.setString(8, configNumber);

                psCompteurs.addBatch();
                psCompteurs.executeBatch();
                con.commit();

                for (List<String> line : allDataInOutFile) {
                    String articleCode = line.get(0);
                    String description = line.get(1);

                    File file = fileCache.get(articleCode);
                    String fileName = file != null ? file.getName() : "";
                    String filePath = file != null ? file.getAbsolutePath() : "";
                    String fileType = file != null ? convertParentToType(file.getParentFile().getName()) : "PLAN";
                    String revision = file != null ? extractRevision(file.getName()) : "";

                    // ===== Table ArticleCAN =====
                    /*
                    Regarde simplement si jamais le code article possède bien une vraie définition (c'est le même fonctionnement que
                    la base de données Access). Autrement dit, on ajoute uniquement les codes articles qui ont une définition juste et non du vide.
                    */
                    if (!Objects.equals(description, "") && !description.isEmpty()) {
                        psArticle.setString(1, articleCode);
                        psArticle.setString(2, "");
                        psArticle.setString(3, "");
                        psArticle.setString(4, "");
                        psArticle.setString(5, description);
                        psArticle.setString(6, "");
                        psArticle.setString(7, "");
                        psArticle.setString(8, "");
                        psArticle.addBatch();
                    }

                    // ===== Table TabInt =====
                    psTabInt.setString(1, fileType);
                    psTabInt.setString(2, fileName);
                    psTabInt.setString(3, filePath);
                    psTabInt.setString(4, articleCode);
                    psTabInt.setString(5, revision);
                    psTabInt.addBatch();

                    $$0++;

                    if ($$0 % BATCH_SIZE == 0) {
                        psArticle.executeBatch();
                        psTabInt.executeBatch();
                        con.commit();

                        int percentage = (int) (((float) $$0 / maxValue) * 100);
                        System.out.print("\rProgression: " + percentage + "%");
                    }
                }

                psArticle.executeBatch();
                psTabInt.executeBatch();
                con.commit();

                for (File file : allFiles) {
                    String fileName = file.getName();
                    String filePath = file.getAbsolutePath();
                    String fileType = convertParentToType(file.getParentFile().getName());

                    String revision = extractRevision(fileName);
                    String articleCode = fileName.split("_")[0];
                    String lastModification = fileName.split("_")[fileName.split("_").length - 1].split("\\.")[0];

                    // ===== Table Fichier =====
                    psFichier.setString(1, fileName);
                    psFichier.setString(2, ".");
                    psFichier.setString(3, fileType);
                    psFichier.setString(4, ".");
                    psFichier.setInt(5, 0);
                    psFichier.setString(6, articleCode);
                    psFichier.setString(7, revision);
                    psFichier.setInt(8, 0);
                    psFichier.setString(9, filePath);
                    try {
                        psFichier.setInt(10, Integer.parseInt(lastModification));
                    } catch (NumberFormatException e) {
                        psFichier.setNull(10, java.sql.Types.INTEGER);
                    }
                    psFichier.setString(11, "0");
                    psFichier.addBatch();

                    $$0++;

                    if ($$0 % BATCH_SIZE == 0) {
                        psFichier.executeBatch();
                        con.commit();

                        int percentage = (int) (((float) $$0 / maxValue) * 100);
                        System.out.print("\rProgression: " + percentage + "%");
                    }
                }

                psFichier.executeBatch();
                con.commit();

                System.out.print("\rProgression: 100%");
            }

            statement.execute("ALTER INDEX ALL ON ArticleCAN REBUILD");
            statement.execute("ALTER INDEX ALL ON Fichier REBUILD");
            statement.execute("ALTER INDEX ALL ON Compteurs REBUILD");
            statement.execute("ALTER INDEX ALL ON Activites REBUILD");
            statement.execute("ALTER INDEX ALL ON TabInt REBUILD");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TODO ATENTION CORRIGER, la DernRev c'est pas le chiffre de Revision, c'est autre chose.

    private static Map<String, File> buildFileCache(String directoryPath) {
        Map<String, File> cache = new HashMap<>();
        Path root = Paths.get(directoryPath);

        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        if (fileName.contains("_")) {
                            String articleCode = fileName.split("_")[0];
                            File existing = cache.get(articleCode);

                            if (existing == null || isNewerVersion(path.toFile(), existing)) {
                                cache.put(articleCode, path.toFile());
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cache;
    }

    private static boolean isNewerVersion(File newFile, File existingFile) {
        String newVersion = extractRevision(newFile.getName());
        String existingVersion = extractRevision(existingFile.getName());

        try {
            return Integer.parseInt(newVersion) > Integer.parseInt(existingVersion);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String extractRevision(String fileName) {
        if (fileName.contains("_")) {
            String[] parts = fileName.split("_");
            if (parts.length > 1) {
                return parts[parts.length - 1].split("\\.")[0];
            }
        }
        return "";
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
     * Méthode permettant de vider complètement les données de la table Compteurs.
     * @param statement : État de la base de données.
     * @throws SQLException : Évite les erreurs SQL.
     */
    public static void truncateCompteursTable(Statement statement) throws SQLException {
        statement.execute("TRUNCATE TABLE Compteurs");
    }

    /**
     * Méthode permettant de vider complètement les données de la table TabInt.
     * @param statement : État de la base de données.
     * @throws SQLException : Évite les erreurs SQL.
     */
    public static void truncateTabIntTable(Statement statement) throws SQLException {
        statement.execute("TRUNCATE TABLE TabInt");
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
                        (path, attr) -> attr.isRegularFile() && path.getFileName().toString().contains(articleCode))
                .parallel()) {

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
            case "PDFS_online" -> "PLAN";
            case "ECLATE_online" -> "PLAN_ECL";
            case "SCANNE_online" -> "SCAN";
            default -> "Non-renseigné";
        };
    }
}