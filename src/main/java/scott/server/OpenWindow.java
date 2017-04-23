package scott.server;

import java.io.*;
import java.util.*;
import java.text.*;
import javax.swing.*;

public class OpenWindow implements ServerTask {

    public static void main(String args[]) {
        try {
            //create a session to a server
            ClientSession session = new ClientSession(args[0], Integer.parseInt(args[1]));
            //create a new connection to the server
            Connection connection = session.newConnection("init");
            //establish services (classloading and later receive logging).
            session.establishServices();

            //send the server task to executeand recieve the response
      Packet execute = new Packet("execute");
      execute.setAttribute("execute", new OpenWindow());
      Packet pContents = connection.sendRequest(execute);
            connection.close();
        }
        catch(Exception x) {
      x.printStackTrace();
        }
    }



  public void run(Server server, ServerSession session, Connection connection) {
    try {
            JOptionPane.showMessageDialog(null, "What's eh crack eh day?", "a message from scott", JOptionPane.INFORMATION_MESSAGE);
            connection.send(new Packet("ok"));
    }
    catch(IOException x) {
      x.printStackTrace();
    }
  }

 }
