package org.cao.backend.logs;

import org.cao.backend.BackendLogic;
import org.cao.backend.errors.ErrorBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogsBuilder {

    public static final String LOGS_DIRECTORY = BackendLogic.readProperty("file.logs");

    // =============== Attributs ===============

    private String startDate;
    private String startHour;
    private String endDate;
    private String endHour;
    private String task;
    private String operation;
    private boolean isError;
    private boolean isWarning;

    private ErrorBuilder errorBuilder;

    // =============== Constructeurs ===============

    public LogsBuilder(String startDate, String startHour, String endDate, String endHour, String task, String operation, boolean isError, boolean isWarning) {
        this.startDate = startDate;
        this.startHour = startHour;
        this.endDate = endDate;
        this.endHour = endHour;
        this.task = task;
        this.operation = operation;
        this.isError = isError;
        this.isWarning = isWarning;
    }

    public LogsBuilder(String startDate, String startHour, String endDate, String endHour, String task, String operation, boolean isError, boolean isWarning, ErrorBuilder errorBuilder) {
        this.startDate = startDate;
        this.startHour = startHour;
        this.endDate = endDate;
        this.endHour = endHour;
        this.task = task;
        this.operation = operation;
        this.isError = isError;
        this.isWarning = isWarning;

        this.errorBuilder = errorBuilder;
    }

    // =============== Méthode ===============

    public void updateLogsFile(String mainFolderPath) {
        File directory = new File(mainFolderPath);

        if (!directory.exists()) {
            try {
                directory.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

        String logsFileName = LocalDateTime.now().format(formatter) + "_CAO.txt";
        File logsFile = new File(mainFolderPath, logsFileName);

        // On crée le fichier s'il n'existe pas déjà.
        if (!logsFile.exists()) {
            try {
                logsFile.createNewFile();
                System.out.println("\nFichier de log créé avec succès !");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fillLogFile(logsFile);
    }

    public void fillLogFile(File logFile) {
        try (FileWriter writer = new FileWriter(logFile);
             BufferedWriter bw = new BufferedWriter(writer)) {

            String startDate = this.startDate;
            String startHour = this.startHour;
            String endDate = this.endDate;
            String endHour = this.endHour;
            String task = this.task;
            String operation = this.operation;
            boolean isError = this.isError;
            boolean isWarning = this.isWarning;

            ErrorBuilder errorBuilder = this.errorBuilder;

            bw.write(
                    startDate + ";"
                        + startHour + ";"
                        + endDate + ";"
                        + endHour + ";"
                        + task + ";"
                        + operation + ";"
                        + isError + ";"
                        + isWarning
            );

            if (errorBuilder != null) {
                bw.write("\n\n[ERROR] " + "[" + errorBuilder.getErrorDate() + "_" + errorBuilder.getErrorHour() + "] " + errorBuilder.getErrorDescription());
            }

            bw.close();
            writer.close();

            System.out.println("Fichier rempli avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =============== Accesseurs ===============

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStartHour() {
        return startHour;
    }

    public void setStartHour(String startHour) {
        this.startHour = startHour;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getEndHour() {
        return endHour;
    }

    public void setEndHour(String endHour) {
        this.endHour = endHour;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public void setWarning(boolean warning) {
        isWarning = warning;
    }
}
