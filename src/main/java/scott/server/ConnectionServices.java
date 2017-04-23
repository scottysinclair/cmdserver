package scott.server;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Scotty
 * Date: Aug 28, 2004
 * Time: 9:37:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionServices extends Thread {
  private Connection connection;

  public ConnectionServices(Connection connection) {
    super("connection class server");
    this.connection = connection;
    setDaemon(true);
  }

  public void run() {
    try {
      while(true) {
        Packet request = connection.receive();
        Packet response = null;
        String requestName = request.getName();
        if ("findclass".equals( requestName )) {
          response = doFindClass( request );
        }
        else if ("logging".equals( request)) {
          byte std[] = (byte[])request.getAttribute("stdout");
          if (std != null) {
            System.out.write(std);
          }
          std = (byte[])request.getAttribute("stderr");
          if (std != null) {
            System.err.write(std);
          }
          request.clear();
          response = request;
        }
        connection.send(response);
      }
    }
    catch(IOException x) {
      System.err.println("connection class server finshed: " + x.getMessage());
      x.printStackTrace();
    }
  }

  private Packet doFindClass(Packet findclass) {
    String className = findclass.getParameter("name");
    findclass.clear();
    try {
      byte classdata[] = findClass( className );
      if (classdata != null) {
        findclass.setAttribute("classdata", classdata);
        System.out.println("found class " + className);
      }
      else {
        findclass.setAttribute(Connection.EXCEPTION, new ClassNotFoundException( className ));
      }
    }
    catch(IOException x) {
      findclass.setAttribute(Connection.EXCEPTION, new ClassNotFoundException( className, x));
    }
    return findclass;
  }

  private byte[] findClass(String name) throws IOException {
    byte data[] = null;
    InputStream in = null;
    try {
      name = name.replace('.', '/');
      in = ConnectionServices.class.getResourceAsStream("/" + name + ".class");
      if (in != null) {
        data = IOUtil.readFully(in);
      }
    }
    catch(IOException x) {
      IOUtil.close(in);
      throw x;
    }
    return data;
  }
}
