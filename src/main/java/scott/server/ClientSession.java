package scott.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

public class ClientSession {
	private String host;
	private int port;
	private String id = "?";
	private Map connections;

	public ClientSession(String host, int port) {
		this.host = host;
		this.port = port;
		this.connections = new HashMap();
	}

	public String getHost() {
		return host;
	}

	public Connection newConnection(String name) throws IOException {
		Connection con = new Connection(new Socket(host, port), name);
    establishSession(con);
		connections.put(name, con);
		return con;
	}

  public void establishServices() throws IOException {
    Connection con = newConnection( Connection.SERVICES );
    ConnectionServices ccs = new ConnectionServices(con);
    ccs.start();
  }

  private void establishSession(Connection connection) throws IOException {
    Packet sessionId = new Packet("sessionid");
    sessionId.setParameter("id", id);
    sessionId = connection.sendRequest( sessionId );
    if ("?".equals(id)) {
      this.id = sessionId.getParameter("id");
    }
 }



}