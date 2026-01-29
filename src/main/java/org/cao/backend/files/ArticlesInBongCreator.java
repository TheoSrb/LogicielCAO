package org.cao.backend.files;

import org.cao.backend.db.DatabaseManager;

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

public class ArticlesInBongCreator extends FileCreator {

    static void main() {
        startDateLog = String.valueOf(LocalDate.now());
        startHourLog = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        ArticlesInBongCreator aibc = new ArticlesInBongCreator();
        aibc.createNewFile();
    }

    @Override
    public void createNewFile() {
        DatabaseManager.startConnectionWithDatabase();
        List<String> codeCANList = new ArrayList<>();
        File fileIn = new File(ARTICLES_IN_BONG_PATH);

        try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD); Statement statement = con.createStatement()) {

            String query = "SELECT CodeCAN FROM (SELECT CodeCAN, MAX(Revision) AS MaxRevision FROM Fichier GROUP BY CodeCAN) m";

            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String codeCAN = resultSet.getString("CodeCAN");
                codeCANList.add(codeCAN);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!fileIn.exists()) {
            try {
                fileIn.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter writer = new FileWriter(fileIn); BufferedWriter bw = new BufferedWriter(writer)) {

            for (String code : codeCANList) {
                bw.write(code + "\n");
            }

            bw.close();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        createLog("FICHIER CRÉÉ", "Le fichier ArticlesInBong.txt a bien été créé.", potentialError);
    }
}
