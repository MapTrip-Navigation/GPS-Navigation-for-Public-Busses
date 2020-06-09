package com.refroutes.file;

import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for handling CSV files.
 * Every line is devided into an array of items.
 */
public class CsvFile {

    /**
     * Delivers the lines of a CSV file as a List of String-Arrays ([]).
     * The size resp. length of the Strings[] can vary between every line.
     *
     * @param filePath Complete Path of the file
     * @param separator The delimiter between the values of a single line.
     * @return The file content prepared as an Array.
     */
    public static List<ArrayList<String>> readFile(String filePath, String separator) {
        try {
            List<ArrayList<String>> lines = new ArrayList();
            BufferedReader bufferedReader = createBufferedReader(filePath);
            if (bufferedReader != null) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    line = line.trim();
                    if (!"".equals(line)) {
                        String[] items = line.split(separator);
                        ArrayList<String> lineAsArray = new ArrayList<>();
                        for (int i = 0; i < items.length; i++) {
                            lineAsArray.add(items[i]);
                        }
                        lines.add(lineAsArray);
                    }
                }
                return lines;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeToFile(String filePath, List<String> lines) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath,false);

            Iterator<String> it = lines.iterator();
            boolean firstLine = true;
            while (it.hasNext()) {
                String line = it.next();
                if (!firstLine) {
                    line = System.lineSeparator() + line;
                }
                fileOutputStream.write((line).getBytes());
                firstLine=false;

            }
            fileOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /* Creates a buffer for reading file
     */
    private static BufferedReader createBufferedReader(String filePath) {
        try {
            File file = new File(filePath);
            InputStream stream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            return bufferedReader;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
