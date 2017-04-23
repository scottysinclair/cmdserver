package scott.server;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Scotty
 * Date: Aug 28, 2004
 * Time: 9:22:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionClassLoader extends ClassLoader {
  private Connection connection;

  public ConnectionClassLoader(Connection connection){
//    super(Class.class.getClassLoader());
    this.connection = connection;
  }

  public Class findClass(String name) throws ClassNotFoundException {
    Console.out.println("ConnectonClassLoader find class");
    try {
      Packet findclass = new Packet("findclass");
      findclass.setParameter("name", name);
      findclass = connection.sendRequest(findclass);
      byte classdata[] = (byte[])findclass.getAttribute("classdata");
      Console.out.println("received class " + name);
      return defineClass(name, classdata, 0, classdata.length);
    }
    catch(IOException x) {
      throw new ClassNotFoundException("could not find class remotely", x);
    }
    catch(Connection.ConnectionException x) {
      if (x.getCause() instanceof ClassNotFoundException) {
        throw (ClassNotFoundException)x.getCause();
      }
      else {
        throw new ClassNotFoundException("could not get class", x.getCause());
      }
    }
  }
}
