package by.katz.gui;

import by.katz.DB;
import by.katz.Transfer;
import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static by.katz.Transfer.getFormattedSize;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

public class FormMain extends JFrame {
    private static final int INDEX_FILE = 0;
    private static final int INDEX_URL = 1;
    private static final int INDEX_SIZE = 2;
    private static final int INDEX_TYPE = 3;
    private static final int INDEX_DATETIME = 4;
    private static final int MAX_UPLOAD_THREADS = 3;
    private JTable tblTransfers;
    private JPanel pnlMain;
    private JButton btnCopy;
    private JButton btnDelete;
    private JLabel lblStatus;
    private JButton btnShowInTC;
    private JButton btnCopyToCBZip;

    static { FlatHiberbeeDarkIJTheme.setup(); }

    public FormMain(String[] args) {
        setTitle("Wrapper for transfer.sh");
        setContentPane(pnlMain);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(850, 500);
        try {
            final InputStream stream = this.getClass().getClassLoader().getResourceAsStream("transferShWrapper.png");
            if (stream != null)
                setIconImage(ImageIO.read(stream));
        } catch (IOException ignored) {}
        setLocationRelativeTo(null);
        setupTable();
        updateTranferTable();
        btnCopy.addActionListener(e -> onCopyCbClick());
        btnCopyToCBZip.addActionListener(e -> onCopyCbZipClick());
        btnShowInTC.addActionListener(e -> onBtnShowInTcClick());
        btnDelete.addActionListener(e -> onDeleteClick());
        pnlMain.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    var obj = evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    //noinspection unchecked
                    uploadFiles((List<File>) obj);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        setupPopup();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(1);
            }
        });
        setVisible(true);
        uploadFromExternal(args);
    }


    private void uploadFromExternal(String[] args) {
        if (args.length > 0) {
            var filesToUpload = new ArrayList<File>();
            for (var filePath : args)
                filesToUpload.add(new File(filePath));
            uploadFiles(filesToUpload);
        }
    }


    private static void addActionToPopup(JPopupMenu menu, String caption, ActionListener a) {
        var item = new JMenuItem();
        item.addActionListener(a);
        item.setText(caption);
        menu.add(item);
    }

    private void setupPopup() {
        var menu = new JPopupMenu();

        addActionToPopup(menu, "Copy to cb", e -> onCopyCbClick());
        addActionToPopup(menu, "Copy to zb (zip)", e -> onCopyCbZipClick());
        addActionToPopup(menu, "Delete", e -> onDeleteClick());

        tblTransfers.addMouseListener(new MouseListenerImpl() {
            @Override public void mousePressed(MouseEvent e) { showPopup(e); }

            @Override public void mouseReleased(MouseEvent e) { showPopup(e); }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger())
                    menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

    }

    private void onDeleteClick() {
        var transfers = getSelectedTransfers();
        new Thread(() -> transfers.forEach(t -> {
            if (t.deleteFromServer()) {
                DB.get().removeTransfer(t);
                updateTranferTable();
            }
        })).start();
    }

    private void onBtnShowInTcClick() {
        var transfers = getSelectedTransfers();
        if (transfers.size() > 0)
            showInTC(transfers.get(0)
                  .getFile()
                  .getPath());
    }

    private void onCopyCbZipClick() {
        var transfers = getSelectedTransfers();
        if (transfers.size() > 0) {
            StringBuilder tmp = new StringBuilder();
            for (var t : transfers)
                tmp.append(t.getUrlZip()).append("\n");
            copyToCB(tmp.toString());
        }
    }

    private void onCopyCbClick() {
        var transfers = getSelectedTransfers();
        if (transfers.size() > 0) {
            var tmp = new StringBuilder();
            for (Transfer t : transfers)
                tmp.append(t.getUrl()).append("\n");
            copyToCB(tmp.toString());
        }
    }

    public void uploadFiles(List<File> files) {

        while (files.stream().anyMatch(File::isDirectory))
            files = unpackFilePaths(files);

        final int NO = 1;
        if (files.size() > 1) {
            long totalSize = files.stream()
                  .mapToLong(File::length)
                  .sum();
            var msg = "You want to upload " + files.size() + " files, size: " + getFormattedSize(totalSize);
            var input = JOptionPane.showConfirmDialog(null, msg, "Confirm", JOptionPane.YES_NO_OPTION);
            if (input == NO)
                return;
        }

        //this.setEnabled(false);
        System.out.println("start upload files");

        final var hasNewFiles = new AtomicBoolean(false);
        final var pool = Executors.newFixedThreadPool(MAX_UPLOAD_THREADS);
        final var cdl = new CountDownLatch(files.size());

        new Thread(() -> {
            int time = 0;
            while (cdl.getCount() > 0) {
                setStatus("Total: " + cdl.getCount() + ", time: " + time++ + " secs");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) { }
            }
        }).start();
        final var finalFiles = files;
        new Thread(() -> {
            for (File file : finalFiles)
                pool.submit(() -> {
                    setStatus("Upload " + file.getName());
                    var transfer = new Transfer(file);
                    try {
                        transfer.uploadFileToServer();
                    } catch (IOException e) {
                        setStatus("Error: " + file.getName() + " " + e.getLocalizedMessage());
                        cdl.countDown();
                        return;
                    }
                    DB.get().putNewRecord(transfer);
                    hasNewFiles.set(true);
                    setStatus("Uploaded: " + file.getName());
                    updateTranferTable();
                    cdl.countDown();
                });
            try { cdl.await(); } catch (InterruptedException ignored) { }
            pool.shutdown();
            System.out.println("upload files complete");
            //this.setEnabled(true);
        }).start();
    }

    @SuppressWarnings("ConstantConditions")
    private List<File> unpackFilePaths(List<File> files) {
        var result = new ArrayList<File>();
        final var dirs = files.stream()
              .filter(File::isDirectory)
              .collect(Collectors.toList());

        dirs.stream()
              .filter(d -> d != null && d.listFiles() != null)
              .forEach(d -> result.addAll(Arrays.asList(d.listFiles())));
        return result;
    }

    private List<Transfer> getSelectedTransfers() {
        var indexes = tblTransfers.getSelectedRows();
        var tmpTransfers = new ArrayList<Transfer>();
        for (int index : indexes) {
            var url = (String) tblTransfers.getModel().getValueAt(index, INDEX_URL);
            if (url != null)
                tmpTransfers.add(DB.get().getTransferByUrl(url));
        }
        return tmpTransfers;
    }

    private synchronized void updateTranferTable() {
        var model = (DefaultTableModel) tblTransfers.getModel();
        while (model.getRowCount() > 0) {
            model.getDataVector().removeAllElements();
            model.fireTableDataChanged();
        }
        for (var t : DB.get().getTransfers()) {
            final var obj = new Object[]{
                  t.getFile().getName(),
                  t.getUrl(),
                  t.getFormattedSize(),
                  t.getMimeType(),
                  t.getFormattedDate()
            };
            model.insertRow(0, obj);
        }
    }

    private void setupTable() {
        tblTransfers.setDefaultEditor(Object.class, null);
        tblTransfers.setShowGrid(true);
        tblTransfers.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);

        var model = (DefaultTableModel) tblTransfers.getModel();
        model.addColumn("Filename");
        model.addColumn("Url");
        model.addColumn("Size");
        model.addColumn("Type");
        model.addColumn("Upload time");

        var columnModel = tblTransfers.getColumnModel();
        columnModel.getColumn(INDEX_FILE).setPreferredWidth(150);
        columnModel.getColumn(INDEX_URL).setPreferredWidth(250);
        columnModel.getColumn(INDEX_SIZE).setPreferredWidth(10);
        columnModel.getColumn(INDEX_TYPE).setPreferredWidth(40);
        columnModel.getColumn(INDEX_DATETIME).setPreferredWidth(60);

        tblTransfers.addMouseListener(new MouseListenerImpl() {});
    }

    private static void copyToCB(String str) {
        Toolkit.getDefaultToolkit()
              .getSystemClipboard()
              .setContents(new StringSelection(str), null);
    }

    private void showInTC(String path) {
        try {
            var totalCmd = getTotalCmd();
            Runtime.getRuntime().exec(totalCmd + " " + path);
        } catch (IOException e1) { setStatus(e1.getLocalizedMessage()); }
    }

    // TODO
    private String getTotalCmd() { return "C:\\totalcmd\\TOTALCMD64.EXE"; }

    private void setStatus(String newStatus) { new Thread(() -> lblStatus.setText(newStatus)).start(); }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        pnlMain = new JPanel();
        pnlMain.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        pnlMain.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        btnCopy = new JButton();
        btnCopy.setText("Copy to CB");
        panel1.add(btnCopy, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        btnDelete = new JButton();
        btnDelete.setText("Delete");
        panel1.add(btnDelete, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnShowInTC = new JButton();
        btnShowInTC.setText("Show in TC");
        panel1.add(btnShowInTC, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnCopyToCBZip = new JButton();
        btnCopyToCBZip.setText("Copy to CB (zip)");
        panel1.add(btnCopyToCBZip, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        pnlMain.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tblTransfers = new JTable();
        scrollPane1.setViewportView(tblTransfers);
        lblStatus = new JLabel();
        lblStatus.setText("....");
        pnlMain.add(lblStatus, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() { return pnlMain; }

}
