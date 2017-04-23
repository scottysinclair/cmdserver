package scott.server;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;

/**
 * Created by IntelliJ IDEA.
 * User: Scotty
 * Date: Aug 28, 2004
 * Time: 10:43:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClassLoadingObjectInputStream extends ObjectInputStream {

  private ClassLoader classLoader;

  public ClassLoadingObjectInputStream() throws IOException {
    super();
  }

  public ClassLoadingObjectInputStream(InputStream in) throws IOException {
    super(in);
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
    Class klass = null;
    try {
      klass = super.resolveClass(desc);
    }
    catch(ClassNotFoundException x) {
      if (classLoader != null) {
        klass =  classLoader.loadClass( desc.getName() );
      }
      else {
        throw x;
      }
    }
    catch(Throwable x) {
      x.printStackTrace();
    }
    return klass;
  }



}
