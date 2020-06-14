package com.refroutes.file;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.refroutes.main.MainActivity;
import com.refroutes.model.RefRoute;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@WorkerThread

/**
 * Handles the lists of reference routes
 */
public class RefRouteController {
    private static String SEQUENCE_FILE_NAME = "refroutes.csv";
    private static int REF_ROUTE_NAME_POS = 0;
    private static int REF_ROUTE_DESCRIPTION_POS = 1;
    private static int REF_ROUTE_FILENAME_POS = 2;
    private static int REF_ROUTE_ACTIVE_POS = 3;
    private static String CSV_SEPARATOR = ";";

    private static Context mainActivity;

    public static String REF_ROUTE_FILE_SUFFIX = ".csv";

    public void initContext(Context context) {
        mainActivity = context;
    }

    /**
     * Initializes the sequence file. If this file doesn't exist the app file directory is searched
     * for files with the matching extension (suffix) and writes them to the file.
     */
    public static void initSequenceFile(String fileDirectory) {
        // check if file exists
        File file = new File(fileDirectory + "/" + SEQUENCE_FILE_NAME);
        if (!file.exists() || file.length() <= 0) {
            // no sequence file found
            // write new one
            ArrayList<String> fileList = readRefRouteFileList(fileDirectory);
            if (!fileList.isEmpty()) {
                // writing sequence file makes no sense without routes
                ArrayList<RefRoute> refRoutes = new ArrayList<>();
                Iterator<String> it = fileList.iterator();
                int index = 0;
                while (it.hasNext()) {
                    String fileName = it.next();
                    String prefix = "invalid_filename";
                    // remove /
                    int pos = fileName.lastIndexOf("/");
                    if (pos > 0) {
                        prefix = fileName.substring(pos + 1);
                        fileName = prefix;
                    }
                    // remove suffix
                    pos = prefix.lastIndexOf('.');
                    if (pos > 0) {
                        prefix = prefix.substring(0, pos);
                    }
                    RefRoute refRoute = new RefRoute(prefix, "Description of " + prefix + "...", fileName, index++, false);
                    refRoutes.add(refRoute);
                }
                writeRefRoutesToFile(fileDirectory, refRoutes);
            }
        }
    }

    /*
     * Delivers a list of filename prefixes. This prefixes are used as RefRoute names in the sequence list.
     * @param fileDirectory The directory to search for refroute files
     * @return A list with filename prefixes or an empty list if no file exists.
     */
    private static ArrayList<String> readRefRouteFileList(String fileDirectory) {
        // create filter for refroute files
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(REF_ROUTE_FILE_SUFFIX);
            }
        };

        // list refroute files
        String path = fileDirectory + "/";
        ArrayList<String> fileNames = new ArrayList<>();
        File directory = new File(path);
        File[] fileList = directory.listFiles(filter);
        try {
            for (int i = 0; i < fileList.length; i++) {
                if (!fileList[i].getName().contains(SEQUENCE_FILE_NAME)) {
                    fileNames.add(fileList[i].getCanonicalPath());
                }
            }
        } catch (Exception e) {
            MainActivity.showMessage("Fehler", "Einlesen der Routen-Dateien fehlgeschlagen: " + e.getMessage());
            e.printStackTrace();
        }
        return fileNames;
    }

    /**
     * Delivers the list of reference routes which are stored to a file
     *
     * @param fileDirectory The source directory of the file
     * @return The list of reference routes.
     */
    public static ArrayList<RefRoute> readRefRoutesFromFile(String fileDirectory) {
        // Get items from file
        String filePath = fileDirectory + "/" + SEQUENCE_FILE_NAME;
        ArrayList<RefRoute> fileItems = new ArrayList<>();

        List<ArrayList<String>> csvLines = CsvFile.readFile(filePath, CSV_SEPARATOR);
        if (null != csvLines) {
            int refRouteIndex = 0;

            Iterator<ArrayList<String>> it = csvLines.iterator();
            while (it.hasNext()) {
                ArrayList<String> nextItem = it.next();
                String refRouteName = nextItem.get(REF_ROUTE_NAME_POS);
                String refRouteDescription = nextItem.get(REF_ROUTE_DESCRIPTION_POS);
                String fileName = nextItem.get(REF_ROUTE_FILENAME_POS);
                String activeString = nextItem.get(REF_ROUTE_ACTIVE_POS);
                fileItems.add(new RefRoute(refRouteName, refRouteDescription, fileName, refRouteIndex, Boolean.valueOf(activeString)));
                ++refRouteIndex;
            }
        }
        return fileItems;
    }

    /**
     * Writes the sequence of reference routes to the sequence file.
     *
     * @param fileDirectory Directory where app files are stored
     * @param refRoutes     List of objects to be persisted.
     */
    public static void writeRefRoutesToFile(String fileDirectory, ArrayList<RefRoute> refRoutes) {
        String filePath = fileDirectory + "/" + SEQUENCE_FILE_NAME;
        Iterator<RefRoute> it = refRoutes.iterator();
        ArrayList<String> csvLines = new ArrayList<>();
        while (it.hasNext()) {
            RefRoute refRoute = it.next();
            String csvLine = new StringBuilder().append(refRoute.getRefRouteName())
                    .append(CSV_SEPARATOR)
                    .append(refRoute.getRefRouteDescription())
                    .append(CSV_SEPARATOR)
                    .append(refRoute.getRefRouteFileName())
                    .append(CSV_SEPARATOR)
                    .append(refRoute.isActive()).toString();

            csvLines.add(csvLine);
        }
        CsvFile.writeToFile(filePath, csvLines);
    }
}
