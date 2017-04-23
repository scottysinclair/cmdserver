package scott.image;

import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.util.*;

public class JPEGFile {

	public static int getNumber(int high, int low) {
		return (high << 8) + (low&0x00ff);
	}

	public static void skipBytes(RandomAccessFile file, int n) throws IOException {
	  int skipped = 0;
	  while(skipped < n) {
	    skipped += file.skipBytes(n-skipped);
	  }
	}

	public static void main(String args[]) {
		try {
			JPEGFile jpeg = new JPEGFile( new File(args[0]) );
			Photoshop photoshop = jpeg.getPhotoshopSection();
			System.out.println("Title: " + photoshop.getTitle());
			System.out.println("Description: " + photoshop.getDescription());
			System.out.println("Author: " + photoshop.getAuthor());
			System.out.println("Description Writer: " + photoshop.getDescriptionWriter());
			String keywords[] = photoshop.getKeywords();
			for (int i=0; i<keywords.length; i++) {
				System.out.println("Keyword: " + keywords[i]);
			}

			StartOfFrame sof = jpeg.getStartOfFrameSection();
			System.out.println("Width: " + sof.getWidth());
			System.out.println("Height: " + sof.getHeight());

		}
		catch(Exception x) {
			x.printStackTrace();
		}
	}

	private RandomAccessFile file;
	private Photoshop photoshop;
	private StartOfFrame sof;

	public JPEGFile(File _file) throws IOException {
		this.file = new RandomAccessFile( _file , "r");
		init();
	}

	public StartOfFrame getStartOfFrameSection() {
		return sof;
	}

	public void setPhotoshopSection(Photoshop photoshop) {
		this.photoshop = photoshop;
	}

	public Photoshop getPhotoshopSection() {
		return photoshop;
	}

	public void save() throws IOException {
		//write all sections before photoshop section
		//write photoshop section
		//write all sections after photoshop section.
	}

	private void init() throws IOException {
		SectionsFac sections = new SectionsFac();
		Section section = sections.createSection(file, 2);
		while(section.getType() != Section.START_OF_SCAN) {
			if (section instanceof Photoshop) {
				this.photoshop = (Photoshop)section;
			}
			else if (section instanceof StartOfFrame) {
				this.sof = (StartOfFrame)section;
			}
			section = sections.createSection(file, section.nextSection(file));
	  }
	}


class SectionsFac {
	public Section createSection(RandomAccessFile file, int location) throws IOException {
		file.seek(location);
		String type = readType(file);
		int size = readSize(file);
		if (Section.APPLICATION == type) {
			return new Application(file, location, type, size);
		}
		else if (Section.START_OF_FRAME == type) {
			return new StartOfFrame(file, location, type, size);
		}
		else if (Section.PHOTOSHOP == type) {
			return new Photoshop(file, location, type, size);
		}
		else {
			return new Section(file, location, type, size);
		}
	}

	private int readSize(RandomAccessFile file) throws IOException  {
		int size = 0;
		int a = file.read(); //read the length of the section
		int b = file.read();
		//System.out.print(a + ", " + b + " ");
		size = JPEGFile.getNumber(a, b);
		if (size < 0) {
			throw new IOException("bad section size");
		}
		return size;
	}

	private String readType(RandomAccessFile file) throws IOException  {
		String type = Section.UNKNOWN;
		int a = file.read(); //read the length of the section
		int b = file.read();
		//System.out.println(a + ", " + b);
		if (a == 0xFF && b == 0xE0) {
			type = Section.APPLICATION;
		}
		else if (a == 0xFF && b == 0xDB) {
			type = Section.QUANTIZATION_TABLE;
		}
		else if (a == 0xFF && b == 0xC0) {
			type = Section.START_OF_FRAME;
		}
		else if (a == 0xFF && b == 0xC4) {
			type = Section.DEFINE_HUFFMAN_TABLE;
		}
		else if (a == 0xFF && b == 0xDA) {
			type = Section.START_OF_SCAN;
		}
		else if (a == 0xFF && b == 0xDD) {
			type = Section.DEFINE_RESTART_INTERVAL;
		}
		else if (a == 0xFF && b == 0xED) {
			type = Section.PHOTOSHOP;
		}
		else if (a == 0xFF && b == 0xEE) {
			type = Section.UNKNOWN_APPLICATION;
		}
		else {
			//System.out.println("Unknown Section Type " + a + ", " + b);
		}
		return type;
	}
}


public class Photoshop extends Section {
	 private String photoshopId;
	 private String keywords[];
	 private String author;
	 private String title;
	 private String description;
	 private String descriptionWriter;

	 public Photoshop(RandomAccessFile file, int location, String type, int size) throws IOException {
		 super(file, location, type, size);
		 byte header[] = new byte[14];
		 file.readFully(header);
		 this.photoshopId = new String(header);
		 read8BIMSegments(file);
	 }

	 public String getPhotoshopId() {
		 return photoshopId;
	 }

	 public void setPhotoshopId(String photoshopId) {
		 this.photoshopId = photoshopId;
   }

	 public String[] getKeywords() {
		 return keywords;
	 }

	 public void setKeywords(String keywords[]) {
		 this.keywords = keywords;
	 }

	 public String getAuthor() {
		 return author;
	 }

	 public void setAuthor(String author) {
		 this.author = author;
   }

	 public String getTitle() {
		 return title;
	 }

	 public void setTitle(String title) {
		 this.title = title;
   }

	 public String getDescriptionWriter() {
		 return descriptionWriter;
	 }

	 public void setDescriptionWriter(String descriptionWriter) {
		 this.descriptionWriter = descriptionWriter;
   }

	 public String getDescription() {
		 return description;
	 }

	 public void setDescription(String description) {
		 this.description = description;
   }

	 private void read8BIMSegments(RandomAccessFile file) throws IOException {
	   while((file.getFilePointer() - location) < size) {
	     long position = file.getFilePointer();
  		 int a = 0;
  		 while((a = file.read()) == 0);

       if (a == 0x38 && file.read() == 0x42 && file.read() == 0x49 && file.read() == 0x4D) {
         int typeA = file.read();
         int typeB = file.read();
	  	   if (typeA == 0x04 && typeB == 0x04) {
		   	   handleTextInfo(file);
		     }
  		   else {
  		     handleUnknown8BIMType(file, typeA, typeB);
  	 	  }
       }
       else {
        System.err.println("could not read 8BIM header");
        file.seek(position);
        return;
      }
    }
	 }

	 private void handleTextInfo(RandomAccessFile file) throws IOException {
		 List keywordsList = new ArrayList();
		 /*
			TYPE 25 - keyword - 0x19
			TYPE 80 - Author - 0x50
			TYPE 120 - descriptionWriter - 0x78
			TYPE 122 - description
			TYPE 5 - title

		 */
	   //System.out.println("TextInfo Segment Type");
	   String segmentDescription = read8BIMHeaderDescription(file);
	   int segmentSize = read8BIMSegmentSize(file);
	   //System.out.println(" size = " + segmentSize);
	   long amountRead = file.getFilePointer();
	   while(amountRead < size) {
	     int typeA = file.read();
		   int typeB = file.read();
    	 int typeText = file.read();
		   int size = JPEGFile.getNumber(file.read(), file.read());
		   byte data[] = new byte[size];
  		 file.readFully(data);
  		 if (typeA == 0x1C && typeB == 0x02) {
				 if (typeText == 0x19) {
					 keywordsList.add(new String(data));
				 }
				 else if (typeText == 0x50) {
					 this.author = new String(data);
				 }
				 else if (typeText == 0x78) {
					 this.descriptionWriter = new String(data);
				 }
				 else if (typeText == 0x7A) {
					 this.description = new String(data);
				 }
				 else if (typeText == 0x05) {
					 this.title = new String(data);
				 }
  		 }
  		 else {
  		   //System.out.println("unknown text type = " + typeA + ", " + typeB);
  		   JPEGFile.skipBytes(file, size);
  		 }
//  		 System.out.println("size = " + size);
  		 amountRead = file.getFilePointer();
 	   }
 	   if (!keywordsList.isEmpty()) {
			 keywords = (String[])keywordsList.toArray(new String[ keywordsList.size() ]);
		 }

	 }

	 private void handleUnknown8BIMType(RandomAccessFile file, int typeA, int typeB) throws IOException {
	   //System.out.print("8BIM Segment type = " + typeA + ", " + typeB);
 	   String description = read8BIMHeaderDescription(file);
	   //System.out.print(" descr = " + description);
       int segmentSize = read8BIMSegmentSize(file);
	   //System.out.println(" size = " + segmentSize);
       JPEGFile.skipBytes(file, segmentSize);
	 }

	 private int read8BIMSegmentSize(RandomAccessFile file) throws IOException {
	  long loc = file.getFilePointer();
	  JPEGFile.skipBytes(file, 2);
	  int size = JPEGFile.getNumber(file.read(), file.read());
	  if (size == 0) { //photoshop bug
		file.seek(loc+3);
		size = JPEGFile.getNumber(file.read(), file.read());
	  }
	  return size;
	 }

	 private String read8BIMHeaderDescription(RandomAccessFile file) throws IOException {
	  int length = file.read();
	  //System.out.println("header size = " + length);
	  byte data[] = new byte[ length ];
	  file.readFully(data);
	  return new String(data);
	 }

	 public String toString() {
		 String str = "---------------\nPhotoshop\nid: " + photoshopId +
		        "\nTitle: " + title + "\nAuthor: " + author +
		        "\nDescription: " + description + "\nDescription Writer: " + descriptionWriter;

		 StringBuffer sb = new StringBuffer(str);
		 sb.append("\nKeywords: ");
		 for (int i=1; i<keywords.length; i++) {
			 sb.append(keywords[i-1] + ",");
		 }
		 sb.append(keywords[keywords.length-1]);
		 return sb.toString();
	 }

}

public class StartOfFrame extends Section {
	  //after first 5 bytes
	 private int horizontalResolution;//2
	 private int verticalResolution;//2

	 public StartOfFrame(RandomAccessFile file, int location, String type, int size) throws IOException {
		 super(file, location, type, size);
		 file.seek(location + 5);
		 byte header[] = new byte[4];
		 file.readFully(header);
		 this.horizontalResolution = JPEGFile.getNumber(header[0], header[1]);
		 this.verticalResolution = JPEGFile.getNumber(header[2], header[3]);
	 }

	 public int getWidth() {
		 return horizontalResolution;
	 }

	 public int getHeight() {
		 return verticalResolution;
	 }

	 public String toString() {
		 return "--------------\nStart of Frame\nwidth: " + horizontalResolution + "\n" +
		 				"height: " + verticalResolution + "\n---------------";
	 }
}

public class Application extends Section {
	 private String applicationCode; //4
	 private String jfifMajorVersion;//2
	 private String jfifMinorVersion;//2
	 private int horizontalPixelDensity;//2
	 private int verticalPixelDensity;//2
	 private int thumbWidth;//1
	 private int thumbHeight;//1

	 public Application(RandomAccessFile file, int location, String type, int size) throws IOException {
		 super(file, location, type, size);
		 byte header[] = new byte[14];
		 file.readFully(header);
		 applicationCode = new String(header, 0, 4);
		 jfifMajorVersion = new String(header, 4, 2);
		 jfifMinorVersion = new String(header, 6, 2);
		 horizontalPixelDensity = JPEGFile.getNumber(header[8], header[9]);
		 verticalPixelDensity = JPEGFile.getNumber(header[10], header[11]);
		 thumbWidth = header[12] & 0x00FF;
		 thumbHeight = header[13] & 0x00FF;
	 }

	 public String getApplicationCode() {
		 return applicationCode;
	 }

	 public String toString() {
		 return "--------------------\nApplication Section\n" +
		 		    "Application Code: " + applicationCode + "\n" +
		 		    "jfifMajorVersion: " + jfifMajorVersion + "\n" +
		 		    "jfifMinorVersion: " + jfifMinorVersion + "\n" +
		 		    "horizontalPixelDensity: " + horizontalPixelDensity + "\n" +
		 		    "verticalPixelDensity: " + verticalPixelDensity + "\n" +
		 		    "thumbWidth: " + thumbWidth + "\n" +
		 		    "thumbHeight: " + thumbHeight + "\n--------------";
	 }
}


class Section {
	public final static String APPLICATION = "APPLICATION";
	public final static String QUANTIZATION_TABLE = "QUANTIZATION TABLE";
	public final static String START_OF_FRAME = "START OF FRAME";
	public final static String DEFINE_HUFFMAN_TABLE = "DEFINE HUFFMAN TABLE";
	public final static String START_OF_SCAN = "START OF SCAN";
	public final static String PHOTOSHOP = "PHOTOSHOP";
	public final static String DEFINE_RESTART_INTERVAL = "DEFINE RESTART INTERVAL";
	public final static String UNKNOWN = "UNKNOWN";
	public final static String UNKNOWN_APPLICATION = "UNKNOWN APPLICATION";

	protected final String type;
	protected final int location;
	protected final int size;
	protected final byte data[];

	public Section(RandomAccessFile file, int location, String type, int size) throws IOException {
		this.location = location;
		this.type = type;
		this.size = size;
        this.data = null;
	}

	public byte[] getBytes(RandomAccessFile file) throws IOException {
		if (data == null) {
			file.seek(location);
			byte data[] = new byte [size + 4];
			file.readFully(data);
		}
		return data;
	}

	public int nextSection(RandomAccessFile file) throws IOException {
		return location + size + 2;
	}

	public String getType() {
		return type;
	}

	public int getLocation() {
		return location;
	}

	public int getSize() {
		return size;
	}

	public String toString() {
		return "-----------------\nl=" + location + ",t=" +  type + ",s=" + size + "\n-----------------";
	}
}

}