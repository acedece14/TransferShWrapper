package by.katz;

import by.katz.gui.FormMain;
import by.katz.gui.ThemeSetter;

class Main {

    public static void main(String[] args) {
        DB.get().getTransfers().stream()
            .filter(t -> t.getMimeType() == null || t.getMimeType().isEmpty())
            .forEach(t -> t.setMimeType("text/plain"));
        ThemeSetter.apply(3);
        new FormMain(args);
    }

}
