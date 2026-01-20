package org.cao.backend.exception;

public class SQLDataAlreadyExistInTable extends RuntimeException {

    public SQLDataAlreadyExistInTable() {
        super("La donnée que vous essayez de rentrer existe déjà dans la base de données.");
    }
}
