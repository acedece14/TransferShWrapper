package by.katz;

import by.katz.gui.FormMain;
import by.katz.gui.ThemeSetter;

class Main {

    public static void main(String[] args) {
        ThemeSetter.apply(3);
        new FormMain(args);
    }

}
