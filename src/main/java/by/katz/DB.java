package by.katz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DB {

    private static final java.lang.reflect.Type TYPE_LIST_TRANSFER = new TypeToken<List<Transfer>>() {}.getType();
    private static final File DB_FILE = new File("transferSh.json");
    private static DB instance;

    @Getter(value = AccessLevel.PUBLIC)
    private List<Transfer> transfers = new ArrayList<>();

    private DB() {
        if (!DB_FILE.exists())
            saveDb();
        else readDb();
    }

    public static DB get() { return instance == null ? instance = new DB() : instance; }

    public void putNewRecord(Transfer transfer) {
        transfers.add(transfer);
        saveDb();
    }

    private void readDb() {
        try (FileReader fileReader = new FileReader(DB_FILE)) {
            transfers = new Gson()
                  .fromJson(fileReader, TYPE_LIST_TRANSFER);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private synchronized void saveDb() {
        String json = new GsonBuilder()
              .setPrettyPrinting()
              .create()
              .toJson(transfers);
        try (FileWriter fw = new FileWriter(DB_FILE)) {
            fw.write(json);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void removeTransfer(String url) {
        Transfer transferToDelete = getTransferByUrl(url);
        if (transferToDelete == null)
            return;
        transfers.remove(transferToDelete);
        saveDb();
    }

    public Transfer getTransferByUrl(String url) {
        return transfers.stream()
              .filter(t -> t.getUrl().equals(url))
              .findFirst()
              .orElse(null);
    }

    public void removeTransfer(Transfer t) {
        transfers.remove(t);
        saveDb();
    }
}
