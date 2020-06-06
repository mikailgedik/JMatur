package ch.mikailgedik.kzn.matur.filemanager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FileManager {
    public FileManager() {
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
}
