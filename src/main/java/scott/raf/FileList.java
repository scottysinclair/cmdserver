package scott.raf;

import java.io.*;
import java.util.Iterator;

public class FileList {
	public interface DataType {
		public byte[] toBytes(Object object);
		public Object fromBytes(RandomAccessFile file, int position) throws IOException;
	}

	private FileAccess file;
	private int head;
	private DataType dataType;
	private int size;

	public FileList(FileAccess file, DataType dataType, int position) throws IOException {
		this.file = file;
		this.head = position;
		this.dataType = dataType;
		findSize();
	}

	public FileList(FileAccess file, DataType dataType) throws IOException {
		this.file = file;
		this.dataType = dataType;
		this.head = file.allocate(4);
		this.size = 0;
		file.access().seek(head);
		file.access().writeInt(-1);
	}

	public int location() {
		return head;
	}

	public Iterator iterator() throws IOException {
		file.access().seek(head);
		final int start = file.access().readInt();
		return new Iterator() {
			private int entry = start;
			public boolean hasNext() {
				return entry != -1;
			}

			public Object next() {
				try {
					file.access().seek(entry);
					int thisEntry = entry;
					entry =  file.access().readInt();
					return dataType.fromBytes(file.access(), thisEntry);
			  }
			  catch(IOException x) {
					throw new RuntimeException(x);
				}
			}

			public void remove() {}
		};
	}

	/** splits the list in two returning a new list containing items from
	 * start to end-1
	 * @param start
	 * @param end
	 * @throws IOException
	 */
	public FileList split(int start) throws IOException {
		if (start < 0) {
			throw new ArrayIndexOutOfBoundsException("" + start);
		}

		//go to the new last entry of this list
		file.access().seek(head);
		int entry = file.access().readInt();
		for (int i=0; i<start-1 && entry!=-1; i++) {
			file.access().seek(entry);
			entry = file.access().readInt();
//			System.out.print(".");
		}
		if (entry == -1) {
			throw new IOException("unexpected end of list");
		}
		int lastEntry = entry;

		//read the position of the new list's first entry
		file.access().seek(lastEntry);
		int newFirstEntry = file.access().readInt();

		//write the end of our list
		file.access().seek(lastEntry);
		file.access().writeInt(-1);
		size = start;

		//write the new first entry to the new list's head
		FileList newList = new FileList(file, dataType);
		file.access().seek(newList.head);
		file.access().writeInt(newFirstEntry);
		newList.findSize();
		return newList;
	}

	public void add(Object object) throws IOException {
//		long start = System.currentTimeMillis();
		byte data[] = dataType.toBytes( object );
		int position = file.allocate(data.length + 4);
		file.access().seek(position);
		file.access().writeInt(-1);
		file.access().write(data);

		int prevEntry = -1;
		file.access().seek(head);
		int entry  = file.access().readInt();
		while(entry != -1) {
//			System.out.print("." + entry + ".");
			prevEntry = entry;
			file.access().seek( entry );
			entry = file.access().readInt();
		}

		if (prevEntry == -1) {
			file.access().seek(head);
			file.access().writeInt(position);
		}
		else {
			file.access().seek(prevEntry);
			file.access().writeInt(position);
		}
		size++;
//		System.out.println("time = " + (System.currentTimeMillis() - start));
	}

	public void add(int index, Object object) throws IOException {
		int entry = head;
		if (index < 0) {
			throw new ArrayIndexOutOfBoundsException("" + index);
		}
		else if (index > 0) {
			file.access().seek(head);
			entry = file.access().readInt();
			for (int i=0; i<index-1 && entry!=-1; i++) {
				file.access().seek(entry);
				entry = file.access().readInt();
			}
		}

		if (entry != -1) {
			file.access().seek(entry);
			int next = file.access().readInt();
			byte data[] = dataType.toBytes( object );
			int position = file.allocate(data.length + 4);
			file.access().seek(entry);
			file.access().writeInt(position);
			file.access().seek(position);
			file.access().writeInt(next);
			file.access().write(data);
	  }
	  else {
			throw new ArrayIndexOutOfBoundsException("" + index);
		}
		size++;
	}

	public Object get(int index) throws IOException {
		Object object = null;
		file.access().seek(head);
		int entry = file.access().readInt();
		for (int i=0; i<index && entry!=-1; i++) {
			file.access().seek(entry);
			entry = file.access().readInt();
		}
		if (entry != -1) {
			object = dataType.fromBytes(file.access(), entry);
	  }
	  return object;
	}

	public Object remove(int index) throws IOException {
		Object object = null;
		file.access().seek(head);
		int entry = file.access().readInt();
		if (index < 0) {
			throw new ArrayIndexOutOfBoundsException("" + index);
		}
		else if (index == 0) {
			file.access().seek(entry);
			int nextPos = file.access().readInt();
			object = dataType.fromBytes(file.access(), entry);
			file.access().seek(head);
			file.access().writeInt(nextPos);
		}
		else {
			for (int i=0; i<index-1 && entry!=-1; i++) {
				file.access().seek(entry);
				entry = file.access().readInt();
			}
			if (entry != -1) {
				file.access().seek(entry);
				int objectPos = file.access().readInt();
				if (objectPos == -1) {
					throw new ArrayIndexOutOfBoundsException("" + index);
				}
				file.access().seek(objectPos);
				int afterObjectPos = file.access().readInt();
				file.access().seek(entry);
				file.access().writeInt(afterObjectPos);
				object = dataType.fromBytes(file.access(), objectPos);
			}
			else {
				throw new ArrayIndexOutOfBoundsException("" + index);
			}
		}
		size--;
		return object;
	}

	private void findSize() throws IOException {
		Object object = null;
		file.access().seek(head);
		int entry = file.access().readInt();
		int i = 0;
		for (; entry!=-1; i++) {
			file.access().seek(entry);
			entry = file.access().readInt();
//			System.out.print(",");
		}
		size = i+1;
	}

	public int size() {
		return size;
	}


	public static class Test1 {
		public static void main(String args[]) {
			try {
				int position = 0;
				if (args.length > 0) {
					position = Integer.parseInt(args[0]);
				}

				DataType stringDT = new DataType() {
					public byte[] toBytes(Object object) {
						String str = (String)object;
						byte data[][] = new byte[2][];
						data[1] =  str.getBytes();
						data[0] = ByteHelper.getBytes(data[1].length);
						//System.out.println("tb - " + data[1].length);
						return ByteHelper.mergeBytes(data);
					}
					public Object fromBytes(RandomAccessFile file, int position) throws IOException {
						int length = ByteHelper.readInt(file, position + 4);
						//System.out.println("fb - " + length);
						byte data[] = new byte [length ];
						file.read(data);
						return new String(data);
					}
				};

				FileAccess file = new FileAccess(new File("filelist.raf"));
				FileList list = null;
				if (position == 0) {
					list = new FileList(file, stringDT);
				}
				else {
					list = new FileList(file, stringDT, position);
				}
				System.out.println( list.location() );

//				long start = System.currentTimeMillis();
//				for (int i=0; i<200; i++) {
					list.add("one");
					list.add("flew");
					list.add("over");
					list.add("the");
					list.add("cuckoo's");
					list.add("nest");
					list.add(0, "CRAZY");

					list.add("one");
					list.add("flew");
					list.add("over");
					list.add("the");
					list.add("cuckoo's");
					list.add("nest");
					list.add(0, "CRAZY");
					System.out.println("size = " + list.size() );

					FileList l2 = list.split(5);
				//list.remove(1);
	//	   }

//				System.out.println("time = " + (System.currentTimeMillis() - start));
				System.out.println("list contains");
				Iterator i = list.iterator();
				int count = 0;
				while(i.hasNext()) {
					//i.next();
					System.out.println(i.next());
					count++;
				}
				System.out.println("list contains " + count + " items");


				System.out.println("list contains");
				list = l2;
				i = list.iterator();
				count = 0;
				while(i.hasNext()) {
					//i.next();
					System.out.println(i.next());
					count++;
				}
				System.out.println("list contains " + count + " items");

			}
			catch(Exception x) {
				x.printStackTrace();
			}
		}
	}

}
