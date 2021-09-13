package by.katz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@AllArgsConstructor
public
class Transfer {

    private static final String URL = "https://transfer.sh/";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss");

    private File file;
    private String url;
    private String urlDelete;
    private long size;
    private long uploadTime;

    public Transfer(File file) {
        this.file = file;
        this.size = file.length();
        this.uploadTime = new Date().getTime();
    }


    public void uploadFileToServer() {
        try {
            String filename = file.getName();
            Connection.Response response = Jsoup.connect(URL + filename)
                    .data("file", filename, new FileInputStream(file))
                    .method(Connection.Method.PUT)
                    .execute();

            urlDelete = response.header("X-Url-Delete");
            url = response.parse().body().text();
            System.out.println();
        } catch (IOException e) { e.printStackTrace(); }
    }

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
                ",\nurlZip='" + getUrlZip() + "%29.zip" + '\'' +
                ",\nurlDelete='" + urlDelete + '\'' +
                ",\nsize=" + size +
                ",\nuploadTime=" + getFormattedDate() +
                '}';
    }

    public String getFormattedDate() { return sdf.format(new Date(uploadTime)); }

    public String getUrlZip() { return url.replace("transfer.sh", "transfer.sh/%28"); }
}
