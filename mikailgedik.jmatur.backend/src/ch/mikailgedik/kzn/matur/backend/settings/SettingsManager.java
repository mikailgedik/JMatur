package ch.mikailgedik.kzn.matur.backend.settings;

import ch.mikailgedik.kzn.matur.backend.connector.Constants;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;

import java.io.*;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsManager implements Constants {

    private final TreeMap<String, Object> settings;
    private SettingsManager() {
        this.settings = new TreeMap<>();
        makeDefault();
    }

    public static SettingsManager createDefaultSettingsManager() {
        return new SettingsManager();
    }

    /** Removes all the settings and loads the default values from {@link FileManager}*/
    public void makeDefault() {
        settings.clear();
        readFromStream();
    }

    private void readFromStream() {
        BufferedReader reader = null;
        Pattern nameValidator = Pattern.compile("[^a-zA-Z.]");
        InputStream inputStream = FileManager.getFileManager().getResourceAsStream(FileManager.DEFAULT_SETTINGS);
        if(inputStream == null) {
            throw new RuntimeException("Cannot get resource \"" + FileManager.DEFAULT_SETTINGS + "\": Resource not found");
        }

        try {
            reader = new BufferedReader(new InputStreamReader(FileManager.getFileManager().getResourceAsStream("/settings/defaultsettings")));
            String line, name, stringVal;
            Object value;
            int lineNum = 0;

            while((line =reader.readLine()) != null) {
                try {
                    lineNum++;
                    if(line.startsWith("#") || line.equals("")) {
                        continue;
                    }
                    int indexOf = line.indexOf('=');
                    if(indexOf < 1) {
                        throw new RuntimeException("No \"=\" in line ");
                    }
                    name = line.substring(0, indexOf);
                    //Regex: search for characters not equal to a-z or A-Z or . (dot)
                    Matcher nameMatcher = nameValidator.matcher(name);
                    if(nameMatcher.find()) {
                        throw new RuntimeException("Name " + name + " contains illegal character-sequence: " + name.substring(nameMatcher.start(), nameMatcher.end()));
                    }
                    stringVal = line.substring(indexOf + 1);
                    int indexOfDot = name.indexOf('.');
                    if(indexOfDot < 1) {
                        throw new RuntimeException("No dot provided, no datatype given");
                    }
                    String dataType = name.substring(0, indexOfDot);
                    value = switch (dataType) {
                        case "int" -> Integer.valueOf(stringVal);
                        case "double" -> Double.valueOf(stringVal);
                        case "string" -> stringVal;
                        default -> throw new RuntimeException("Unknown datatype: " + dataType);
                    };
                    if(settings.containsKey(name)) {
                        throw new RuntimeException("Setting " + name + " exists twice");
                    }
                    settings.put(name, value);
                } catch(RuntimeException exception) {
                    throw new RuntimeException("Exception reading line " + lineNum, exception);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot get resource \"" + FileManager.DEFAULT_SETTINGS + "\": Exception on reading", e);
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Cannot get resource \"" + FileManager.DEFAULT_SETTINGS + "\": Exception closing stream", e);
            }
        }
    }

    public void addSetting(String name, Object value) {
        settings.put(name, value);
    }

    public boolean exists(String name) {
        return settings.containsKey(name);
    }

    public Object get(String name) {
        if(!exists(name)) {
            throw new RuntimeException("Setting " + name + " does not exist");
        }
        return settings.get(name);
    }

    public int getI(String name) {
        return (int) settings.get(name);
    }

    public double getD(String name) {
        return (double) get(name);
    }

    public String getS(String name) {
        return (String) settings.get(name);
    }
}
