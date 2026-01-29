package org.cao.backend.errors;

public class ErrorRegistry {

    public static final ErrorBuilder ARTICLES_NOT_IN_SAP = new ErrorBuilder(
            "Codes articles non synchronisés",
            "Des codes articles venus de SAP n'ont pas été trouvés dans les dossiers des fichiers PDF."
    );

    public static final ErrorBuilder FILE_NAME_ERROR = new ErrorBuilder(
            "",
            ""
    );

    public static final ErrorBuilder REVISION_ERROR = new ErrorBuilder(
            "",
            ""
    );

    public static final ErrorBuilder ARTICLES_WITHOUT_DESCRIPTION = new ErrorBuilder(
            "",
            ""
    );

}
