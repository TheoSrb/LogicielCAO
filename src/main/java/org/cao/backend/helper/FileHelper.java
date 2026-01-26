package org.cao.backend.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileHelper {

    // =============== Attributs ===============

    private File file;

    // =============== Constructeur ===============

    public FileHelper(File file) {
        this.file = file;
    }

    // =============== Méthodes ===============

    public List<String> readAllLines() {
        List<String> lines = new ArrayList<>();

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lines.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lines;
    }

    public List<String> readAllCodeCAN() {
        List<String> codeCAN = new ArrayList<>();

        for (String line : readAllLines()) {
            String code = line.split(";")[0];
            codeCAN.add(code);
        }

        return codeCAN;
    }

    // =============== Accesseurs ===============

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
