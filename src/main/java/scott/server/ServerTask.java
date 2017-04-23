package scott.server;

/**
 * Created by IntelliJ IDEA.
 * User: Scotty
 * Date: Aug 29, 2004
 * Time: 1:54:06 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ServerTask extends java.io.Serializable {
  public void run(Server server, ServerSession session, Connection connection);
}
