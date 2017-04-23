package scott.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestServer {

	public static void main(String args[]) {
		try {
			Server server = new Server( Integer.parseInt( args[0] ));
			server.run();
		}
		catch(Exception x) {
      x.printStackTrace(Console.err);
		}
	}
}
