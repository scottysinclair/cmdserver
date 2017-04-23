package scott.server;

import java.text.*;
import java.io.*;
import java.util.*;


public class ListAllFiles implements ServerTask, Serializable  {

	public static void main(String args[]) {

			File currentDir = new File(".");

			ClientSession session = new ClientSession(args[0], Integer.parseInt(args[1]));
			Connection connection = null;
			try {
				connection = session.newConnection("init");
		  	session.establishServices();

				String dir = args[2];
				for (int i=3; i<args.length; i++) dir += " " + args[i];

				Packet execute = new Packet("execute");
				execute.setAttribute("execute", new ListAllFiles());
				connection.sendRequest(execute);

				execute = new Packet("execute2");
				execute.setParameter("dir", dir);
				Packet response = connection.sendRequest(execute);
				if (response.getAttribute("error") != null) {
					throw (Throwable)response.getAttribute("error");
				}
				System.out.println(response.getParameter("list"));
	  }
	  catch(Throwable x) {
			x.printStackTrace();
		}
		finally {
			if (connection != null) {
				connection.close();
			}
		}
}


	public void run(Server server, ServerSession session, Connection connection) {
		try {
			connection.send(new Packet("ok"));
			Packet response = new Packet("response");
			try {
				System.out.println("running ListAllFiles");
				Packet command = connection.receive();
				File dir = new File(command.getParameter("dir"));
				if (dir.exists() == false) {
					throw new FileNotFoundException("directory does not exist");
				}
				if (dir.isFile() == true) {
					throw new FileNotFoundException("is a file");
				}

				System.out.println("listing " + dir);
				StringBuffer sb = new StringBuffer();
				for (Iterator i=listContents(dir, new ArrayList()).iterator(); i.hasNext();) {
					sb.append(((File)i.next()).getName() + "\n");
				}
				response.setParameter("list", sb.toString());
		  }
		  catch(Exception x) {
				response.setAttribute("error", x);
			}
			connection.send(response);
	 }
	 catch(Exception x) {
		 x.printStackTrace();
		}
	}

	private List listContents(File file, List list) {
		File f[] = file.listFiles();
		for (int i=0; i<f.length; i++) {
			if (f[i].isFile()) {
				list.add(f[i]);
			}
			else {
				listContents(f[i], list);
			}
		}
		return list;
	}
}