package by.katz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@AllArgsConstructor
public
class Transfer {

    private static final String URL = "https://transfer.sh/";
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


    public void uploadFileToServer() throws IOException {
        String filename = file.getName();
        Connection.Response response = Jsoup.connect(URL + filename)
            .data("file", filename, new FileInputStream(file))
            .method(Connection.Method.PUT)
            .header("Content-Type", mimeType)
            .execute();
        urlDelete = response.header("X-Url-Delete");
        url = response.parse().body().text();
        System.out.println();
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean deleteFromServer() {
        try {
            Jsoup.connect(urlDelete)
                .method(Connection.Method.DELETE)
                .execute();
            DB.get().removeTransfer(url);
        } catch (IOException e) {
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
}
