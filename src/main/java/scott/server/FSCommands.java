package scott.server;

import java.io.*;
import java.util.*;
import java.text.*;

public class FSCommands implements ServerTask {

	private static DateFormat df = new SimpleDateFormat("dd/MM/yyyy hh:mm a");

	public static void main(String args[]) {
		try {
			File localCurrentDir = new File(".");
			//create a session to a server
			ClientSession session = new ClientSession(args[0], Integer.parseInt(args[1]));
			//create a new connection to the server
			Connection connection = session.newConnection("init");
			//establish services (classloading and later receive logging).
			session.establishServices();

			//send the server task to executeand recieve the response
      Packet execute = new Packet("execute");
      execute.setAttribute("execute", new FSCommands());
      connection.sendRequest(execute);
      System.out.println("Connected to " + session.getHost() + ", local directory = " + localCurrentDir.getCanonicalPath());

			Packet command = new Packet("command");
			command.setParameter("command", "cd .");
			command = connection.sendRequest(command);
			String remotePrompt = command.getParameter("prompt");
			String localPrompt = localCurrentDir.getCanonicalPath() + ">";
			String prompt = remotePrompt;
			System.out.print(remotePrompt);

			LineNumberReader lin = new LineNumberReader(new InputStreamReader(System.in));
			String prefix = "";
			String input = null;
			boolean remote = true;
			while((input = lin.readLine()) != null && !"quit".equals(input)) {
				if (input.length() > 0) {
					if ("sw".equals(input)) {
						remote = !remote;
						if (remote) {
							prompt = remotePrompt;
							System.out.println("switched to remote computer");
						}
						else {
							prompt = localPrompt;
							System.out.println("switched to local computer");
						}
					}
					else if ("wh".equals(input)) {
						if (remote) {
							prompt = remotePrompt;
							System.out.println("remote computer");
						}
						else {
							prompt = localPrompt;
							System.out.println("local computer");
						}
					}
					else if (input.startsWith("receive") || input.startsWith("get")) {
						command = new Packet("command");
						command.setParameter("command", input);
						connection.send(command);
						handleReceive(connection, localCurrentDir);
						command = connection.receive();
						String output = command.getParameter("output");
						if (output != null) {
							System.out.println(output);
						}
					}
					else if (input.startsWith("send") || input.startsWith("put")) {
						command = new Packet("command");
						command.setParameter("command", input);
						connection.send(command);
						handleSend(input, connection, localCurrentDir);
						command = connection.receive();
						String output = command.getParameter("output");
						if (output != null) {
							System.out.println(output);
						}
					}
					else if (remote) {
						command = new Packet("command");
						command.setParameter("command", input);
						command = connection.sendRequest(command);
						String output = command.getParameter("output");
						if (output != null) {
							System.out.println(output);
						}
						prompt = remotePrompt = command.getParameter("prompt");
					}
					else {
						if (input.startsWith("cd")) {
							localCurrentDir = handleLCD(input, localCurrentDir);
						}
						else if (input.startsWith("pwd")) {
							handleLPWD(localCurrentDir);
						}
						else if (input.startsWith("dir") || input.startsWith("ls")) {
							System.out.println(handleDIR(input, localCurrentDir));
						}
						else if (input.startsWith("mkdir")) {
							System.out.println(handleMKDIR(input, localCurrentDir, "local"));
						}
						else if (input.startsWith("rm")) {
							System.out.println(handleRM(input, localCurrentDir));
						}
						prompt = localPrompt = localCurrentDir.getCanonicalPath() + ">";
					}
			  }
			  System.out.print(prompt);
			}
			command = new Packet("command");
			command.setParameter("command", "quit");
			command = connection.sendRequest(command);
			System.out.println(command.getParameter("output"));
			connection.close();
		}
		catch(Exception x) {
      x.printStackTrace();
		}
	}

	private static void handleLPWD(File localCurrentDir) throws IOException {
		System.out.println(localCurrentDir.getCanonicalPath() + File.separator);
	}

	private static String handleMKDIR(String input, File currentDir, String type) {
		String output = null;
		try {
			String parts[] = input.split("\\s+");
			if (parts == null || parts.length == 1) {
				output = "usage: mkdir <directory>";
			}
			else {
				File f = getPath(currentDir, parts[1]);
				if (f.exists()) {
					if (f.isFile()) {
						output = f.getCanonicalPath() + " is a file";
					}
					else if (f.exists() && f.isDirectory()) {
						output = f.getCanonicalPath() + " already exists";
					}
				}
				else {
				 if (!f.getParentFile().exists()) {
					 output = "parent directory does not exist";
				 }
				 else {
					 if (f.mkdir()) {
						output = "created " + type + " directory " + f.getCanonicalPath();
					 }
					 else {
						 output = "could not create " + type + " directory " + f.getCanonicalPath();
					 }
				 }
				}
			}
		}
		catch(Exception x) {
			output = x.toString();
		}
		return output;
	}

	private static File handleLCD(String input, File localCurrentDir) {
		try {
			String parts[] = input.split("\\s+", 2);
			if (parts == null || parts.length == 1) {
				System.out.println("usage: cd <directory>");
			}
			else {
				File f = getPath(localCurrentDir, parts[1]);
				if (!f.exists()) {
					System.out.println(f.getCanonicalPath() + " does not exist");
				}
				else if (f.isFile()) {
					System.out.println(f.getCanonicalPath() + " is a file");
				}
				else {
					localCurrentDir = f;
				}
			}
		}
		catch(IOException x) {
			System.out.println(x);
		}
		return localCurrentDir;
	}

	private static String handleRM(String input, File currentDir) {
		String output = null;
		String parts[] = input.split("\\s+");
		if (parts != null && parts.length > 1) {
			File file = getPath(currentDir, parts[1]);
			try {
				if (file.isDirectory()) {
					output = file.getCanonicalPath() + " is a directory";
				}
				else {
					if (file.delete()) {
						output = file.getCanonicalPath() + " removed";
					}
					else {
						output =  file.getCanonicalPath() + " could not be removed";
					}
				}
			}
			catch(IOException x) {
				output = x.toString();
			}
		}
		return output;
	}

	private static String handleDIR(String input, File currentDir) {
		String output = null;
		try {
			String parts[] = input.split("\\s+", 2);
			File f;
			if (parts != null && parts.length > 1) {
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

	private static void handleSend(String input, Connection connection, File localCurrentDir) throws IOException {
		String parts[] = input.split("\\s+", 2);
		if (parts != null && parts.length > 1) {
			InputStream in = null;
			long start = System.currentTimeMillis();
			File f = new File(localCurrentDir, parts[1]);
			if (f.exists()) {
				try {
					Packet streamHeader = new Packet("streamHeader");
					streamHeader.setParameter("name", f.getName());
					streamHeader.setParameter("length", "" + f.length());
					connection.send(streamHeader);
					in = new BufferedInputStream( new FileInputStream( f ) );
					connection.sendStream(in);
					System.out.println("sent file " + f.getName() + " " + f.length() + " bytes in " + ((double)(System.currentTimeMillis() - start) / 1000.0) + " seconds");
				}
				catch(IOException x) {
					throw x;
				}
				finally {
					IOUtil.close(in);
				}
		  }
		  else {
				Packet streamHeader = new Packet("cancelled");
				connection.send(streamHeader);
				System.out.println("file " + f.getCanonicalPath() + " does not exist");
			}
		}
		else {
			System.out.println("no file specified");
		}
	}

	private static void handleReceive(Connection connection, File localCurrentDir) throws IOException {
		Packet streamHeader = connection.receive();
		if (!"cancelled".equals( streamHeader.getName() )) {
			File file = new File(localCurrentDir, streamHeader.getParameter("name"));
			int length = Integer.parseInt(streamHeader.getParameter("length"));
			System.out.println("receiving " + file.getName() + " of size " + length + " bytes");
			OutputStream out = null;
			try {
				out = new BufferedOutputStream(new FileOutputStream(file));
				connection.receiveStream(out, length);
			}
			catch(IOException x) {
				throw x;
			}
			finally {
				IOUtil.close(out);
			}
		}
	}

	private static File getPath(File currentDir, String path) {
		File f;
		if (path.length() > 2 && path.charAt(1) == ':') {
			f = new File(path);
		}
		else {
			f = new File(currentDir, path);
		}
		return f;
	}

  public void run(Server server, ServerSession session, Connection connection) {
    try {
			connection.send(new Packet("ok"));
      System.out.println("executing FSCommandsTask task");
      File currentDir = new File(".");
      while(true) {
				Packet pCommand = connection.receive();
				Packet response = new Packet("response");
				String command = pCommand.getParameter("command");
				if (command == null) {
					command = "";
				}
				command = command.trim();
				if (command.startsWith("receive") || command.startsWith("get")) {
					handleReceive(connection, currentDir, command, response);
				}
				else if (command.startsWith("dir") || command.startsWith("ls")) {
					handleDir(currentDir, command, response);
				}
				else if (command.startsWith("cd")) {
					currentDir = handleCD(currentDir, command, response);
				}
				else if (command.startsWith("rm")) {
					handleRM(currentDir, command, response);
				}
				else if (command.startsWith("quit")) {
					break;
				}
				else if (command.startsWith("mkdir")) {
					handleMKDIR(currentDir, command, response);
				}
				else if (command.startsWith("send") || command.startsWith("put")) {
					handleSend(connection, currentDir, command, response);
				}
				else {
					response.setParameter("output", "unknown command");
			  }
			  response.setParameter("prompt", currentDir.getCanonicalPath() + ">");
			  connection.send(response);
			}
			Packet response = new Packet("response");
			response.setParameter("output", "server quit");
			connection.send(response);
    }
    catch(IOException x) {
      x.printStackTrace();
    }
  }

  private void handleRM(File currentDir, String command, Packet response) throws IOException {
		response.setParameter("output", handleRM(command, currentDir));
	}

  private void handleSend(Connection connection, File currentDir, String command, Packet response) throws IOException {
		OutputStream out = null;
		try {
			Packet streamHeader = connection.receive();
			if (!"cancelled".equals(streamHeader.getName())) {
				File file = new File(currentDir, streamHeader.getParameter("name"));
				int length = Integer.parseInt(streamHeader.getParameter("length"));
				out = new BufferedOutputStream(new FileOutputStream(file));
				connection.receiveStream(out, length);
			}
		}
		catch(FileNotFoundException x) {
			response.setParameter("output", x.getMessage());
		}
		finally {
			IOUtil.close(out);
		}
	}


  private void handleReceive(Connection connection, File currentDir, String command, Packet response) throws IOException  {
		InputStream in = null;
		try {
			String parts[] = command.split("\\s+", 2);
			if (parts != null && parts.length > 1) {
				long start = System.currentTimeMillis();
				File f = new File(currentDir, parts[1]);
				if (f.exists()) {
					System.out.println("current dir " + currentDir);
					Packet streamHeader = new Packet("streamHeader");
					streamHeader.setParameter("name", f.getName());
					streamHeader.setParameter("length", "" + f.length());
					connection.send(streamHeader);
					in = new BufferedInputStream( new FileInputStream( f ) );
					connection.sendStream( in );
					response.setParameter("output", "sent file " + f.getName() + " " + f.length() + " bytes in " + ((double)(System.currentTimeMillis() - start) / 1000.0) + " seconds");
				}
				else {
					Packet streamHeader = new Packet("cancelled");
					connection.send(streamHeader);
					response.setParameter("output", f.getCanonicalPath() + " does not exist");
				}
			}
			else {
				response.setParameter("output", "no file specified");
			}
		}
		catch(FileNotFoundException x) {
			response.setParameter("output", x.getMessage());
		}
		finally {
			IOUtil.close(in);
		}
	}

  private void handleDir(File currentDir, String command, Packet response) {
		response.setParameter("output", handleDIR(command, currentDir));
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

  private File handleCD(File currentDir, String command, Packet response) {
		try {
			String parts[] = command.split("\\s+", 2);
			if (parts == null || parts.length == 1) {
				System.out.println("usage: cd <directory>");
			}
			else {
				File f = getPath(currentDir, parts[1]);
				if (f.exists() && f.isDirectory()) {
					currentDir = f.getCanonicalFile();
				}
				else if (f.exists() && f.isFile()) {
					response.setParameter("output", f.getName() + " is a file");
				}
				else {
					response.setParameter("output", f.getName() + " does not exist");
				}
			}
		}
		catch(IOException x) {
			response.setParameter("output", x.getMessage());
		}
		return currentDir;
	}


  private void handleMKDIR(File currentDir, String command, Packet response) {
		response.setParameter("output", handleMKDIR(command, currentDir, "remote"));
	}

 }
