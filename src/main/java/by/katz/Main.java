package by.katz;

import by.katz.gui.FormMain;
import by.katz.gui.ThemeSetter;

class Main {

    public static void main(String[] args) {
        ThemeSetter.apply(3);
        new FormMain(args);
        showTransfersInDb();
    }

    private static void showTransfersInDb() {
        System.out.println("Transfers in db: ");
        DB.get().getTransfers()
                .forEach(t -> System.out.println(t + "\n\n"));
    }
}
