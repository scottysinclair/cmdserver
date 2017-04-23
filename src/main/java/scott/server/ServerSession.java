package scott.server;

import java.util.*;
import java.io.PrintStream;

public class ServerSession {
  private Server server;
	private String id = "?";
	private Map connections;
  private ClassLoader classLoader;
  public PrintStream out = System.out;
  public PrintStream err = System.err;

	private Map parameters;
	private Map attributes;


	public ServerSession(Server server, String id) {
    this.server = server;
		this.id = id;
		this.connections = new Hashtable();
		this.parameters = new HashMap();
		this.attributes = new HashMap();
	}

	public String getId() {
		return id;
	}

	public synchronized void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	public synchronized String getParameter(String name) {
		return (String)parameters.get(name);
	}

	public synchronized Iterator parameterNames() {
		return parameters.keySet().iterator();
	}

	public synchronized void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public synchronized Object getAttribute(String name) {
		return attributes.get(name);
	}

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    Iterator i = connections.values().iterator();
    while(i.hasNext()) {
      Connection connection = (Connection)i.next();
      if (!"classloader".equals( connection.getName() )) {
        connection.setClassLoader( classLoader );
      }
    }
  }



	public synchronized void addConnection(Connection connection) {
		connections.put(connection.getName(), connection);
	}

	public synchronized Connection getConnection(String name) {
		return (Connection)connections.get(name);
	}

	public synchronized boolean removeConnection(Connection connection) {
		return connections.remove(connection.getName()) != null;
	}

}