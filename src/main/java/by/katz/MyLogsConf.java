package by.katz;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MyLogsConf extends SimpleFormatter {
    static {
        InputStream stream = Main.class.getClassLoader().
              getResourceAsStream("logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
            Logger LOGGER = Logger.getLogger(Main.class.getName());
            LOGGER.info("logs configured");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public String formatMessage(LogRecord r) {
        return "{" + r.getSourceClassName() + " : " + r.getSourceMethodName() + "}\n\t\t\t\t\t\t\t\t"
              + r.getMessage();
        //return record.getLevel() + "\t" + record.getSourceClassName() + "\t" + record.getMessage();
        //return super.formatMessage(record);
    }
}
