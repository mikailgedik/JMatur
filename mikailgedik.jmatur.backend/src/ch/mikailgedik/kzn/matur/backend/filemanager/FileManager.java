package ch.mikailgedik.kzn.matur.backend.filemanager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileManager {
    public static final String DEFAULT_SETTINGS = "/settings/defaultsettings";

    public static final FileManager FILE_MANAGER;

    static {
        FILE_MANAGER = new FileManager();
    }

    private FileManager() {
    }

    public void saveImage(String name, BufferedImage image) {
        try {
            ImageIO.write(image, "png", new File(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getResourceAsStream(String name) {
        return FileManager.class.getResourceAsStream(name);
    }

    public static FileManager getFileManager() {
        return FILE_MANAGER;
    }
}
