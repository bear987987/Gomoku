package resource;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;

import javax.imageio.ImageIO;

import gomoku.Setting;

public class ImageLoader {

    private ImageLoader() {
    }

    /**
     * Loader for resource into {@code BufferedImage}, if image result is multiple,
     * use the first found
     * 
     * @param imgName image name, can add suffix ext or not
     * @param size    if null use origin size
     * @return {@code BufferedImage}
     */
    public static BufferedImage loadImage(String imgName, Dimension size) throws FileNotFoundException {
        BufferedImage image = null;
        try {
            File resDir = new File(Setting.resPath);
            File[] targets = resDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    for (String suffix : ImageIO.getReaderFileSuffixes()) {
                        if (pathname.getName().equals(imgName.endsWith(suffix) ? imgName : imgName + '.' + suffix)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (targets != null && targets.length != 0) {
                image = ImageIO.read(targets[0]);
                if (size != null) {
                    Image img = image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
                    image = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = image.createGraphics();
                    g.drawImage(img, 0, 0, null);
                    g.dispose();
                }
                return image;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        throw new FileNotFoundException("Image: " + imgName + " not exist!");
    }
}