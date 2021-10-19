package by.katz;

import by.katz.gui.FormMain;
import lombok.extern.java.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

@Log
class LinkReceiver {

    private static final int PORT_NUMBER = 54345;
    private ServerSocket serverSocket;
    private FormMain form;

    LinkReceiver() throws IOException {
        serverSocket = new ServerSocket(PORT_NUMBER);
        log.info("Receive server created");
        new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try (var socket = serverSocket.accept();
                     var isr = new InputStreamReader(socket.getInputStream());
                     var br = new BufferedReader(isr)) {

                    var list = new ArrayList<String>();
                    String tmp;
                    while ((tmp = br.readLine()) != null)
                        list.add(tmp);
                    var fileList = new ArrayList<File>();
                    for (String a : list)
                        fileList.add(new File(a));
                    form.toFront();
                    form.repaint();
                    form.uploadFiles(fileList);
                } catch (IOException ignored) { }
            }
        }).start();
    }


    static void sendDataToAnotherExemplair(String[] args) {
        try (var socket = new Socket("127.0.0.1", PORT_NUMBER);
             var out = socket.getOutputStream()) {

            var tmp = "";
            for (var s : args)
                tmp += s + "\n";
            out.write(tmp.trim().getBytes());
        } catch (IOException e) { e.printStackTrace(); }
    }

    void setForm(FormMain form) { this.form = form; }

    void close() {
        log.info("Try close application");
        try {
            serverSocket.close();
        } catch (IOException ignored) { }
    }
}
