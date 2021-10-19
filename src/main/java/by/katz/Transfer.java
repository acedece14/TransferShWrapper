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
import java.util.Date;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Log
public
class Transfer {

    final static HttpClient httpClient = HttpClientBuilder.create().build();
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

    public static String getFormattedSize(long size) {
        if (size < 1_000)
            return size + " b";
        if (size < 1_000_000)
            return String.format("%.2f", (size / 1_000.0)) + " k";
        if (size < 1_000_000_000)
            return String.format("%.2f", (size / 1_000_000.0)) + " M";
        return String.format("%.2f", (size / 1_000_000_000.0)) + " G";
    }

    public void uploadFileToServer() throws IOException {
        final HttpPut http = new HttpPut("https://transfer.sh/" + file.getName());
        final byte[] bytes = Files.readAllBytes(file.toPath());
        log.info("Upload file: " + file.getAbsolutePath());
        final HttpEntity entity = new ByteArrayEntity(bytes);
        http.setEntity(entity);

        final HttpResponse resp = httpClient.execute(http);
        final InputStream in = resp.getEntity().getContent();
        url = new BufferedReader(
              new InputStreamReader(in, StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
        urlDelete = resp.getFirstHeader("X-Url-Delete").getValue();
        log.info("File uploaded to: " + url);
    }

    public boolean deleteFromServer() {
        try { httpClient.execute(new HttpDelete(urlDelete)); } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return "Transfer{" +
              "file='" + file.getPath() + '\'' +
              ",\nurl='" + url + '\'' +
              ",\nurlZip='" + getUrlZip() + '\'' +
              ",\nurlDelete='" + urlDelete + '\'' +
              ",\nsize=" + getFormattedSize() +
              ",\nmimeType='" + mimeType + '\'' +
              ",\nuploadTime=" + getFormattedDate() +
              '}';
    }

    public String getFormattedSize() { return getFormattedSize(size); }

    public String getFormattedDate() { return sdf.format(new Date(uploadTime)); }

    public String getUrlZip() { return url.replace("transfer.sh", "transfer.sh/%28") + "%29.zip"; }
}
