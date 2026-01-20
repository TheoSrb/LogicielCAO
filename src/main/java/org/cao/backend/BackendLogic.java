package org.cao.backend;

import java.io.*;
import java.util.*;

public class BackendLogic {

    // =============== Chemins des fichiers et dossiers principaux ===============

    public static final String ARTICLES_OUT_BONG_PATH = readProperty("file.out.path");
    public static final String ARTICLES_IN_BONG_PATH = readProperty("file.in.path");
    public static final String DIRECTORY_PATH = readProperty("directory.path");

    /*
    Map permettant de disocier le code article de sa version (fonctionne uniquement dans le dossier PDFS_online).
     */
    private static Map<String, Integer> fileWithVersion = new HashMap<>();

    // =============== Méthodes ===============

    static void main() {
        /*
        Création des fichiers principaux.
         */
        File fileOUT = new File(ARTICLES_OUT_BONG_PATH);
        File directory = new File(DIRECTORY_PATH);

        /*
        Listes globales, et utiles pour le logiciel.
         */
        List<String> articleCodeNotUpdated = new ArrayList<>();
        List<String> newArticleCodeNotPresentInOutFile = new ArrayList<>();

        /*
        On assigne à la map chaque code article avec sa version depuis le dossier PDFS_online.
         */
        for (String fileNameStr : getAllFilesNamesInDirectory(directory)) {
            String key = cutArticleCode(fileNameStr);
            int value = cutArticleVersion(fileNameStr);

            fileWithVersion.put(key, value);
        }

        /*
        On compare la version d'un code article du dossier PDFS_online au même code article dans le fichier ArticlesOutBong.txt, on regarde si
        la version du dossier est supérieure (donc plus récente) à celle du fichier, sinon on ajoute le code article à la liste
        articleCodeNotUpdated permettant de savoir quels codes article doivent être mis à jour entre le fichier ArticlesOutBong.txt et le
        dossier PDFS_online.
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
        On affiche juste un message pour voir s'il faut mettre à jour des plans (et lesquels dans ce cas), ou non.
         */
        if (!articleCodeNotUpdated.isEmpty()) {
            System.out.println("Des fichiers doivent être mis à jour ! \n-> Les codes articles qui doivent être mit à jour sont: " + articleCodeNotUpdated);
        } else System.out.println("Tous les codes artices sont bien mis à jour !");

        /*
        On regarde et compare pour chaque code article du dossier, s'il n'existe PAS dans le fichier ArticlesOutBong.txt, si c'est le cas, on
        l'ajoute à la liste newArticleCodeNotPresentInOutFile qui servira à contenir les code article à rajouter dans le futur fichier
        ArticlesInBong.txt.
         */
        for (String fileName : getAllFilesNamesInDirectory(directory)) {
            String articleCode = fileName.toUpperCase().split("_")[0];
            List<String> allArticlesCodeInOut = new ArrayList<>();

            for (String lineOut : getAllLinesInOutFile(fileOUT)) {
                String articleCodeInOut = lineOut.toUpperCase().split(";")[0];
                allArticlesCodeInOut.add(articleCodeInOut);
            }
            if (!allArticlesCodeInOut.contains(articleCode)) {
                newArticleCodeNotPresentInOutFile.add(articleCode);
            }
        }

        /*
        On crée le fichier ArticlesInBong.txt à partir du fichier ArticlesOutBong.txt déjà existant et des nouveaux code article à ajouter
        (qu'on a ajouté plus tôt dans la liste newArticleCodeNotPresentInOutFile).
        */
        createArticleInBongFile(fileOUT, newArticleCodeNotPresentInOutFile);
    }

    /**
     * Méthode permettant de retourner une liste des noms des fichiers pdf contenus dans le dossier PDFS_online en enlevant leur extension .pdf.
     * @param directory : Le dossier dans lequel on récupère les noms des fichiers pdf.
     * @return Une liste de nom de fichiers pdf, avec à gauche son code article et à droite sa nouvelle version.
     */
    public static List<String> getAllFilesNamesInDirectory(File directory) {
        List<String> fileNames = new ArrayList<>();
        /*
        On récupère tous les fichiers contenus dans le dossier qui ont comme extension ".pdf".
         */
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (files != null) {
            for (File file : files) {
                // On ajoute le nom du fichier à la liste qu'on va retourner, sans garder l'extension ".pdf", uniquement son nom pûr.
                String fileName = file.getName();
                int index = fileName.lastIndexOf('.');
                if (index > 0) {
                    fileNames.add(fileName.substring(0, index));
                }

            }
        }

        return fileNames;
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
                lines.add(line);    // On ajoute chaque ligne lue dans la liste qu'on retournera plus tard.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lines;
    }

    /**
     * Méthode permetant d'entrer un nom de fichier .pdf (obtenu avec la méthode getAllFilesNamesInDirectory()) et d'en ressortir uniquement le
     * code article.
     * @param fileName : Le nom du fichier à découper.
     * @return Le code article du fichier.
     */
    private static String cutArticleCode(String fileName) {
        return fileName.split("_")[0];    // On prend la partie gauche avant le "_".
    }

    /**
     * Méthode permetant d'entrer un nom de fichier .pdf (obtenu avec la méthode getAllFilesNamesInDirectory()) et d'en ressortir uniquement la
     * version la plus récente du dossier PDFS_online.
     * @param fileName : Le nom du fichier à découper.
     * @return La version du fichier.
     */
    private static int cutArticleVersion(String fileName) {
        return Integer.parseInt(fileName.split("_")[1]);    // On prend la partie droite après le "_".
    }

    /**
     * Méthode permettant d'entrer un code article et d'avoir sa version depuis le fichier ArticlesOutBong.txt.
     * @param articleCode : Le code article dont on veut sa version dans le fichier ArticlesOutBong.txt.
     * @return La version du code article entré, depuis le fichier ArticlesOutBong.txt.
     */
    private static int findVersionForArticleCode(String articleCode) {
        File fileOut = new File(ARTICLES_OUT_BONG_PATH);
        int version = Integer.MAX_VALUE;

        try (Scanner scanner = new Scanner(fileOut)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                /*
                 Si la ligne qu'on regarde actuellement contient le code article dont on veut trouver sa version dans le fichier
                 ArticlesOutBong.txt, alors on l'a trouvé et on l'assigne à version qu'on retournera plus tard.
                 */
                if (line.contains(articleCode.toUpperCase())) {
                    version = Integer.parseInt(line.split(";")[2]);
                    return version;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        On retourne la valeur max de Integer si jamais il ne trouve pas le code, afin de ne pas envoyer ce même code dans la liste
        articleCodeNotUpdated et croire qu'il faut le mettre à jour alors qu'il n'existe simplement pas.
        */
        return version;
    }

    /**
     * Méthode permettant de créer un fichier (ou de le mettre à jour s'il l'est déjà) "ArticlesInBong.txt" dont son contenu est équivalent à
     * tous les codes articles déjà présents dans le fichier ArticlesOutBong.txt puis des codes articles présents dans le dossier PDFS_online
     * mais non-présents dans ce même fichier ArticlesOutBong.txt.
     * @param fileOUT : Le fichier ArticlesOutBong.txt.
     * @param newArticleCodeNotPresentInOutFile : La liste créée et remplie plus tôt qui contient les codes articles qui sont dans le dossier
     *                                          PDFS_online mais qui n'étaient pas dans le fichier ArticlesOutBong.txt.
     */
    private static void createArticleInBongFile(File fileOUT, List<String> newArticleCodeNotPresentInOutFile) {
        File fileIN = new File(ARTICLES_IN_BONG_PATH);

        if (!fileIN.exists()) {
            try {
                fileIN.createNewFile(); // On crée le fichier s'il n'existe pas déjà.
                System.out.println("Le fichier ArticlesInBong.txt a bien été créé.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else System.err.println("Le fichier ArticlesInBong.txt existe déjà, ce dernier va être mis à jour.");
        try {
            FileWriter writer = new FileWriter(fileIN);
            BufferedWriter bw = new BufferedWriter(writer);

                /*
                On récupère tous les codes articles du fichier ArticlesOutBong.txt puis on les réécrit dans le nouveau fichier
                ArticlesInBong.txt.
                 */
            for (String line : getAllLinesInOutFile(fileOUT)) {
                String articleCode = line.split(";")[0];
                bw.write(articleCode + "\n");
            }

                /*
                On réécrit tous les codes articles dans le nouveau fichier ArticlesInBong.txt.
                 */
            for (String articleCode : newArticleCodeNotPresentInOutFile) {
                bw.write(articleCode + "\n");
            }

            bw.close();     // On ferme tout pour éviter d'avoir des problèmes de mémoire.
            writer.close();

            System.out.println("Le fichier ArticlesInBong.txt a bien été mis à jour.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Méthode permettant de récupérer la valeur d'une propriété donnée en entrée, dans le fichier config.properties du projet.
     * @param property : La valeur de la propriété qu'on veut obtenir.
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
}
