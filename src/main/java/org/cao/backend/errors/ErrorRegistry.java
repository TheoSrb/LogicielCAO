package org.cao.backend.errors;

import java.util.ArrayList;
import java.util.List;

public class ErrorRegistry {

    public static final ErrorBuilder ARTICLES_NOT_IN_SAP = new ErrorBuilder(
            "Articles non synchronisés",
            "Des codes articles ne sont pas dans SAP mais sont dans les dossiers des fichiers PDF.",
            new ArrayList<>()
    );

    public static final ErrorBuilder ARTICLES_NOT_IN_FOLDERS = new ErrorBuilder(
            "Articles non synchronisés",
            "Des codes articles sont dans SAP mais ne sont pas des les dossiers des fichiers PDF.",
            new ArrayList<>()
    );

    // TODO ici, facile à voir, si un fichier ne commence pas par AF ou af ou Af ou aF alors le mettre dans une liste et déclarer l'erreur
    public static final ErrorBuilder FILE_NAME_ERROR = new ErrorBuilder(
            "Nom d'articles incorrects",
            "Des codes articles ont un nom incorrect.",
            new ArrayList<>()
    );

    // TODO ici, vérifier si pour chaque fichier, sa dernière rev est bien celle qui est dans OUT, si elle est inférieure seulement, erreur.
    public static final ErrorBuilder REVISION_ERROR = new ErrorBuilder(
            "",
            "",
            new ArrayList<>()
    );

    // TODO ici, simple, juste vérifier si jamais le split[1] de chaque ligne de OUT n'est pas égal à "" ou pas empty, sinon, erreur.
    public static final ErrorBuilder ARTICLES_WITHOUT_DESCRIPTION = new ErrorBuilder(
            "Articles sans description",
            "Des codes articles ne possèdent pas de description.",
            new ArrayList<>()
    );

}
