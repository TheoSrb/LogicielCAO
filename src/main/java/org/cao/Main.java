package org.cao;

import org.cao.frontend.frame.MainFrame;

public class Main {

    static void main(String[] args) {
        // Appel du constructeur de la fenêtre principale afin de la lancer.
        MainFrame mainFrame = new MainFrame();
        // On la démarre.
        mainFrame.startDisplaying();
    }

}
