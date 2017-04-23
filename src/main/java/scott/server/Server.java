package scott.server;

import java.io.*;
import java.net.*;
import java.util.*;

/** a simple server which manages a server socket
 * subclase to set behaviour for handling new connections.
 *
 */
public class Server implements Runnable {

  private Map sessions;
	private ServerSocket serverSocket;
	private Map connectionThreads;
  private Map sessionsByThreadGroup;

	public Server(int port) throws IOException {
    sessions = new HashMap();
		serverSocket = new ServerSocket( port );
		connectionThreads = new HashMap();
    sessionsByThreadGroup = new HashMap();
	}

  public void setTransmitStreams(boolean transmitStreams) {

  }

	public void run() {
		try {
			while(true) {
				final Socket socket = serverSocket.accept();
        ThreadGroup tg = new ThreadGroup("connection " + socket.getLocalAddress());
				Thread t = new Thread(tg, "connection thread") { public void run() { newConnection( socket ); }};
				t.setDaemon(true);
				t.start();
			}
	  }
	  catch(IOException x) {
			Console.err.println(x);
		}
	}

  private void newConnection(Socket socket) {
		try {
			Connection connection = new Connection(socket);
			Packet sessionId = connection.receive();
			ServerSession session = getSession( sessionId.getParameter("id") );
      sessionId.setParameter("id", session.getId());
			session.addConnection( connection );

      Thread.currentThread().setName("connection " + connection.getName());
      associateThreadToSession(session);
      synchronized(connectionThreads) {
        connectionThreads.put(connection.getName(), new ServerConnection(connection, Thread.currentThread()));
      }

      boolean servicesConnection = Connection.SERVICES.equals(connection.getName());
      if (servicesConnection) {
        ConnectionClassLoader cl = new ConnectionClassLoader(connection);
        session.setClassLoader(cl);
        //session.transmitStreams();
      }
      connection.send(sessionId);

      if (!servicesConnection) {
        Packet serverTask = connection.receive();
        ServerTask task = (ServerTask)serverTask.getAttribute("execute");
        task.run(this, session, connection);
      }
		}
		catch(IOException x) {
			if (socket.isClosed()) {
				Console.err.println("socket closed");
			}
			else {
				x.printStackTrace();
			}
		}
	}

	public void shutdown() {
		IOUtil.close(serverSocket);
		synchronized(connectionThreads) {
			Iterator i = connectionThreads.values().iterator();
			while(i.hasNext()) {
				ServerConnection sc = (ServerConnection)i.next();
        sc.getConnection().close();
			}
			connectionThreads.clear();
		}
	}

  public ServerSession getServerSession() {
    synchronized(sessionsByThreadGroup) {
      return (ServerSession)sessionsByThreadGroup.get(Thread.currentThread().getThreadGroup());
    }
  }

  private void associateThreadToSession(ServerSession session) {
    synchronized(sessionsByThreadGroup) {
      sessionsByThreadGroup.put(Thread.currentThread().getThreadGroup(), session);
    }
  }

  private ServerSession getSession(String id) {
    ServerSession session = null;
    if ("?".equals( id )) {
        id = new Date().toString();
    }
    session = (ServerSession)sessions.get( id );
    if (session == null) {
        session = new ServerSession(this,id);
        sessions.put(session.getId(), session);
    }
    return session;
  }

  public ServerConnection getServerConnection(String name) {
    synchronized(connectionThreads) {
      return (ServerConnection)connectionThreads.get(name);
    }
  }


  public class ServerConnection {
    private Connection connection;
    private Thread thread;

    public ServerConnection(Connection connection, Thread thread) {
      this.connection = connection;
      this.thread = thread;
    }

    public Connection getConnection() {
      return connection;
    }

    public Thread getThread() {
      return thread;
    }
  }
}


