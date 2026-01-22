package org.cao.backend;

import org.cao.backend.logs.LogsBuilder;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BackendLogic {

    // =============== Chemins des fichiers et dossiers principaux ===============

    public static final String ARTICLES_OUT_BONG_PATH = readProperty("file.out.path");
    public static final String ARTICLES_IN_BONG_PATH = readProperty("file.in.path");
    public static final String DIRECTORY_PATH = readProperty("directory.path");

    public static final List<String> AUTHORIZED_FOLDERS_NAMES = separateAllAuthorizedFolders(readProperty("authorized.explore.folders"));

    /**
     * Map permettant de dissocier le code article de sa version.
     */
    private static Map<String, Integer> fileWithVersion = new ConcurrentHashMap<>();

    // =============== Méthodes ===============

    static void main() {
        //register();

        System.out.println(isValidPath(Path.of("C:\\Users\\theosarbachfischer\\Desktop\\cao2016\\publi_web\\PDFS_online\\TODEL")));
    }

    private static void register() {
        String startDateLog = String.valueOf(LocalDate.now());
        String startHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));

        /*
        Création des fichiers principaux.
         */
        File fileOUT = new File(ARTICLES_OUT_BONG_PATH);
        Path directory = Paths.get(DIRECTORY_PATH);

        /*
        Listes globales et utiles pour le logiciel.
         */
        List<String> articleCodeNotUpdated = new ArrayList<>();
        List<String> newArticleCodeNotPresentInOutFile = new ArrayList<>();

        /*
        On assigne à la map chaque code article avec sa version depuis le dossier principal.
         */
        List<String> allFileNames = getAllFilesNamesInDirectory(directory);

        for (String fileNameStr : allFileNames) {
            String key = cutArticleCode(fileNameStr);
            int value = Integer.MAX_VALUE;
            try {
                value = cutArticleVersion(fileNameStr);
            } catch (NumberFormatException e) {

            }

            fileWithVersion.put(key, value);
        }

        /*
        On compare la version d'un code article du dossier principal au même code article dans le fichier ArticlesOutBong.txt.
        Si la version du dossier est supérieure (donc plus récente) à celle du fichier, on ajoute le code article à la liste
        articleCodeNotUpdated.
         */

        for (Map.Entry<String, Integer> entry : fileWithVersion.entrySet()) {
            String codeArticle = entry.getKey();

            int lastVersion = entry.getValue();
            int actualVersion = findVersionForArticleCode(codeArticle);

            if (lastVersion > actualVersion) {
                articleCodeNotUpdated.add(codeArticle);
            }
        }

        /*
        Affichage d'un message pour voir s'il faut mettre à jour des plans (et lesquels dans ce cas), ou non.
         */
        if (!articleCodeNotUpdated.isEmpty()) {
            System.out.println("Des fichiers doivent être mis à jour ! \n   -> Les codes articles qui doivent être mis à jour sont: " + articleCodeNotUpdated);
        } else {
            System.out.println("Tous les codes articles sont bien mis à jour !");
        }

        /*
        On regarde et compare pour chaque code article du dossier, s'il n'existe PAS dans le fichier ArticlesOutBong.txt.
        Si c'est le cas, on l'ajoute à la liste newArticleCodeNotPresentInOutFile.
         */

        // On charge UNE SEULE FOIS tous les codes articles du fichier OUT dans un Set pour des recherches rapides
        Set<String> allArticlesCodeInOut = new HashSet<>();
        for (String lineOut : getAllLinesInOutFile(fileOUT)) {
            String articleCodeInOut = lineOut.toUpperCase().split(";")[0];
            allArticlesCodeInOut.add(articleCodeInOut);
        }

        // On vérifie chaque fichier par rapport au Set (recherche en O(1) au lieu de O(n))
        Set<String> uniqueNewCodes = new HashSet<>();
        for (String fileName : allFileNames) {
            String articleCode = fileName.toUpperCase().split("_")[0];
            if (!allArticlesCodeInOut.contains(articleCode)) {
                uniqueNewCodes.add(articleCode);
            }
        }
        newArticleCodeNotPresentInOutFile.addAll(uniqueNewCodes);

        /*
        Création du fichier ArticlesInBong.txt à partir du fichier ArticlesOutBong.txt déjà existant et des nouveaux codes articles.
        */
        createArticleInBongFile(fileOUT, newArticleCodeNotPresentInOutFile, startDateLog, startHourLog);
    }

    /**
     * Méthode permettant de retourner une liste des noms des fichiers PDF contenus dans l'arborescence
     * du dossier principal (publi_web avec ses sous-dossiers et plan_be/SCANNE_online).
     * @param mainDirectory : Le dossier principal contenant publi_web et plan_be
     * @return Une liste de noms de fichiers PDF sans extension
     */
    public static List<String> getAllFilesNamesInDirectory(Path mainDirectory) {
        try (var stream = Files.walk(mainDirectory, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
                .filter(path -> {
                    if (Files.isDirectory(path)) {
                        return isAuthorizedDirectory(path);
                    }
                    return true;
                })
                .parallel()
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                .filter(path -> isValidPath(path))) {

            return stream
                    .map(path -> {
                        String fileName = path.getFileName().toString();
                        int index = fileName.lastIndexOf('.');
                        return index > 0 ? fileName.substring(0, index) : fileName;
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Méthode permettant de retourner une liste des fichiers PDF contenus dans l'arborescence
     * du dossier principal (publi_web avec ses sous-dossiers et plan_be/SCANNE_online)
     * @param mainDirectory Le dossier principal contenant publi_web et plan_be
     * @return Une liste d'objets File correspondant aux fichiers PDF
     */
    public static List<File> getAllFilesInDirectory(Path mainDirectory) {
        try (var stream = Files.walk(mainDirectory, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS)
                .filter(path -> {
                    if (Files.isDirectory(path)) {
                        return isAuthorizedDirectory(path);
                    }
                    return true;
                })
                .parallel()
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                .filter(path -> isValidPath(path))) {

            return stream
                    .map(Path::toFile)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Méthode permettant de vérifier si un dossier est autorisé à être exploré.
     * @param directory : Le dossier à vérifier.
     * @return Un booléen indiquant si le dossier peut être exploré.
     */
    private static boolean isAuthorizedDirectory(Path directory) {
        File directoryFile = new File(directory.toUri());
        return directory.equals(directory.getRoot()) || AUTHORIZED_FOLDERS_NAMES.contains(directoryFile.getName());
    }

    /**
     * Méthode permettant de vérifier si le chemin contient un dossier autorisé.
     * @param path : Le chemin à vérifier.
     * @return Un booléen indiquant si le chemin est autorisé.
     */
    private static boolean isValidPath(Path path) {
        String pathString = path.toString();
        // On ne veut pas vérifier ces dossiers dans PDFS_online
        if (pathString.contains("TODEL") || pathString.contains("Pochoirs")) return false;
        return AUTHORIZED_FOLDERS_NAMES.stream().anyMatch(pathString::contains);
    }

    /**
     * Méthode permettant de retourner une liste de toutes les lignes dans le fichier ArticlesOutBong.txt.
     * @param fileOut : Le fichier ArticlesOutBong.txt.
     * @return Une liste de toutes les lignes.
     */
    public static List<String> getAllLinesInOutFile(File fileOut) {
        List<String> lines = new ArrayList<>();

        try (Scanner scanner = new Scanner(fileOut)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lines;
    }

    /**
     * Méthode permettant d'entrer un nom de fichier .pdf et d'en ressortir uniquement le code article.
     * @param fileName : Le nom du fichier à découper.
     * @return Le code article du fichier.
     */
    private static String cutArticleCode(String fileName) {
        return fileName.split("_")[fileName.split("_").length - 1];
    }

    /**
     * Méthode permettant d'entrer un nom de fichier .pdf et d'en ressortir uniquement la version.
     * @param fileName : Le nom du fichier à découper.
     * @return La version du fichier.
     */
    private static int cutArticleVersion(String fileName) {
        return Integer.parseInt(fileName.split("_")[fileName.split("_").length - 1]);
    }

    /**
     * Méthode permettant d'entrer un code article et d'avoir sa version depuis le fichier ArticlesOutBong.txt.
     * @param articleCode : Le code article dont on veut la version dans le fichier ArticlesOutBong.txt.
     * @return La version du code article entré, depuis le fichier ArticlesOutBong.txt.
     */
    private static int findVersionForArticleCode(String articleCode) {
        File fileOut = new File(ARTICLES_OUT_BONG_PATH);
        int version = Integer.MAX_VALUE;

        try (Scanner scanner = new Scanner(fileOut)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.contains(articleCode.toUpperCase())) {
                    version = Integer.parseInt(line.split(";")[2]);
                    return version;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        Retourne la valeur max de Integer si le code n'est pas trouvé, afin de ne pas l'ajouter
        dans la liste articleCodeNotUpdated alors qu'il n'existe simplement pas.
        */
        return version;
    }

    /**
     * Méthode permettant de créer un fichier (ou de le mettre à jour s'il existe déjà) "ArticlesInBong.txt".
     * Son contenu est équivalent à tous les codes articles présents dans ArticlesOutBong.txt
     * plus les codes articles présents dans le dossier principal, mais absents du fichier ArticlesOutBong.txt.
     * @param fileOUT : Le fichier ArticlesOutBong.txt.
     * @param newArticleCodeNotPresentInOutFile : La liste des codes articles dans le dossier principal, mais absents du
     *                                          fichier ArticlesOutBong.txt.
     */
    private static void createArticleInBongFile(File fileOUT, List<String> newArticleCodeNotPresentInOutFile, String startDateLog, String startHourLog) {
        File fileIN = new File(ARTICLES_IN_BONG_PATH);

        if (!fileIN.exists()) {
            try {
                fileIN.createNewFile();
                System.out.println("Le fichier ArticlesInBong.txt a bien été créé.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Le fichier ArticlesInBong.txt existe déjà, ce dernier va être mis à jour.");
        }

        try (FileWriter writer = new FileWriter(fileIN);
             BufferedWriter bw = new BufferedWriter(writer)) {

            List<String> allLinesOut = getAllLinesInOutFile(fileOUT);

            /*
            Récupération de tous les codes articles du fichier ArticlesOutBong.txt
            puis réécriture dans le nouveau fichier ArticlesInBong.txt.
             */
            for (String line : allLinesOut) {
                String articleCode = line.split(";")[0];
                bw.write(articleCode + "\n");
            }

            /*
            Écriture de tous les nouveaux codes articles dans le fichier ArticlesInBong.txt.
             */
            for (String articleCode : newArticleCodeNotPresentInOutFile) {
                bw.write(articleCode + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String endDateLog = String.valueOf(LocalDate.now());
        String endHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));

        LogsBuilder logsBuilder = new LogsBuilder(
                startDateLog,
                startHourLog,
                endDateLog,
                endHourLog,
                "[Tâche]",
                "[Opération]",
                false,
                false
        );

        logsBuilder.updateLogsFile(LogsBuilder.LOGS_DIRECTORY);
    }

    /**
     * Méthode permettant de récupérer la valeur d'une propriété donnée en entrée,
     * dans le fichier config.properties du projet.
     * @param property : La propriété qu'on veut obtenir.
     * @return La valeur de la propriété donnée en entrée.
     */
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

    /**
     * Méthode permettant de séparer les éléments dans config.properties pour en obtenir une liste.
     * @param filesName : La propriété de config.properties.
     * @return Une liste des dossiers pouvant être explorés.
     */
    private static List<String> separateAllAuthorizedFolders(String filesName) {
        return List.of(filesName.split(";"));
    }
}