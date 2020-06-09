package com.refroutes.file;

import com.refroutes.log.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    private Properties properties;

    public Settings (String filePath) {
        initProperties(filePath);
    }

    public String getValue(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public Boolean getValue(String key, boolean defaultValue) {
        String booleanDefaultString = Boolean.toString(defaultValue);
        String booleanString = properties.getProperty(key, booleanDefaultString);
        return Boolean.parseBoolean(booleanString);
    }

    private void initProperties(String filePath) {
        properties = new Properties();

        try {
            InputStream stream = new FileInputStream(filePath);
            properties.load(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
