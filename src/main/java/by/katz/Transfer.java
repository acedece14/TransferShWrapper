package by.katz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Log
public
class Transfer {

    private static final String URL = "https://transfer.sh";
    //private static final String URL = "http://127.0.0.1:8080";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private File file;
    private String url;
    private String urlDelete;
    private long size;
    private long uploadTime;
    @Setter
    private String mimeType;


    final static HttpClient httpClient = HttpClientBuilder.create().build();

    public Transfer(File file) {
        this.file = file;
        this.size = file.length();
        this.uploadTime = new Date().getTime();

        try { this.mimeType = Files.probeContentType(file.toPath()); } catch (IOException ignored) { }
        if (mimeType == null)
            mimeType = URLConnection.guessContentTypeFromName(file.getName());
        if (mimeType == null)
            mimeType = "text/plain";
    }


    public void uploadFileToServer() throws IOException {
        log.info("Upload file: " + file.getAbsolutePath());
        final HttpPut http = new HttpPut("https://transfer.sh/" + file.getName());
        final byte[] bytes = Files.readAllBytes(file.toPath());
        final HttpEntity entity = new ByteArrayEntity(bytes);
        http.setEntity(entity);

        final HttpResponse resp = httpClient.execute(http);
        final InputStream in = resp.getEntity().getContent();
        url = new BufferedReader(
              new InputStreamReader(in, StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
        urlDelete = resp.getFirstHeader("X-Url-Delete").getValue();
    }

    public boolean deleteFromServer() {
        final HttpDelete http = new HttpDelete(urlDelete);
        try { httpClient.execute(http); } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return "Transfer{" +
              "file=" + file.getPath() +
              ",\nurl='" + url + '\'' +
              ",\nurlZip='" + getUrlZip() + '\'' +
              ",\nurlDelete='" + urlDelete + '\'' +
              ",\nsize=" + getFormattedSize() +
              ",\nmimeType='" + mimeType + '\'' +
              ",\nuploadTime=" + getFormattedDate() +
              '}';
    }


    public static String getFormattedSize(long size) {
        if (size < 1_000)
            return size + " b";
        if (size < 1_000_000)
            return String.format("%.2f", (size / 1_000.0)) + " k";
        if (size < 1_000_000_000)
            return String.format("%.2f", (size / 1_000_000.0)) + " M";
        return String.format("%.2f", (size / 1_000_000_000.0)) + " G";
    }

    public String getFormattedSize() { return getFormattedSize(size); }

    public String getFormattedDate() { return sdf.format(new Date(uploadTime)); }

    public String getUrlZip() { return url.replace("transfer.sh", "transfer.sh/%28") + "%29.zip"; }

    private static Map<String, String> getHeaders() {
        final String in = "Accept\t*/*\n" +
              "Accept-Encoding\tgzip, deflate, br\n" +
              "Accept-Language\tru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3\n" +
              "Connection\tkeep-alive\n" +
              "DNT\t1\n" +
              "Host\ttransfer.sh\n" +
              "Sec-Fetch-Dest\tempty\n" +
              "Sec-Fetch-Mode\tcors\n" +
              "Sec-Fetch-Site\tsame-origin\n" +
              "Sec-GPC\t1\n" +
              "TE\ttrailers\n" +
              "User-Agent\tMozilla/5.0 (Windows NT 6.1; Win64; x64; rv:92.0) Gecko/20100101 Firefox/92.0\n";
        Map<String, String> headers = new HashMap<>();
        Arrays.stream(in.split("\n")).forEach(line -> headers.put(line.split("\t")[0], line.split("\t")[1]));
        return headers;
    }
}
