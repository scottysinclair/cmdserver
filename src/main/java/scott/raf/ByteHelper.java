package scott.raf;

import java.io.*;

public class ByteHelper {

	public static int INT_SIZE = 4;
/*
	public static void writeByte(FileAccess fa, int location, int b) throws
IOException {
		synchronized(fa) {
			fa.getRAF().seek( location );
			fa.getRAF().writeByte( b );
		}
	}
*/
	public static int[] readInt(RandomAccessFile fa, int location, int data[])
throws IOException {
		synchronized(fa) {
			fa.seek( location );
			for (int i=0; i<data.length; i++) {
				data[i] = fa.readInt();
			}
		}
		return data;
	}
/*
	public static void writeInt(FileAccess fa, int location, int data[]) throws
IOException {
		synchronized(fa) {
			fa.getRAF().seek( location );
			for (int i=0; i<data.length; i++) {
				fa.getRAF().writeInt( data[i] );
			}
		}
	}

	public static void copyBytes(byte from[], byte to[], int offset) throws
IOException {
		for (int i=0; i<from.length; i++) {
			to[i] = from[i+offset];
		}
	}

	public static String readString(FileAccess fa, int location) throws IOException
{
		synchronized(fa) {
			fa.getRAF().seek( location );
			int size = readInt(fa, location );
			byte stringdata[] = new byte [size];
			readBytes(fa, location + INT_SIZE, stringdata, 0, size);
			return new String( stringdata );
		}
	}

	public static String getString(byte bytes[], int location) {
		int size = getInt(bytes, location );
		byte stringdata[] = new byte [size];
		for (int i=0; i<stringdata.length; i++) {
			stringdata[i] = bytes[i + INT_SIZE];
		}
		return new String( stringdata );
	}

	public static char Char(byte bytes[]) {
	  return (char)((bytes[1] << 8) + (bytes[0] << 0));
	}

	public static void writeInt(FileAccess fa, int position, int value) throws
IOException {
		synchronized(fa) {
			fa.getRAF().seek( position );
			fa.getRAF().writeInt(value);
		}
	}

	public static void writeInt(byte to[], int position, int value) throws
IOException {
		to[position] = (byte)((value >>> 24) & 0xFF);
		to[position+1] = (byte)((value >>> 16) & 0xFF);
		to[position+2] = (byte)((value >>> 8) & 0xFF);
		to[position+3] = (byte)((value >>> 0) & 0xFF);
	}
*/
	public static byte[] mergeBytes(byte bytes[][]) {
		int length = 0;
		for (int i=0; i<bytes.length; i++) {
			length += bytes[i].length;
		}

		byte merged[] = new byte [ length ];
		int index = 0;
		for (int i=0; i<bytes.length; i++) {
			for (int j=0; j<bytes[i].length; j++) {
				merged[index++] = bytes[i][j];
			}
		}

		return merged;
	}
/*
	public static void readBytes(FileAccess fa, int position, byte data[], int
offset, int length) throws IOException {
		synchronized(fa) {
			fa.getRAF().seek( position );
			fa.getRAF().read(data, offset, length);
		}
	}

	public static void readBytes(byte bytes[], int position, byte data[], int
offset, int length) {
		int end = offset + length;
		for (int i=offset; i<end; i++) {
			data[i] = bytes[position + (i - offset)];
		}
	}
*/
	public static byte[] getBytes(int value) {
		byte data[] = new byte[4];

		data[0] = (byte)((value >>> 24) & 0xFF);
		data[1] = (byte)((value >>> 16) & 0xFF);
		data[2] = (byte)((value >>> 8) & 0xFF);
		data[3] = (byte)((value >>> 0) & 0xFF);

		return data;
	}

	public static int readInt(RandomAccessFile fa, int position) throws IOException
{
		synchronized(fa) {
			fa.seek( position );
			return fa.readInt();
		}
	}
/*
	public static byte readByte(FileAccess fa, int position) throws IOException {
		synchronized(fa) {
			fa.getRAF().seek( position );
			return fa.getRAF().readByte();
		}
	}


	public static int getInt(byte data[]) {
		return getInt(data, 0);
	}
	public static int getInt(byte data[], int position) {
		int value = 0;

		value = (data[position + 0] & 0xFF) << 24;

		value += (data[position + 1] & 0xFF) << 16;

		value += (data[position + 2]  & 0xFF)<< 8;

		value += (data[position + 3]  & 0xFF) << 0;

		return value;
	}



	public static class TestA {
		public static void main(String args[]) {
			for (int i=0; i<7000; i++) {
				int num = i;
				byte data[] = getBytes(num);
				System.out.println(num + " and " + getInt(data));
		  }

		}
	}

	public static class TestB {
		public static void main(String args[]) {
			System.out.println(Integer.toBinaryString((byte)128));
			System.out.println(Integer.toBinaryString((byte)128 & 0xFF));
		}
	}
*/
}
