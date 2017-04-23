package scott.server;

import java.io.*;
import java.net.*;

public class IOUtil {

  public static void close(ServerSocket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    }
    catch(IOException x) {}
  }

  public static void close(Socket socket) {
    try {
      if (socket != null) {
        socket.close();
      }
    }
    catch(IOException x) {}
  }

  public static void close(InputStream in) {
    try {
      if (in != null) {
        in.close();
      }
    }
    catch(IOException x) {}
  }

  public static void close(OutputStream out) {
    try {
      if (out != null) {
        out.close();
      }
    }
    catch(IOException x) {}
  }

  public static void readFully(InputStream in, byte buf[]) throws IOException {
		int soFar = 0;
		int len = 0;
		while((len = in.read(buf, soFar, len-soFar)) != -1) {
			soFar += len;
		}
	}

  public static byte[] readFully(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte buf[] = new byte[1024];
    int len = 0;
    while((len=in.read(buf)) != -1) {
      out.write(buf, 0,len);
    }
    return out.toByteArray();
  }

  public static void sendAll(InputStream in, OutputStream out) throws IOException {
		byte buf[] = new byte [1024];
		int len = 0;
		while((len = in.read(buf)) != -1) {
			out.write(buf, 0, len);
			out.flush();
		}
	}

  public static void send(InputStream in, OutputStream out, int length) throws IOException {
		byte buf[] = new byte [1024];
		int len = 0;
		int amountToRead =  length  > buf.length ? buf.length : length;
		while(length > 0 && (len = in.read(buf, 0, amountToRead)) != -1) {
			out.write(buf, 0, len);
			out.flush();
			length -= len;
			amountToRead = length  > buf.length ? buf.length : length;
			System.out.println("read " + len);
		}
	}


}