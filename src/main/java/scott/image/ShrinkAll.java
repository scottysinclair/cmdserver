package scott.image;

import java.awt.*;
import java.awt.image.*;
import com.sun.image.codec.jpeg.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;

public class ShrinkAll {

	public static void main(String args[]) {
		try {
			File dir = new File(args[0]);
			if (dir.exists() == false) {
				throw new Exception("source dir does not exist");
			}

			File toDir = new File(args[1]);
			int width = Integer.parseInt(args[2]);
			File pics[] = dir.listFiles(new FilenameFilter(){
				public boolean accept(File dir, String fileName) {
					return fileName.endsWith(".JPG") || fileName.endsWith(".JPEG") ||
								 fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
				}
			});

			for (int i=0; i<pics.length; i++) {
				shrink(pics[i], new File(toDir, pics[i].getName()), width);
			}
		}
		catch(Exception x) {
			x.printStackTrace();
		}
	}


  private static void shrink(File sourceFile, File thumbFile, int width) throws IOException {

    File thumbDir = thumbFile.getParentFile();
    if (!thumbDir.exists()) {
      thumbDir.mkdir();
    }

    BufferedImage bi = ImageIO.read(sourceFile);

    float scale = ((float)width / (float)bi.getWidth());
    int height = (int)((float)bi.getHeight() * scale);

    BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = scaled.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    graphics2D.drawImage(bi, 0, 0, width, height, null);

    BufferedOutputStream out = new BufferedOutputStream(new
      FileOutputStream( thumbFile ));

    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
    JPEGEncodeParam param = encoder.
      getDefaultJPEGEncodeParam(scaled);

    param.setQuality(1f, false);
/*    int quality = 50;
    param.setQuality((float)quality / 100.0f, false);
    */
    encoder.setJPEGEncodeParam(param);
    encoder.encode(scaled);
    out.flush();
    out.close();
    System.out.println("created thumb for " + sourceFile.getPath());
  }

}