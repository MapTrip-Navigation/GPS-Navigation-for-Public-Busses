package com.refroutes.log;

import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

@WorkerThread
public class Logger {
    /**
     *  Simple Logger.
     *  Of course there are standard solutions; this logger was created to learn a little bit more about development under android.
     *
     *  All classes use the same log file during the apps life cycle.
     *  Nevertheless it is possible to distinguish between calling classes by setting a unique name/identifier when creating an instance of the logger.
     *
     *  How to use:
     *  1. At first (typically in your Main Activity) set the base path for logging. If you use a property file this must be stored here.
     *     If you don't use a property file logfiles are stored under the base path into the directory ./log/logging.log
     *     Set the base path by the method Logger.setBasePath()
     *  2. Create an instance of the logger with Logger.createLogger()
     *  3. Write log entries with the method logger.log()
     *
     *  Property File:
     *  The propertyFile allows setting of less properties
     *    loggingFileName - relative to app path, without leading slash
     *    logLevel        - like defined in java.util.logging
     *    maxFileSize     - max size until rotating to next file, size is only a reference value and not guaranteed exactly
     *
     */

    private String identifier;
    private long maxFileSize = 1000000;
    private static Properties properties;
    private static String parentPath;
    private static boolean reStarted = true;
    private static String logFileName;
    private static Level logLevel;
    private static long fileSize = 0;
    private static ConcurrentLinkedQueue<String> logEntries;
    private static boolean cacheBlocked = false;
    private static Thread cacheRunner;

    private static String placeHolder = "                                                  ";

    /**
     * Returns a new Logger
     * @param identifier Any string to distinguish between the different classes of the project using the logger.
     *                   This can be the name of a class or whatever.
     */
    public static Logger createLogger(String identifier) throws InternalError{
        if (parentPath == null) {
            throw new InternalError("propertyFilePath not yet initialized. At first call static method setPropertyFilePath()");
        }

        startCacheWriter();

        Logger logger = new Logger (identifier);
        return logger;
    }

    /**
     * Used to set the basic path of logging. If a property file has to be used it is expected in this path.
     * @param parentPath Absolute path of logging.
     */
    public static void setBasePath(String parentPath) {
        Logger.parentPath = parentPath;
        properties = new Properties();

        try {
            InputStream stream = new FileInputStream(parentPath + "/logging.properties");
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a log entry with level FINEST
     * @param message The log message
     * @param source Method or other info about source
     */
    public void finest(String source, String message) {
        log(source, message, Level.FINEST);
    }

    /**
     * Create a log entry with level FINER
     * @param message The log message
     * @param source Method or other info about source
     */
    public void finer(String source, String message) {
        log(source, message, Level.FINER);
    }

    /**
     * Create a log entry with level CONFIG
     * @param message The log message
     * @param source Method or other info about source
     */
    public void config(String source, String message) {
        log(source, message, Level.CONFIG);
    }

    /**
     * Create a log entry with level WARNING
     * @param message The log message
     * @param source Method or other info about source
     */
    public void warn(String source, String message) {
        log(source, message, Level.WARNING);
    }

    /**
     * Create a log entry with level INFO
     * @param message The log message
     * @param source Method or other info about source
     */
    public void info(String source, String message) {
        log(source, message, Level.INFO);
    }

    /**
     * Create a log entry with level FINE
     * @param message The log message
     * @param source Method or other info about source
     */
    public void fine(String source, String message) {
        log(source, message, Level.FINE);
    }

    /**
     * Create a log entry with level SEVERE
     * @param message The log message
     * @param source Method or other info about source
     */
    public void severe(String source, String message) {
        log(source, message, Level.SEVERE);
    }

    private Logger(String identifier) {
        this.identifier = identifier;

        if (reStarted) {
            String logFileName = parentPath + "/" + getProperty("loggingFileName", "log/logging.log");
            Level logLevel = Level.parse(getProperty("logLevel", "FINE"));
            this.logFileName = logFileName;
            this.logLevel = logLevel;

            checkPath();
        }
    }

    /*
     * Create the log entry
     * @param message The log message
     * @param source Method or other info about source
     * @param level Log level (java.util.logging.Level)
     */
    private void log(String source, String message, Level level) {
        // Filter log level
        if (logLevel.intValue() > level.intValue()) {
            return;
        }

        replaceLogFile();

        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

        String space50 = new String(new char[50]).replaceAll("", " ");

        if (null == source) {
            source = "";
        }
        String strDate = (formatter.format(date) + placeHolder).substring(0, 30);
        String levelFormatted = (level.getName() + placeHolder).substring(0,15);
        String identifierFormatted = (identifier + ": " + source + placeHolder).substring(0,40) + "    ";

        message = String.format("%s%s%s%s%s", strDate, levelFormatted, identifierFormatted, message, System.lineSeparator());

        logToCache(message);
    }

    private String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private void checkPath() {
        File file = new File (logFileName);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdir();
        }
    }

    private static void addBytesToLogFileSize(long bytes) {
        fileSize+=bytes;
    }

    private long getLogFileSize () {
        // at first init get file size by file
        if (reStarted) {
            File file = new File(logFileName);
            if (!file.exists()) {
                fileSize = 0;
            } else {
                fileSize = file.length();
            }
        }

        // aware that fileSize is resized by method addBytesToLogFileSize()
        return fileSize;
    }

    private void replaceLogFile() {
        if (reStarted) {
            String maxFileSizeStr = properties.getProperty("maxFileSize", new Long(maxFileSize).toString());
            maxFileSize = Long.parseLong(maxFileSizeStr);
        }

        int i = 0;
        if (getLogFileSize() > maxFileSize) {
            while (i < 1000) {
                File file = new File(logFileName + "." + i);
                if (!file.exists()) {
                    break;
                }
                ++i;
            }
            // replace
            File oldFile = new File(logFileName);
            File newFile = new File(logFileName + "." + i);
            oldFile.renameTo(newFile);
        }

        reStarted = false;
    }

    @WorkerThread
    private static void writeToFile(String message) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(logFileName, true);
            fileOutputStream.write((message).getBytes());
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        addBytesToLogFileSize(message.getBytes().length);
    }

    private static void logToCache(String message) {
        while (cacheBlocked) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logEntries.add(message);
    }

    private static void startCacheWriter() {
        if (null == cacheRunner) {
//            logEntries = Collections.synchronizedHashMap(new ArrayList<String>());
            logEntries = new ConcurrentLinkedQueue<String>();
            cacheRunner = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
//                        cacheBlocked = true;
                        if (!logEntries.isEmpty()) {
                            String logEntry = logEntries.poll();
                            writeToFile(logEntry);
                        }
//                        cacheBlocked = false;

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            cacheRunner.start();
        }
    }

    private static class LogObject {
        private String message;
        private String source;
        private Level level;

        public LogObject (String source, String message, Level level) {
            this.source = source;
            this.message = message;
            this.level = level;
        }

        public Level getLevel() {
            return this.level;
        }

        public String getMessage() {
            return this.message;
        }

        public String getSource () {
            return this.source;
        }
    }


}
