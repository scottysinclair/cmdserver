package scott.thumbs;

import java.awt.*;
import java.awt.image.*;
import com.sun.image.codec.jpeg.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;

/*
This is split up into parts.
Part 1: create thumbs if necessary while build a list of matching thumbs -> pics
Part 2: organize the set of matches into HTML pages.
Part 3: write out HTML pages.
*/
public class Thumbs {

  public static void main(String args[]) {
    try {
      int maxWidth = 0;
      int maxImages = 0;
      Set excludeSet = new HashSet();
      java.util.List roots = new ArrayList();

      for (int i=0; i<args.length; i++) {
        if ("--thumb-width".equals(args[i])) {
          maxWidth = Integer.parseInt( args[++i] );
        }
        else if ("--max-per-page".equals(args[i])) {
          maxImages = Integer.parseInt( args[++i] );
        }
        else if ("--exclude-file".equals(args[i])) {
          processExcludes(excludeSet, new File( args[++i] ));
        }
        else if ("--roots".equals(args[i])) {
          processRoots(roots, args[++i]);
        }
      }

      if (maxWidth == 0) {
        System.out.println("Error: --thumb-width is required");
        printUsage();
        return;
      }

      if (maxImages == 0) {
        System.out.println("Error: --max-per-page is required");
        printUsage();
        return;
      }

      if (roots.size() == 0) {
        System.out.println("Error: --roots is required");
        printUsage();
        return;
      }


      DirectoryProcessor processor = null;
      DataBuilder dataBuilder = new DataBuilder();

      ThumbMaker thumbMaker = new ThumbMaker(maxWidth, 1);
      thumbMaker.setThumbEventsListener(dataBuilder);
      thumbMaker.setExcludes( excludeSet );
      processor = thumbMaker;

      DirectoryWalker walker = new DirectoryWalker( processor, new ThumbsDirFilter() );

      Iterator i = roots.iterator();
      while(i.hasNext()) {
        File rootDir = new File((String)i.next());
        walker.walk( rootDir );
      }

      HTMLGenerator htmlGen = new HTMLGenerator();
      htmlGen.buildHTML( dataBuilder, maxImages );
      htmlGen.writeOut();
   }
   catch(Exception x) {
     x.printStackTrace();
   }
  }

  private static void processRoots(java.util.List roots, String rootList) {
    StringTokenizer tok = new StringTokenizer(rootList, ",");
    while(tok.hasMoreTokens()) {
      String root = tok.nextToken();
      roots.add( root );
    }
  }

  private static void processExcludes(Set excludeSet, File excludeList) throws IOException {
    BufferedReader reader = new BufferedReader( new FileReader( excludeList ) );
    String exclude = reader.readLine();
    while(exclude != null) {
      excludeSet.add( exclude.toUpperCase() );
      exclude = reader.readLine();
    }
    reader.close();
  }

  private static void printUsage() {
    System.out.println("Usage: --thumb-width <thumbnail width> --max-per-page <num> --exclude-file <file> --roots <base dir>,<base dir>,...");
  }
}




/** processes a directory in some way. */
interface DirectoryProcessor {
  public void processDirectory(File dir) throws Exception;
}


/** makes thumbnails for images which don't have any. */
class ThumbMaker implements DirectoryProcessor {

  public interface ThumbEvents {
    public void notifyThumbCreated(File sourceFile, File thumbFile);
    public void notifyThumbMatched(File sourceFile, File thumbFile);
  }

  private Set excludeSet;
  private FilenameFilter jpegFilter;
  private ThumbEvents events;
  private float quality;
  private int width;

  public ThumbMaker(int width, float quality) {
    this.width = width;
    this.quality = quality;
    jpegFilter = new JPEGFilenameFilter();

    events = new ThumbEvents() {
      public void notifyThumbCreated(File sourceFile, File thumbFile) {}
      public void notifyThumbMatched(File sourceFile, File thumbFile) {}
    };
  }

  public void setExcludes(Set excludes) {
    excludeSet = excludes;
  }

  public void setThumbEventsListener(ThumbEvents events) {
    this.events = events;
  }

  public void processDirectory(File dir) throws Exception {
    if (isExcluded( dir )) {
      System.out.println("excluding " + dir.getCanonicalPath());
      return;
    }

    File contents[] = dir.listFiles( jpegFilter );
    File thumbsDir = new File(dir, "thumbs");

    for (int i=0; i<contents.length; i++) {
      if (isExcluded(contents[i]) == false) {
        File thumbFile = new File(thumbsDir, contents[i].getName());
        if (thumbFile.exists() == false) {
          createThumb(contents[i], thumbFile);
        }
        else {
          events.notifyThumbMatched(contents[i], thumbFile);
        }
      }
      else {
        System.out.println("excluding " + contents[i].getCanonicalPath());
      }
    }
  }

  private boolean isExcluded(File file) throws IOException {
    return excludeSet.contains(file.getCanonicalPath().toUpperCase());
  }

  private void createThumb(File sourceFile, File thumbFile) throws IOException {

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

    param.setQuality(quality, false);
/*    int quality = 50;
    param.setQuality((float)quality / 100.0f, false);
    */
    encoder.setJPEGEncodeParam(param);
    encoder.encode(scaled);
    out.flush();
    out.close();

    events.notifyThumbCreated(sourceFile, thumbFile);
    System.out.println("created thumb for " + sourceFile.getPath());
  }
}

class DirectoryWalker {

  private DirectoryProcessor processor;
  private FileFilter filter;

  public DirectoryWalker(DirectoryProcessor processor) {
    this(processor, new FileFilter() {
      public boolean accept(File file) { return true; }
    });
  }

  public DirectoryWalker(DirectoryProcessor processor, FileFilter filter) {
    this.processor = processor;
    this.filter = filter;
  }

  public void walk(File dir) throws Exception {
    processor.processDirectory(dir); //process the directory

    File contents[] = dir.listFiles( filter );  //process the child directories
    if (contents != null) {
      for (int i=0; i<contents.length; i++) {
        if (contents[i].isDirectory()) {
          walk(contents[i]);
        }
      }
   }
  }
}

class ThumbsDirFilter implements FileFilter {
  public boolean accept(File file) {
    return file.isDirectory() && !file.getName().equals("thumbs");
  }
}

class JPEGFilenameFilter implements FilenameFilter {
  public boolean accept(File dir, String name) {
    name = name.toUpperCase();
    return name.endsWith("JPEG") || name.endsWith("JPG");
  }
}