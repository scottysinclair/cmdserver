package scott.server;

import java.io.*;
import java.util.*;
import java.util.List;
import java.text.*;
import java.awt.*;
import java.awt.image.*;
import com.sun.image.codec.jpeg.*;
import javax.imageio.*;
import javax.swing.*;

import scott.image.*;

public class PictureViewer implements ServerTask {

	public static void main(String args[]) {
		try {
      if (args.length != 5) {
        System.out.println("usage: <host> <port> <output dir> <source dir> <width>");
        return;
      }


			ClientSession session = new ClientSession(args[0], Integer.parseInt(args[1]));
			Connection connection = session.newConnection("init");
			session.establishServices();

			LineNumberReader lin = new LineNumberReader( new InputStreamReader( System.in ) );
			File outputDir = new File(args[2]);
			outputDir.mkdirs();

			//send the server task to executeand recieve the response
      Packet execute = new Packet("execute");
      execute.setAttribute("execute", new PictureViewer());
      connection.sendRequest(execute);
      System.out.println("Connected to " + session.getHost());

      Packet request = new Packet("request");
      request.setParameter("dir", args[3]);
      request.setParameter("width", args[4]);
      connection.send(request);

      Packet inform = connection.receive();
      int num = Integer.parseInt(inform.getParameter("number"));
      System.out.println("downloading " + num + " thumbs");

      JFrame frame = new JFrame("Thumbs");
      Container c = frame.getContentPane();
      c.setLayout(new FlowLayout());

      for (int i=0; i<num; i++) {
				Packet thumb = connection.receive();
				System.out.println(thumb.getParameter("name"));
				System.out.println(thumb.getParameter("title"));
				System.out.println(thumb.getParameter("description"));
				System.out.println(thumb.getParameter("author"));
				System.out.println(thumb.getParameter("keywords"));
				System.out.println(thumb.getParameter("size"));
        System.out.println();

        byte image[] = (byte[])thumb.getAttribute("data");
        JButton b = new JButton(new ImageIcon(image));
        b.setToolTipText(thumb.getParameter("name"));
        c.add(b);
				File f = new File(outputDir, thumb.getParameter("name"));
				FileOutputStream out = new FileOutputStream(f);
				out.write(image);
				out.flush();
				out.close();

				connection.send(new Packet("ok"));
			}
      frame.setLocation(300, 300);
      frame.setSize(600, 400);
      frame.setVisible(true);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		catch(Exception x) {
			x.printStackTrace();
		}
	}


	private List jpegs;

	public PictureViewer() {
		jpegs = new ArrayList();
	}

  public void run(Server server, ServerSession session, Connection connection) {
    try {
			connection.send(new Packet("ok"));
			Packet request = connection.receive();
			File dir = new File(request.getParameter("dir"));
			int width = Integer.parseInt(request.getParameter("width"));
			File contents[] = dir.listFiles();
      if (contents != null) {
        for (int i=0; i<contents.length; i++) {
          if (contents[i].getName().endsWith("JPG")  ||
              contents[i].getName().endsWith("JPEG") ||
              contents[i].getName().endsWith("jpg")  ||
              contents[i].getName().endsWith("jpeg")) {

            jpegs.add(contents[i]);
          }
        }
      }

      Packet inform = new Packet("inform");
      inform.setParameter("number", "" + jpegs.size());
      connection.send(inform);

      Iterator i = jpegs.iterator();
      while(i.hasNext()) {
        File jpeg = (File)i.next();
        sendThumb(connection, jpeg, width);
      }
		}
		catch(IOException x) {
			x.printStackTrace();
		}
		catch(Exception x) {
			Packet error = new Packet("error");
			error.setAttribute(Connection.EXCEPTION, x);
			try {
				connection.send(error);
			}
			catch(IOException x2) {}
		}

	}

	private static void sendThumb(Connection connection, File jpeg, int width) throws IOException {
		Packet thumbp = new Packet("thumb");
		JPEGFile _jpeg = new JPEGFile(jpeg);
		thumbp.setParameter("name", jpeg.getName());
		JPEGFile.Photoshop ph = _jpeg.getPhotoshopSection();
		if (ph != null) {
			thumbp.setParameter("title", ph.getTitle());
			thumbp.setParameter("description", ph.getDescription());
			thumbp.setParameter("author", ph.getAuthor());
			String keywords[] = ph.getKeywords();
			if (keywords != null) {
				StringBuffer sb = new StringBuffer();
				for (int i=0; i<keywords.length-1; i++) {
					sb.append(keywords[i] + ",");
				}
				sb.append(keywords[keywords.length-1]);
				thumbp.setParameter("keywords", sb.toString());
			}
		}
		thumbp.setParameter("size", "" + jpeg.length());
		thumbp.setAttribute("data", createThumb(jpeg, width));
		connection.sendRequest(thumbp);
	}


  private static byte[] createThumb(File sourceFile, int width) throws IOException {
    BufferedImage bi = ImageIO.read(sourceFile);

    float scale = ((float)width / (float)bi.getWidth());
    int height = (int)((float)bi.getHeight() * scale);

    BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = scaled.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    graphics2D.drawImage(bi, 0, 0, width, height, null);

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
    JPEGEncodeParam param = encoder.
      getDefaultJPEGEncodeParam(scaled);

		int quality = 1;
    param.setQuality(quality, false);
/*    int quality = 50;
    param.setQuality((float)quality / 100.0f, false);
    */
    encoder.setJPEGEncodeParam(param);
    encoder.encode(scaled);
    out.flush();
    out.close();
    System.out.println("created thumb for " + sourceFile.getPath());
    return out.toByteArray();
  }
}