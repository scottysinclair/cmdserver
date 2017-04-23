package scott.server;

import java.text.*;
import java.io.*;
import java.util.*;

public class FSRemote implements ServerTask, Serializable  {

	private static DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

	public static void main(String args[]) {
		try {
			File currentDir = new File(".");

			//create a session to a server
			ClientSession session = new ClientSession(args[0], Integer.parseInt(args[1]));
			//create a new connection to the server
			Connection connection = session.newConnection("init");
			//establish services (classloading and later receive logging).
			session.establishServices();

			//send the server task to executeand recieve the response
      Packet execute = new Packet("execute");
      execute.setAttribute("execute", new FSRemote());
      connection.sendRequest(execute);
      System.out.println("connected");

      LineNumberReader lin = new LineNumberReader( new InputStreamReader( System.in ) );
      String input;
      boolean remote = true;
			Packet pfirst = new Packet("command");
			pfirst.setParameter("input", "cd .");
			pfirst = connection.sendRequest(pfirst);
			String remotePrompt = pfirst.getParameter("dir") + ">";
      String localPrompt = currentDir.getCanonicalPath() + ">";
      String prompt = remotePrompt;
      System.out.print(prompt);
      while((input = lin.readLine()) != null && !"quit".equals(input)) {
				if (input.length() == 0) {
					System.out.print(prompt);
					continue;
				}
				try {
					if (input.startsWith("sw")) {
						remote = !remote;
						if (remote) {
							prompt = remotePrompt;
							System.out.println("switched to remote machine");
						}
						else {
							prompt = localPrompt;
							System.out.println("switched to local machine");
						}
						try {
							input = input.split("\\s+", 2)[1];
						}
						catch(ArrayIndexOutOfBoundsException x) {
							System.out.print(prompt);
							continue;
						}
					}
					if (input.startsWith("wh")) {
						if (remote) {
							prompt = remotePrompt;
							System.out.println("remote machine");
						}
						else {
							prompt = localPrompt;
							System.out.println("local machine");
						}
						System.out.print(prompt);
						continue;
					}
					if (input.startsWith("send")) {
						handleSend(input, connection, currentDir);
					}
					else if (remote) {
						Packet command = new Packet("command");
						command.setParameter("input", input);
						command = connection.sendRequest(command);
						CommandResponse response = (CommandResponse)command.getAttribute("response");
						if (response != null) {
							command = response.process(connection, currentDir, command);
							prompt = remotePrompt = command.getParameter("dir") + ">";
						}
					}
					else {
						if (input.startsWith("cd")) {
							try {
								currentDir = processCD(input, currentDir);
							}
							catch(IOException x) {
								System.out.println(x);
							}
						}
						else if (input.startsWith("dir") || input.startsWith("ls")) {
							System.out.println(processDIR(input, currentDir));
						}
						else if (input.startsWith("mkdir")) {
							System.out.println(processMKDIR(input, currentDir));
						}
						else if (input.startsWith("rmdir")) {
							System.out.println(processRMDIR(input, currentDir));
						}
						prompt = currentDir.getCanonicalPath() + ">";
					}
			  }
			  catch(Connection.ConnectionException x) {
					x.printStackTrace();
				}
				System.out.print(prompt);
			}
			Packet command = new Packet("command");
			command.setParameter("input", "quit");
			connection.sendRequest(command);
			CommandResponse response = (CommandResponse)command.getAttribute("response");
			connection.close();
		}
		catch(Exception x) {
      x.printStackTrace();
		}
	}

	private static void handleSend(String input, Connection connection, File currentDir) throws IOException {
		System.out.println(transmitFile(input, connection, currentDir));
		System.out.print(currentDir.getCanonicalPath() + ">");
	}

	private File currentDir;
	private Server server;
	private ServerSession session;
	private Connection connection;


  public void run(Server server, ServerSession session, Connection connection) {
   try {
		 	this.server = server;
		 	this.session = session;
		 	this.connection = connection;
		 	this.currentDir = new File(".");

			connection.send(new Packet("ok"));
			Packet command = connection.receive();
			String input = command.getParameter("input");
      while(!"quit".equals(input)) {
				try {
					if (input.startsWith("cd")) {
						serveCD(input);
					}
					else if (input.startsWith("dir") || input.startsWith("ls")) {
						serveDir(input);
					}
					else if (input.startsWith("send") || input.startsWith("put")) {
						serveSend(command);
					}
					else if (input.startsWith("receive") || input.startsWith("get")) {
						serveReceive(input);
					}
					else if (input.startsWith("exec")) {
						serveExec(input);
					}
					else if (input.startsWith("mkdir")) {
						serveMkdir(input);
					}
					else if (input.startsWith("rmdir")) {
						serveRmdir(input);
					}
					else {
						serveUnknown(input);
					}
				}
				catch(IOException x) {
					throw x;
				}
				catch(Exception x) {
					Packet response = new Packet("response");
					response.setAttribute(Connection.EXCEPTION, x);
					connection.send(response);
				}
				catch(Error x) {
					Packet response = new Packet("response");
					response.setAttribute(Connection.ERROR, x);
					connection.send(response);
				}
			  command = connection.receive();
			  input = command.getParameter("input");
			}
			connection.send(new Packet("quit"));
			connection.close();
	 }
	 catch(Exception x) {
		 x.printStackTrace();
	 }
 }

 private Packet newResponse(CommandResponse cmd, String message) {
	 Packet response = new Packet("response");
	 response.setAttribute("response", cmd);
	 response.setParameter("output", message);
	 try {
	 	response.setParameter("dir", currentDir.getCanonicalPath());
 	 }
 	 catch(IOException x) {
		 response.setParameter("dir", "");
		 x.printStackTrace();
 	 }
	 return response;
 }


 private void serveUnknown(String input) throws IOException {
	 Packet response = newResponse(new CommandResponse(), "unknown command " + input);
	 connection.send(response);
 }

 private void serveCD(String input) throws IOException {
	 String message = null;
	 try {
	 	currentDir = processCD(input, currentDir);
 	 }
 	 catch(Exception x) {
		 message = x.getMessage();
	 }
	 Packet response = newResponse(new CommandResponse(), message);
	 connection.send(response);
 }


 private void serveDir(String input) throws IOException  {
   String message = processDIR(input, currentDir);
	 connection.send(newResponse(new CommandResponse(), message));
 }

 //receives a file on the client
 private void serveReceive(String input) throws IOException {
	 String message;
	 try {
		 //client is receiving, we are transmitting
		 message = transmitFile(input, connection, currentDir);
	 }
	 catch(IOException x) {
		 throw x;
	 }
	 catch(Exception x) {
		 message = x.getMessage();
	 }
	 Packet response = newResponse(new CommandResponse(), message);
	 connection.send(response);
 }

 //receives a file on the server
 private void serveSend(Packet packetIn) {
	 receiveFile(connection, currentDir, packetIn);
 }


 private void serveExec(String input) throws IOException {
	 String parts[] = input.split("\\s+");
	 String command[] = new String [ parts.length - 1 ];
	 System.arraycopy(parts, 1, command, 0, command.length);
	 Process process = null;
	 try {
		 process = Runtime.getRuntime().exec(command);
	 }
	 catch(IOException x) {
		 connection.send(newResponse(new CommandResponse(), "could not create process"));
		 return;
	 }
	 connection.send(newResponse(new ExecProcess(), "ok"));
	 ProcessControl ctrl = new ProcessControl(process, connection);
	 ctrl.waitFor();
	 Packet packet = new Packet("finished");
	 connection.send(packet);
 }


 private void serveMkdir(String input) throws IOException {
   String message = processMKDIR(input, currentDir);
	 connection.send(newResponse(new CommandResponse(), message));
 }

 private void serveRmdir(String input) throws IOException {
   String message = processRMDIR(input, currentDir);
	 connection.send(newResponse(new CommandResponse(), message));
 }

 private static String processMKDIR(String input, File currentDir)  {
	 try {
		 String parts[] = input.split("\\s+", 2);
		 if (parts.length != 2) {
			 return "usage: mkdir <directory>";
		 }
 		 File f = getPath(currentDir, parts[1]);
		 if (f.exists() && f.isDirectory()) {
		  	return "directory " + f.getCanonicalPath() + " already exists";
		 }
		 if (f.exists() && f.isFile()) {
		 	 return f.getCanonicalPath() + " is a file";
		 }
		 if (f.getParentFile().exists() == false) {
			 return "parent directory does not exist";
		 }
		 if (f.mkdir() == false) {
			 return "could not create directory, reason unknown";
		 }
	 }
	 catch(IOException x) {
		 return "error: " + x.getMessage();
	 }
	return null;
 }

 private static String processRMDIR(String input, File currentDir)  {
	 try {
		 String parts[] = input.split("\\s+", 2);
		 if (parts.length != 2) {
			 return "usage: rmdir <directory>";
		 }
 		 File f = getPath(currentDir, parts[1]);
		 if (!f.exists()) {
		  	return "directory " + f.getCanonicalPath() + " does not exist";
		 }
		 if (f.exists() && f.isFile()) {
		 	 return f.getCanonicalPath() + " is a file";
		 }
		 if (f.list().length > 0) {
			 return "directory is not empty";
		 }
		 if (f.delete() == false) {
			 return "could not delete directory, reason unknown";
		 }
	 }
	 catch(IOException x) {
		 return "error: " + x.getMessage();
	 }
	return null;
 }

 private static File processCD(String input, File currentDir) throws IOException {
 	 String parts[] = input.split("\\s+", 2);
	 if (parts.length != 2) {
	   throw new IOException("usage: cd <directory>");
	 }
	 else {
		File f = getPath(currentDir, parts[1]);
		if (!f.exists()) {
			throw new IOException("Directory " + f.getCanonicalPath() + " does not exist");
		}
		else if (f.isFile()) {
			throw new IOException(f.getCanonicalPath() + " is a file");
		}
		else {
			currentDir = f;
		}
	}
	return currentDir;
 }

 private static String processDIR(String input, File currentDir)  {
		String output = null;
		try {
			String parts[] = input.split("\\s+", 2);
			File f;
			if (parts.length > 1) {
				f = getPath(currentDir, parts[1]);
			}
			else {
				f = currentDir;
			}
			f = f.getCanonicalFile();
			File contents[] = f.listFiles();
			if (contents != null) {
				int longestName = getLongestName(contents);
				StringBuffer sb = new StringBuffer();
				sb.append("\n  Directory of " + f + "\n\n");
				int files = 0;
				int dirs = 0;
				for (int i=0; i<contents.length; i++) {
					if (contents[i].isFile()) {
						files++;
						sb.append(pad(contents[i].getName(), longestName) + "\t" + formatLength(contents[i], 15) + "\t" + df.format(new Date(contents[i].lastModified())) + "\n");
					}
					else {
						dirs++;
						sb.append(pad(contents[i].getName(), longestName) + "\t" + pad("", 15) + "\t" + df.format(new Date(contents[i].lastModified())) + "\n");
					}
				}
				sb.append("\t\t" + files + " File(s) and " + dirs + " Dir(s)");
				output = sb.toString();
			}
			else {
				output = "cannot get contents of directory " + currentDir.getCanonicalPath();
			}
	  }
	  catch(Exception x) {
			output = x.getMessage();
		}
		return output;
 }

 public static File getPath(File currentDir, String path) {
	File f;
	if (path.length() > 2 && path.charAt(1) == ':') {
		f = new File(path);
	}
	else {
		f = new File(currentDir, path);
	}
	return f;
 }


	private static String formatLength(File file, int maxLength) {
		double length = (double)file.length() / 1024.0;
		String type = "KB";

		String strLen = ("" + length);
		int i = strLen.indexOf(".");
		if ((i + 3) < strLen.length()) {
			strLen = strLen.substring(0, i + 3);
		}
		return pad(strLen + " " + type, maxLength);
	}

	private static String pad(String str, int length) {
		StringBuffer sb = new StringBuffer(str);
		int extra = length - str.length();
		for (int i=0; i<extra; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}

	private static int getLongestName(File contents[]) {
		int length = 0;
		for (int i=0; i<contents.length; i++) {
			int nameLength = contents[i].getName().length();
			if (length < nameLength) {
				length = nameLength;
			}
		}
		return length;
	}

 private static String transmitFile(String input, Connection connection, File currentDir) throws IOException {
	 String parts[] = input.split("\\s+", 2);
	 if (parts.length != 2) {
	   return "usage: send <file>";
	 }
	 else {
		 File f = getPath(currentDir, parts[1]);
		 if (f.exists() == false) {
			 return "file " + f.getPath() + " does not exist";
		 }
		 CommandResponse cmdResponse = new ReceiveFile();
		 try {
			 Packet header = new Packet("header");
			 header.setParameter("input", input);
			 header.setAttribute("response", cmdResponse);
			 header.setParameter("filename", f.getName());
			 header.setParameter("length", "" + f.length());
			 connection.sendRequest(header);
			 InputStream in = new BufferedInputStream( new FileInputStream( f ) );
			 connection.sendStream(in);
			 IOUtil.close(in);
			 return "sent " + f.getName() + " (" + f.length() + " bytes)";
		 }
		 catch(Connection.ConnectionException x) {
			 return x.toString();
	 	 }
	 }
 }

 public static void receiveFile(Connection con, File cDir, Packet packetIn) {
	 try {
		 String fileName = packetIn.getParameter("filename");
		 if (fileName == null) {
			 System.out.println("did not receive file name");
			 packetIn.setAttribute(Connection.EXCEPTION, new IOException("did not receive file name"));
			 con.send(packetIn);
			 return;
		 }
		 File file = FSRemote.getPath(cDir, fileName);
		 if (file.exists()) {
			 System.out.println("file " + file.getName() + " already exists");
			 packetIn.setAttribute(Connection.EXCEPTION, new IOException("file already exists"));
			 con.send(packetIn);
			 return;
		 }
		 int length = 0;
		 try {
			 length = Integer.parseInt(packetIn.getParameter("length"));
		 }
		 catch(Exception x) {
			 System.out.println("could not read file length");
			 packetIn.setAttribute(Connection.EXCEPTION, new IOException("could not read file length"));
			 con.send(packetIn);
			 return;
		 }
		 if (length <= 0) {
			 System.out.println("bad file length " + length);
			 packetIn.setAttribute(Connection.EXCEPTION, new IOException("bad file length " + length));
			 con.send(packetIn);
			 return;
		 }
		 packetIn.clear();
		 con.send(packetIn);
		 OutputStream out = new BufferedOutputStream( new FileOutputStream( file ) );
		 con.receiveStream(out, length);
		 IOUtil.close(out);
		 System.out.println("received " + file.getName() + " (" + length + " bytes)");
	 }
	 catch(Exception x) {
		 System.out.println("could not receive file: " + x);
	 }
 }


}

class CommandResponse implements Serializable {
	public Packet process(Connection connection, File currentDir, Packet response) throws IOException {
		String message = response.getParameter("output");
		if (message != null) {
			System.out.println(message);
		}
		return response;
	}
}


class ReceiveFile extends CommandResponse {
	 public Packet process(Connection con, File cDir, Packet response) throws IOException {
		 FSRemote.receiveFile(con, cDir, response);
		 return con.receive();
	 }
}

class ExecProcess extends CommandResponse {
	 public Packet process(Connection con, File cDir, Packet response) throws IOException {
		 StdinThread stdIn = new StdinThread();
		 stdIn.start();
		 Packet packet;
		 while(!"finished".equals((packet = con.receive()).getName())) {
			 if ("logs".equals(packet.getName())) {
				 Iterator i = ((List)packet.getAttribute("stdout")).iterator();
				 while(i.hasNext()) {
					 System.out.println((String)i.next());
				 }
				 i = ((List)packet.getAttribute("stderr")).iterator();
				 while(i.hasNext()) {
					 System.err.println((String)i.next());
				 }
			 }

			 Packet reply = new Packet("stdin");
			 String input = stdIn.getInput();
			 stdIn.clearInput();
			 reply.setParameter("input", input);
			 con.send(reply);
		 }
		stdIn.finish();
		return response;
	 }
}


class StdinThread extends Thread {
	private List input;
	private boolean finished;

	public void finish() {
		finished = true;
	}

	public void run() {
		try {
			input = new ArrayList();
			LineNumberReader lin = new LineNumberReader( new InputStreamReader( System.in ) );
			while(!finished) {
				String in = lin.readLine() + "\n";
				synchronized(input) {
					input.add(in);
				}
			}
		}
		catch(IOException x) {
			x.printStackTrace();
		}
	}

	public String getInput() {
		StringBuffer sb = new StringBuffer();
		synchronized(input) {
			Iterator i = input.iterator();
			while(i.hasNext()) {
				sb.append((String)i.next());
			}
		}
		return sb.toString();
	}

	public void clearInput() {
		synchronized(input) {
			input.clear();
		}
	}
}