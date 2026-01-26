package org.cao.backend.errors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ErrorBuilder {

    private String errorTitle;
    private String errorDescription;

    private String errorDate;
    private String errorHour;

    public ErrorBuilder(String errorTitle, String errorDescription) {
        this.errorTitle = errorTitle;
        this.errorDescription = errorDescription;

        this.errorDate = LocalDate.now().toString();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.errorHour = LocalTime.now().format(formatter);
    }

    public String getErrorTitle() {
        return errorTitle;
    }

    public void setErrorTitle(String errorTitle) {
        this.errorTitle = errorTitle;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorDate() {
        return errorDate;
    }

    public String getErrorHour() {
        return errorHour;
    }
}
