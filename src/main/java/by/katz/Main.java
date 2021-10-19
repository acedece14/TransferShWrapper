package by.katz;

import by.katz.gui.FormMain;

import java.io.IOException;

class Main {

    public static void main(String[] args) {
        DB.get().getTransfers().stream()
              .filter(t -> t.getMimeType() == null || t.getMimeType().isEmpty())
              .forEach(t -> t.setMimeType("text/plain"));
        //ThemeSetter.apply(3);
        // new FormMain(args);
        try {
            final var receiveServer = new LinkReceiver();
            final var form = new FormMain(args);
            receiveServer.setForm(form);
            Runtime.getRuntime().addShutdownHook(new Thread(receiveServer::close));
        } catch (IOException e) {
            LinkReceiver.sendDataToAnotherExemplair(args);
        }
    }


}
