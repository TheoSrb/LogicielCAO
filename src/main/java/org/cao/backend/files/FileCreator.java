package org.cao.backend.files;

import org.cao.backend.db.DatabaseManager;
import org.cao.backend.errors.ErrorBuilder;
import org.cao.backend.logs.LogsBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public abstract class FileCreator {

    public static final String URL = DatabaseManager.URL;
    public static final String USER = DatabaseManager.USER;
    public static final String PASSWORD = DatabaseManager.PASSWORD;

    public static final String ARTICLES_IN_BONG_PATH = DatabaseManager.readProperty("file.in.path");
    public static final String EXCEL_RECAP_PATH = DatabaseManager.readProperty("file.excel.path");

    public static String startDateLog;
    public static String startHourLog;

    public static boolean isErrorLog = false;
    public static boolean isWarningLog = false;

    public static List<ErrorBuilder> potentialError = null;

    public abstract void createNewFile();

    public static void createLog(String task, String operation, List<ErrorBuilder> potentialError) {
        String endDateLog = String.valueOf(LocalDate.now());
        String endHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        LogsBuilder logsBuilder = new LogsBuilder(
                startDateLog,
                startHourLog,
                endDateLog,
                endHourLog,
                task,
                operation,
                isErrorLog,
                isWarningLog,
                potentialError
        );

        logsBuilder.updateLogsFile(LogsBuilder.LOGS_DIRECTORY);
    }


}
