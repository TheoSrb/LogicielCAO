package org.cao.backend.errors;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ErrorBuilder {

    private String errorTitle;
    private String errorDescription;
    private List<String> articlesConcerned;

    private String errorDate;
    private String errorHour;

    public ErrorBuilder(String errorTitle, String errorDescription, List<String> articlesConcerned) {
        this.errorTitle = errorTitle;
        this.errorDescription = errorDescription;
        this.articlesConcerned = articlesConcerned;

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

    public List<String> getArticlesConcerned() {
        return articlesConcerned;
    }

    public void setArticlesConcerned(List<String> articlesConcerned) {
        this.articlesConcerned = articlesConcerned;
    }
}
