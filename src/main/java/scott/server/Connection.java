package scott.server;

import java.io.*;
import java.net.*;

public final class Connection {

  public static final String EXCEPTION = "--EXCEPTION";
  public static final String ERROR = "--ERROR";
  public static final String SERVICES = "--SERVICES";

	public class ConnectionException extends java.lang.RuntimeException{
		public ConnectionException(java.lang.Exception e) {
			super(e);
		}
	}

	private Socket socket;
	private ClassLoadingObjectInputStream in;
	private ObjectOutputStream out;
	private String name;

	public Connection(Socket socket, String name) throws IOException {
			this.socket = socket;
			this.name = name;
			out= new ObjectOutputStream( new BufferedOutputStream( socket.getOutputStream() ) );
			out.flush();
			in = new ClassLoadingObjectInputStream( new BufferedInputStream( socket.getInputStream() ) );

			Packet register = new Packet("register");
			register.setParameter("name", name);
			Packet response = sendRequest(register);
	}

	public Connection(Socket socket) throws IOException {
			this.socket = socket;
			this.name = name;
			out= new ObjectOutputStream( new BufferedOutputStream( socket.getOutputStream() ) );
			out.flush();
			in = new ClassLoadingObjectInputStream( new BufferedInputStream( socket.getInputStream() ) );

			Packet register = (Packet)receive();
			this.name = register.getParameter("name");
			send(new Packet("register"));
	}

	public String getName() {
		return name;
	}

	public synchronized Packet sendRequest(Packet packet) throws IOException {
		try {
        out.writeObject(packet);
        out.flush();
			  return check((Packet)in.readObject());
	 }
	 catch(ClassNotFoundException x) {
		 throw new IOException("class not found " + x.getMessage());
	 }
	}

	public synchronized void send(Packet packet) throws IOException {
		out.writeObject(packet);
		out.flush();
	}

	public synchronized Packet receive() throws IOException {
	 try {
		return check((Packet)in.readObject());
	 }
	 catch(ClassNotFoundException x) {
		 throw new IOException("class not found " + x.getMessage());
	 }
	}

	public void sendStream(InputStream in) throws IOException {
		IOUtil.sendAll(in, out);
		receive();
	}

	public void receiveStream(OutputStream out, int length) throws IOException {
		IOUtil.send(in, out, length);
		send(new Packet("ok"));
	}

  public void setClassLoader(ClassLoader classLoader) {
    in.setClassLoader( classLoader );
  }

  public void close() {
    IOUtil.close(socket);
  }

	private Packet check(Packet packet) {
    Exception e = (Exception)packet.getAttribute( EXCEPTION );
		if (e != null) {
			throw new ConnectionException(e);
		}

		Error er = (Error)packet.getAttribute( ERROR );
		if (er != null) {
			throw er;
		}
		return packet;
	}

}
