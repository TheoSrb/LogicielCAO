package org.cao.backend.files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PDFReader {

    // =============== Attributs ===============

    private String filePath;

    // =============== Constructeur ===============

    public PDFReader(String filePath) {
        this.filePath = filePath;
    }

    // =============== Méthodes ===============

    public List<String> readLinesInTable() {
        List<String> result = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(filePath))) {

            if (document.isEncrypted()) return null;

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            String[] lines = text.split("\n");

            for (String line : lines) {
                if (line.contains("V.LANGE")) continue;
                if (line.length() >= 5 && line.startsWith("0")) {
                    if (Character.isDigit(line.charAt(1)) && Character.isDigit(line.charAt(2)) && line.charAt(3) == ' ' &&
                            Character.isDigit(line.charAt(4))) {
                        result.add(line);
                    }
                }
            }

            for (String line : lines) {
                if (line.contains("Code:")) {
                    result.add(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<String> getCodeCANInTable() {
        List<String> codes = new ArrayList<>();
        List<String> lines = readLinesInTable();

        for (String line : lines) {
            if (line.contains("V.LANGE")) continue;
            codes.add(line.split(" ")[2]);
        }

        for (String line : lines) {
            if (line.contains("Code:")) {
                String code = line.split("Code:")[1].trim().split("\\s+")[0];
                codes.add(code);
                break;
            }
        }

        return codes;
    }

    public List<String> getChilds() throws Exception {
        List<String> codes = getCodeCANInTable();
        codes.removeLast();
        return codes;
    }

    public String getParent() throws Exception {
        List<String> codes = getCodeCANInTable();
        return codes.getLast();
    }

    // =============== Accesseurs ===============

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}