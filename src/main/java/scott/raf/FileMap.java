package scott.raf;

import java.util.Iterator;
import java.io.*;

public class FileMap {

	public int put(String name) {
		return 0;
	}

	public int get(String name) {
		return 0;
	}


}

class MapContext {
	private FileAccess fileAccess;
	private FileList.DataType datatype;
	private int maxKeys;

	public MapContext(FileAccess fa, FileList.DataType da, int maxKeys) {
		this.fileAccess = fa;
		this.datatype = da;
		this.maxKeys = maxKeys;
	}

	public int getMaxKeys() {
		return maxKeys;
	}

	public FileAccess getFileAccess() {
		return fileAccess;
	}

	public FileList.DataType getDataType() {
		return datatype;
  }
}


interface Parent {
	public void addKey(String key, Child left, Child right);
}

interface Child {
	public FileList remove(String name) throws IOException;
	public FileList put(String name) throws IOException;
	public FileList get(String name) throws IOException;
}

class Node implements Child, Parent {
	private Parent parent;
	private MapContext ctx;
	private int position;
	private FileList list;

	public Node(MapContext ctx, Parent parent, FileList list) throws IOException {
		this.ctx = ctx;
		this.parent = parent;
		this.position = ctx.getFileAccess().allocate(1);
		ctx.getFileAccess().access().seek(position);
		ctx.getFileAccess().access().writeInt( list.location() );
		this.list = list;
	}

	public Node(MapContext ctx, Parent parent, int position) throws IOException {
		this.ctx = ctx;
		this.parent = parent;
		this.position = position;
		ctx.getFileAccess().access().seek(position);
		int listPos = ctx.getFileAccess().access().readInt();
		this.list = new FileList(ctx.getFileAccess(), new NodeEntryType(ctx), listPos);
	}

	public int location() {
		return position;
	}

	public FileList remove(String name) throws IOException {
		Iterator i = list.iterator();
		NodeEntry entry = null;
		while(i.hasNext()) {
			entry = (NodeEntry)i.next();
			int cmp = name.compareTo(entry.getName());
			if (cmp < 0) {
				Child child = entry.getLeftChild(ctx, this);
				return child.remove(name);
			}
		}
		if (entry != null && name.compareTo(entry.getName()) >= 0) {
			Child child = entry.getRightChild(ctx, this);
			return child.remove(name);
		}
		throw new RuntimeException(name + " not found");
	}


	public FileList put(String name) throws IOException {
		Iterator i = list.iterator();
		NodeEntry entry = null;
		while(i.hasNext()) {
			entry = (NodeEntry)i.next();
			int cmp = name.compareTo(entry.getName());
			if (cmp < 0) {
				Child child = entry.getLeftChild(ctx, this);
				return child.put(name);
			}
		}
		if (entry != null && name.compareTo(entry.getName()) >= 0) {
			Child child = entry.getRightChild(ctx, this);
			return child.put(name);
		}
		throw new RuntimeException(name + " not found");
	}


	public FileList get(String name) throws IOException {
		Iterator i = list.iterator();
		NodeEntry entry = null;
		while(i.hasNext()) {
			entry = (NodeEntry)i.next();
//			System.out.println("checking node entry " + entry.getName());
			int cmp = name.compareTo(entry.getName());
			if (cmp < 0) {
				Child child = entry.getLeftChild(ctx, this);
				return child.get(name);
			}
		}
		if (entry != null && name.compareTo(entry.getName()) >= 0) {
			Child child = entry.getRightChild(ctx, this);
			return child.get(name);
		}
		throw new RuntimeException(name + " not found");
	}

	public void addKey(String key, Child left, Child right) {
	}

}


class Leaf implements Child {
	private Parent parent;
	private MapContext ctx;
	private int position;
	private FileList list;

	/** creates a leaf on disk. */
	public Leaf(MapContext ctx, Parent parent) throws IOException {
		this.ctx = ctx;
		this.parent = parent;
		this.position = ctx.getFileAccess().allocate(1);
		this.list = new FileList(ctx.getFileAccess(), new LeafEntryType(ctx));
		int listPos = list.location();
		ctx.getFileAccess().access().seek(position);
		ctx.getFileAccess().access().writeInt(listPos);
	}

  /**loads a leaf from disk. */
	public Leaf(MapContext ctx, Parent parent, int position) throws IOException {
		this.ctx = ctx;
		this.parent = parent;
		this.position = position;
		ctx.getFileAccess().access().seek(position);
		int listPos = ctx.getFileAccess().access().readInt();
		this.list = new FileList(ctx.getFileAccess(), new LeafEntryType(ctx), listPos);
	}

  /**creates a splitted leaf. */
  public Leaf(MapContext ctx, Parent parent, FileList list) throws IOException {
	  this.ctx = ctx;
	  this.parent = parent;
		this.position = ctx.getFileAccess().allocate(1);
		this.list = list;
		int listPos = list.location();
		ctx.getFileAccess().access().seek(position);
		ctx.getFileAccess().access().writeInt(listPos);
  }

	public int location() {
		return position;
	}

	public FileList remove(String name) throws IOException  {
		int index = 0;
		Iterator i = list.iterator();
		while(i.hasNext()) {
			LeafEntry entry = (LeafEntry)i.next();
			int cmp = entry.getName().compareTo(name);
			if (cmp == 0) {
				list.remove(index);
				return entry.getList();
			}
			else if (cmp > 0) {
				throw new RuntimeException(name + " not found");
			}
			index++;
		}
		throw new RuntimeException(name + " not found");
	}


	public FileList put(String name) throws IOException  {
		int index = 0;
		FileList reference = null;
		Iterator i = list.iterator();
		while(reference == null && i.hasNext()) {
			LeafEntry entry = (LeafEntry)i.next();
			int cmp = entry.getName().compareTo(name);
			if (cmp == 0) {
				reference = entry.getList();
			}
			else if (cmp > 0) {
				LeafEntry le = new LeafEntry(name, new FileList(ctx.getFileAccess(), ctx.getDataType()));
				list.add(index, le);
				reference = le.getList();
			}
			index++;
		}

		if (reference == null) {
			LeafEntry le = new LeafEntry(name, new FileList(ctx.getFileAccess(), ctx.getDataType()));
			list.add(le);
			reference = le.getList();
		}

		if (list.size() > (ctx.getMaxKeys() + 1)) {
			split();
		}

		return reference;
	}

	private void split() throws IOException {
		int middle = list.size() / 2;
		FileList newList = list.split(middle+1);
		Leaf right = new Leaf(ctx, parent, newList);
		LeafEntry le = (LeafEntry)newList.get(0);
		parent.addKey(le.getName(), this, right);
	}

	public FileList get(String name) throws IOException {
		Iterator i = list.iterator();
		while(i.hasNext()) {
			LeafEntry entry = (LeafEntry)i.next();
//			System.out.print(entry.getName() + " ");
			if (entry.getName().equals( name )) {
				return entry.getList();
			}
		}
		throw new RuntimeException(name + " not found");
	}
}


class NodeEntry {
	private String name;
	private int left, right;
	private boolean leftIsLeaf, rightIsLeaf;

	public NodeEntry(String name, int left, int right, boolean ll, boolean rl) throws IOException {
		if (left < 0 || right < 0) {
			throw new IOException("left and right must have a position greater than 0");
		}
		this.name = name;
		this.left = left;
		this.right = right;
		this.leftIsLeaf = ll;
		this.rightIsLeaf = rl;
	}

	public String getName() {
		return name;
	}

	public Child getLeftChild(MapContext ctx, Node parent) throws IOException {
		if (leftIsLeaf) {
			return new Leaf(ctx, parent, left);
		}
		else {
			return new Node(ctx, parent, left);
		}
	}

	public int getLeft() {
		return left;
	}

	public Child getRightChild(MapContext ctx, Node parent) throws IOException {
		if (rightIsLeaf) {
			return new Leaf(ctx, parent, right);
		}
		else {
			return new Node(ctx, parent, right);
		}
	}

	public int getRight() {
		return right;
	}

	public boolean leftIsLeaf() {
		return leftIsLeaf;
  }
	public boolean rightIsLeaf() {
		return rightIsLeaf;
  }
}


class LeafEntry {
	private String name;
	private FileList list;

	public LeafEntry(String name, FileList list) {
		this.name = name;
		this.list = list;
	}

	public String getName() {
		return name;
	}

	public FileList getList() {
		return list;
	}

}


class NodeEntryType implements FileList.DataType {
	private MapContext ctx;

	public NodeEntryType(MapContext ctx) {
		this.ctx = ctx;
	}

	public byte[] toBytes(Object object) {
		NodeEntry ne = (NodeEntry)object;
		byte data[][] = new byte[5][];
		data[0] = ByteHelper.getBytes(ne.getLeft());
		data[1] = ByteHelper.getBytes(ne.getRight());
		data[2] = new byte[2];
		data[2][0] = ne.leftIsLeaf() ? (byte)1 : (byte)0;
		data[2][1] = ne.rightIsLeaf() ? (byte)1: (byte)0;
		data[4] = ne.getName().getBytes();
		data[3] = ByteHelper.getBytes( data[4].length );
		return ByteHelper.mergeBytes(data);
	}
	public Object fromBytes(RandomAccessFile file, int position) throws IOException
{
		file.seek(position + 4);
		int left = file.readInt();
		int right = file.readInt();
		boolean leftIsLeaf = (1 == file.read());
		boolean rightIsLeaf = (1 == file.read());
		int length = file.readInt();
		byte data[] = new byte [length ];
		file.read(data);
		return new NodeEntry(new String(data), left, right, leftIsLeaf, rightIsLeaf);
	}
}


class LeafEntryType implements FileList.DataType {
	private MapContext ctx;

	public LeafEntryType(MapContext ctx) {
		this.ctx = ctx;
	}

	public byte[] toBytes(Object object) {
		LeafEntry le = (LeafEntry)object;
		byte data[][] = new byte[3][];
		data[0] = ByteHelper.getBytes(le.getList().location());
		data[2] =  le.getName().getBytes();
		data[1] = ByteHelper.getBytes(data[2].length);
		//System.out.println("tb - " + data[1].length);
		return ByteHelper.mergeBytes(data);
	}
	public Object fromBytes(RandomAccessFile file, int position) throws IOException
{
		int ref = ByteHelper.readInt(file, position + 4);
		int length = ByteHelper.readInt(file, position + 8);
		//System.out.println("fb - " + length);
		byte data[] = new byte [length ];
		file.read(data);
		return new LeafEntry(new String(data), new FileList(ctx.getFileAccess(),
ctx.getDataType(), ref));
	}
}
