package scott.raf;

import java.io.*;
import java.util.*;

public class FileAccess {

	private RandomAccessFile file;

	public FileAccess(File f) throws IOException {
		boolean newFile = f.exists() == false;
		file = new RandomAccessFile(f, "rws");
		if (newFile) {
			file.seek(0);
			file.writeInt(-1);
		}
	}

	public int allocate(int length) throws IOException {
		if (length > 10) {
			length = 10;
		}

		int position = findInFreedMemory( length );
		if (position == -1) {
			position = allocateFromEOF( length );
		}
//		System.out.println();
		return position;
	}

	public RandomAccessFile access() {
		return file;
	}

	public void deallocate(int position) throws IOException {
		//TODO: check if we can shrink the file
		int firstChunkPosition = firstChunkPosition();
		file.seek(0);
		file.writeInt(position - 4);
		file.seek(position);
		file.writeInt(firstChunkPosition);
	}

	private int findInFreedMemory(int length) throws IOException {
		int lengthMatch = (int)file.length();
		int prevChunkMatch = -1;
		int chunkMatch = -1;

		int prevChunk = -1;
		int chunkPosition = firstChunkPosition();
		while(chunkPosition != -1) {
			System.out.print("." + chunkPosition + ".");
			file.seek(chunkPosition);
			int chunkLength = file.readInt();
			int nextChunkPosition = file.readInt();
			if (chunkLength >= length && chunkLength < lengthMatch) {
				lengthMatch = chunkLength;
				chunkMatch = chunkPosition;
				prevChunkMatch = prevChunk;
		  }
			prevChunk = chunkPosition;
			chunkPosition = nextChunkPosition;
		}

		if (chunkMatch != -1) {
				file.seek(chunkMatch);
				int chunkLength = file.readInt();
				int nextChunkPosition = file.readInt();

				//join the previous chunk to the next chunk
				if (prevChunkMatch == -1) {
					file.seek(0);
					file.writeInt( nextChunkPosition );
					System.out.println(0 + " - " + nextChunkPosition);
				}
				else {
					file.seek(prevChunkMatch + 4);
					file.writeInt( nextChunkPosition );
					System.out.println(prevChunkMatch + " - " + nextChunkPosition);
				}

				//if the chunk has more than 10 bytes extra create a new chunk for the extra
				//and put it on the head
				if ((lengthMatch - length) > 10) {
					System.out.print("S");
					file.seek(chunkMatch); //change the length of the current chunk
					file.writeInt(length);
					int newChunkPosition = chunkMatch + 4 + length;
					file.seek(newChunkPosition);
					file.writeInt(lengthMatch - length);
					deallocate(newChunkPosition + 4);
				}
				chunkMatch += 4;
			}

		return chunkMatch;
	}

	private int allocateFromEOF(int length) throws IOException {
//		System.out.print(" eof ");
		int position = (int)file.length();
		file.setLength( file.length() + length + 4);
		file.seek(position);
		file.writeInt(length);
		return position + 4;
	}

	private int firstChunkPosition() throws IOException {
		file.seek(0);
		return file.readInt();
	}

	public static class Test1 {
		public static void main(String args[]) {
			try {
				FileAccess file = new FileAccess(new File("Test1.raf"));

				for (int i=0; i<100; i++) {
					int size = 100;
					int pos = file.allocate(size);
					System.out.println("allocated " + size + " bytes at " + pos);
					file.deallocate(pos);
					System.out.println("deallocated " + size + " bytes at " + pos);
					System.out.println("length = " + file.file.length());


					size = 200;
					pos = file.allocate(size);
					System.out.println("allocated " + size + " bytes at " + pos);
					file.deallocate(pos);
					System.out.println("deallocated " + size + " bytes at " + pos);
					System.out.println("length = " + file.file.length());

					size = 2100;
					pos = file.allocate(size);
					System.out.println("allocated " + size + " bytes at " + pos);
					file.deallocate(pos);
					System.out.println("deallocated " + size + " bytes at " + pos);
					System.out.println("length = " + file.file.length());
				}
			}
			catch(IOException x) {
				x.printStackTrace();
			}
		}
	}


	public static class Test2 {
		public static void main(String args[]) {
			try {
				FileAccess file = new FileAccess(new File("Test2.raf"));

				for (int k=0; k<3; k++) {
					int pos[] = new int [100];
					Random rand = new Random();
					System.out.println("ALLOCATING");
					for (int i=0; i<100; i++) {
						int length = rand.nextInt(1000);
						pos[i] = file.allocate(length);
					}

					System.out.println("DEALLOCATING");
					for (int i=0; i<100; i++) {
						file.deallocate(pos[i]);
					}
					System.out.println("LENGTH = " + file.file.length());
			 }



			}
			catch(IOException x) {
				x.printStackTrace();
			}
		}
	}

}
