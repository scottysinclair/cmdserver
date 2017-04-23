package scott.emdb;

import java.sql.*;
import java.util.*;
import java.io.*;
import com.sun.image.codec.jpeg.*;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class Example {
  private static final String URL = "jdbc:hsqldb:file:data\\emdb\\testdb";

  public static final class Create {
    public static void main(String args[]) {
      try {
        Class.forName("org.hsqldb.jdbcDriver" );
        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        //props.setProperty("ifexists", "true");
        Connection connection = DriverManager.getConnection(URL, props);
        Statement stmt = connection.createStatement();
        String sqlCreate = "create text table pictures(id integer, path varchar, thumb varbinary default null);";
        stmt.execute(sqlCreate);
        sqlCreate = "SET TABLE pictures SOURCE \"pictures\";";
        stmt.execute(sqlCreate);

        sqlCreate = "create text table keywords(picid integer, catid integer, value varchar);";
        stmt.execute(sqlCreate);
        sqlCreate = "SET TABLE keywords SOURCE \"keywords\";";
        stmt.execute(sqlCreate);

        sqlCreate = "create text table categories(id integer, name varchar, description varchar);";
        stmt.execute(sqlCreate);
        sqlCreate = "SET TABLE categories SOURCE \"categories\";";
        stmt.execute(sqlCreate);
      }
      catch(Exception x) {
        x.printStackTrace();
      }
    }
  }

  public static final class Insert {
    public static void main(String args[]) {
      try {
        Class.forName("org.hsqldb.jdbcDriver" );
        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("ifexists", "true");
        Connection connection = DriverManager.getConnection(URL, props);
        Statement stmt = connection.createStatement();

        String sqlInsert = "insert into categories values(1, 'Person', 'A person in the picture');";
        stmt.execute(sqlInsert);

        sqlInsert = "insert into categories values(2, 'Location', 'Where the picture was taken');";
        stmt.execute(sqlInsert);

        sqlInsert = "insert into categories values(3, 'Date', 'When the picture was taken');";
        stmt.execute(sqlInsert);

        sqlInsert = "insert into categories values(4, 'Object', 'An object in the picture');";
        stmt.execute(sqlInsert);


        sqlInsert = "insert into keywords values(1, 1, 'Anna');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(1, 1, 'Mum');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(1, 1, 'Dad');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(1, 2, 'Vienna');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(1, 3, '2004');";
                stmt.execute(sqlInsert);

                sqlInsert = "insert into keywords values(2, 1, 'Anna');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(2, 1, 'Scott');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(2, 2, 'Flat');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(2, 3, '2004');";
                stmt.execute(sqlInsert);

                sqlInsert = "insert into keywords values(3, 1, 'Anna');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(3, 1, 'Hat');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(3, 2, 'Flat');";
                stmt.execute(sqlInsert);
                sqlInsert = "insert into keywords values(3, 3, '2004');";
        stmt.execute(sqlInsert);

                stmt.close();
                PreparedStatement pstmt = connection.prepareStatement("insert into pictures values(?, ?, ?);");
                insertJPEG(pstmt, 1, new File("E:\\pics\\Anna\\S1010007.JPG"));
                insertJPEG(pstmt, 2, new File("E:\\pics\\Anna\\S1010002.JPG"));
                insertJPEG(pstmt, 3, new File("E:\\pics\\Anna\\P1010006.JPG"));
      }
      catch(Exception x) {
        x.printStackTrace();
      }
    }
  }

  private static void insertJPEG(PreparedStatement pstmt, int index, File file) throws SQLException, IOException {
        pstmt.setInt(1, index);
        pstmt.setString(2, file.getCanonicalPath());
        byte thumb[] = createThumb(file, 200);
        pstmt.setBinaryStream(3,new ByteArrayInputStream(thumb), thumb.length);
        pstmt.execute();
    }

  public static final class Select {
    public static void main(String args[]) {
      try {
        Class.forName("org.hsqldb.jdbcDriver" );
        Properties props = new Properties();
        props.setProperty("user", "sa");
        props.setProperty("password", "");
        props.setProperty("ifexists", "true");
        Connection connection = DriverManager.getConnection(URL, props);
        Statement stmt = connection.createStatement();

        String cat ="Person";
        String person1 ="Scott";
        String person2 ="Anna";
        String sql = "SELECT * FROM pictures pics WHERE " +
                        "EXISTS (SELECT * FROM categories cats, keywords keys WHERE cats.id=keys.catid and cats.name='" + cat + "' and keys.picid=pics.id and keys.value='" + person1 + "') AND " +
                        "EXISTS (SELECT * FROM categories cats, keywords keys WHERE cats.id=keys.catid and cats.name='" + cat + "' and keys.picid=pics.id and keys.value='" + person2 + "')";
        long time = System.currentTimeMillis();
        ResultSet rs = stmt.executeQuery(sql);
        JFrame output = new JFrame("results");
        Container c = output.getContentPane();
        c.setLayout(new FlowLayout());

        System.out.println(System.currentTimeMillis() - time + "millis");
        int numCols = rs.getMetaData().getColumnCount();
        while(rs.next()) {
                    Blob blob = rs.getBlob("thumb");
                    JButton b = new JButton(new ImageIcon(blob.getBytes(1l, (int)blob.length())));
                    b.setToolTipText(rs.getString("path"));
                    c.add(b);
              }
              output.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              output.setSize(600, 400);
              output.setLocation(300, 200);
              output.setVisible(true);
      }
      catch(Exception x) {
        x.printStackTrace();
      }
    }
  }

  public static final class GUI {
        private static JFrame frame;
    public static void main(String args[]) {
      try {
                Class.forName("org.hsqldb.jdbcDriver" );
                Properties props = new Properties();
                props.setProperty("user", "sa");
                props.setProperty("password", "");
                props.setProperty("ifexists", "true");
                Connection connection = DriverManager.getConnection(URL, props);
                final Statement stmt = connection.createStatement();


                JFrame gui = frame = new JFrame("Image Search v0.1");
                Container c = gui.getContentPane();
                JPanel north = new JPanel();
                final JPanel center = new JPanel();
                center.setLayout(new FlowLayout());
                c.add(north, BorderLayout.NORTH);
                c.add(new JScrollPane(center), BorderLayout.CENTER);

                north.setLayout(new BorderLayout());
                final JTextField textf = new JTextField();
                north.add(textf, BorderLayout.CENTER);
                textf.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ev) {
                        doSearch(stmt, center, textf.getText());
                    }
                    });


              gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              gui.setSize(600, 400);
              gui.setLocation(300, 200);
              gui.setVisible(true);
      }
      catch(Exception x) {
        x.printStackTrace();
      }
    }

    private static void doSearch(Statement stmt, JPanel center, String text)  {
            try {
                center.removeAll();
                center.setLayout(new FlowLayout());
                center.validate();
                frame.validate();
                StringBuffer sql = new StringBuffer("SELECT * FROM pictures pics WHERE 1=1 ");
                String parts[] = text.split("\\s+");
                for (int i=0; i<parts.length; i++) {
                    String s[] = parts[i].split("=");
                    if (s.length == 2) {
                        String category = s[0];
                        String value = s[1];
                        sql.append("AND EXISTS (SELECT * FROM categories cats, keywords keys WHERE cats.id=keys.catid and cats.name='" + category + "' and keys.picid=pics.id and keys.value='" + value + "') ");
                    }
                }
                System.out.println("performing :" + sql.toString());
                ResultSet rs = stmt.executeQuery(sql.toString());
                while(rs.next()) {
                    Blob blob = rs.getBlob("thumb");
                    JButton b = new JButton(new ImageIcon(blob.getBytes(1l, (int)blob.length())));
                    b.setToolTipText(rs.getString("path"));
                    center.add(b);
                }
                frame.validate();
         }
         catch(SQLException x) {
             x.printStackTrace();
         }
        }
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

    encoder.setJPEGEncodeParam(param);
    encoder.encode(scaled);
    out.flush();
    out.close();
    return out.toByteArray();
  }


}