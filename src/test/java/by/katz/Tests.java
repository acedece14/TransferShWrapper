package by.katz;


import lombok.extern.java.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

import static by.katz.LinkReceiver.PORT_NUMBER;

@SuppressWarnings({"unused", "Duplicates"})
@Log
public class Tests {

    private File file = new File("g:\\work\\hercules_3-2-5.exe");
    //private File file = new File("i:\\Download\\hparams(1).json");

    static {
        new MyLogsConf();
    }

    @Test
    @Tag("enable")
    public void testServer() {
        try {
            var server = new LinkReceiver();
            Thread.sleep(100);
            log.info("server1 started");
        } catch (IOException | InterruptedException e) {
            log.info("server1 not started");
        }

        try {
            var server = new LinkReceiver();
            Thread.sleep(100);
            log.info("server2 started");
        } catch (IOException | InterruptedException e) {
            log.info("server2 not started");
        }

        try {
            var socket = new Socket("127.0.0.1", PORT_NUMBER);
            var out = socket.getOutputStream();

            out.write("abc\n".getBytes());
            out.write("defg".getBytes());
            out.close();

            socket = new Socket("127.0.0.1", PORT_NUMBER);
            out = socket.getOutputStream();

            out.write("hi\n".getBytes());
            out.write("jklmn".getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String browserUa = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36";

    @Test
    public void testUpload() {
        log.info("Start test");
        try {
            Transfer transfer = new Transfer(file);
            transfer.uploadFileToServer();
            String url = transfer.getUrl();
            log.info("Mime: " + transfer.getMimeType());
            log.info("Utl: " + transfer.getUrl());
            log.info("Del utl: " + transfer.getUrlDelete());
            //transfer.deleteFromServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void postFile() throws IOException {

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpPut http = new HttpPut("https://transfer.sh/" + file.getName());
        final byte[] bytes = Files.readAllBytes(file.toPath());
        final HttpEntity entity = new ByteArrayEntity(bytes);
        http.setEntity(entity);

        final HttpResponse resp = client.execute(http);
        final InputStream in = resp.getEntity().getContent();

        String url = new BufferedReader(
              new InputStreamReader(in, StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
        String urlDelete = resp.getFirstHeader("X-Url-Delete").getValue();
        log.info("url: " + url);
        log.info("delete: " + urlDelete);
        testDelete(client, urlDelete);
    }

    private void testDelete(HttpClient client, String urlDelete) throws IOException {
        final HttpDelete http = new HttpDelete(urlDelete);
        client.execute(http);
        log.info("complete");
    }
}
