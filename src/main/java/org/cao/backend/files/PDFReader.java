package org.cao.backend.files;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PDFReader {

    // =============== Attributs ===============

    private String filePath;
    private PDDocument document;
    private String cachedText;

    // =============== Constructeur ===============

    public PDFReader(String filePath) {
        this.filePath = filePath;
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
        Logger.getLogger("org.apache.fontbox").setLevel(Level.OFF);
    }

    // =============== Méthodes ===============

    private void loadDocument() throws IOException {
        if (document == null) {
            document = PDDocument.load(new File(filePath));
        }
    }

    private String getText() throws IOException {
        if (cachedText == null) {
            loadDocument();
            if (document.isEncrypted()) return null;
            PDFTextStripper pdfStripper = new PDFTextStripper();
            cachedText = pdfStripper.getText(document);
        }
        return cachedText;
    }

    public List<String> readLinesInTable() {
        List<String> result = new ArrayList<>();

        try {
            String text = getText();
            if (text == null) return null;

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

        }

        return result;
    }

    public Set<String> getCodeCANInTable() {
        Set<String> codes = new HashSet<>();
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
        Set<String> codes = getCodeCANInTable();
        List<String> list = new ArrayList<>(codes);
        if (!list.isEmpty()) {
            list.removeLast();
        }

        return list.stream()
                .filter(PDFReader::isValidIndividualCode)
                .collect(Collectors.toList());
    }

    public String getParent() throws Exception {
        Set<String> codes = getCodeCANInTable();
        List<String> list = new ArrayList<>(codes);
        return list.isEmpty() ? null : list.getLast();
    }

    private static boolean isValidIndividualCode(String code) {
        return code != null
                && (code.startsWith("0") || code.startsWith("AF") || code.startsWith("af"))
                && code.length() >= 2
                && Character.isLetterOrDigit(code.charAt(1));
    }

    public void close() {
        if (document != null) {
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // =============== Accesseurs ===============

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.document = null;
        this.cachedText = null;
    }
}