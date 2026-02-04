package org.cao.backend.errors;

import java.util.ArrayList;
import java.util.List;

public class ErrorRegistry {

    public static final ErrorBuilder ARTICLES_NOT_IN_SAP = new ErrorBuilder(
            "Articles non synchronisés",
            "Des codes articles ne sont pas dans SAP mais sont dans les dossiers des fichiers PDF.",
            new ArrayList<>(),
            false
    );

    public static final ErrorBuilder ARTICLES_NOT_IN_FOLDERS = new ErrorBuilder(
            "Articles non synchronisés",
            "Des codes articles sont dans SAP mais ne sont pas des les dossiers des fichiers PDF.",
            new ArrayList<>(),
            false
    );

    public static final ErrorBuilder FILE_NAME_ERROR = new ErrorBuilder(
            "Nom d'articles incorrects",
            "Des codes articles ont un nom incorrect (ne commencent pas par \"AF\").",
            new ArrayList<>(),
            false
    );

    public static final ErrorBuilder REVISION_ERROR = new ErrorBuilder(
            "Revision incorrecte",
            "Des articles dans les dossiers des fichiers PDF ont une version inférieure aux mêmes codes dans SAP.",
            new ArrayList<>(),
            false
    );

    public static final ErrorBuilder ARTICLES_WITHOUT_DESCRIPTION = new ErrorBuilder(
            "Articles sans description",
            "Des codes articles ne possèdent pas de description.",
            new ArrayList<>(),
            true
    );

}
