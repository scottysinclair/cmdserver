package scott.server;

import java.util.*;
import java.io.*;

public class ProcessControl {

	private List stdout;
	private List stderr;
	private OutputStream pin;
	private Process process;
	private Connection connection;

	public ProcessControl(Process process, Connection connection) {
		this.process = process;
		this.connection = connection;
		stdout = new ArrayList();
		stderr = new ArrayList();
		new Pumper(process.getInputStream(), stdout).start();
		new Pumper(process.getErrorStream(), stderr).start();
		pin = process.getOutputStream();
	}

	public void waitFor() {
		boolean finished = false;
		try  {
			while(!finished) {
				try {
					process.exitValue();
					finished = true;
				}
				catch(IllegalThreadStateException x) {
				}
				Packet logs = new Packet("logs");
				synchronized(stdout) {
					logs.setAttribute("stdout", new ArrayList(stdout));
					stdout.clear();
				}
				synchronized(stderr) {
					logs.setAttribute("stderr", new ArrayList(stderr));
					stderr.clear();
				}
				Packet response = connection.sendRequest(logs);
				String input = response.getParameter("input");
				if (input != null) {
					pin.write(input.getBytes());
					pin.flush();
				}
				try {
					Thread.sleep(600);
				}
				catch(InterruptedException x) {
					System.err.println("interrupt caught ProcessControl.waitFor()");
				}
			}
	 }
	 catch(IOException x) {
		 System.err.println("could not send logs to client " + x);
			try {
				process.waitFor();
			}
			catch(InterruptedException x2) {
				System.err.println("interrupt caught ProcessControl.waitFor()");
			}
	 }
	}


	class Pumper extends Thread {
		private LineNumberReader in;
		private List list;

		public Pumper(InputStream in, List list) {
			this.in = new LineNumberReader(new InputStreamReader(in));
			this.list = list;
		}

		public void run() {
			String line;
			try {
				while((line = in.readLine()) != null) {
					synchronized(list) {
						list.add(line);
					}
				}
		  }
		  catch(IOException x) {
				x.printStackTrace();
			}
		}
	}
}